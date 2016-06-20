package ru.spbau.mit;

import com.google.common.base.Throwables;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.architecture.Architecture;
import ru.spbau.mit.architecture.ServerType;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class ServerTestsMain {
    private static final Logger LOG = LogManager.getLogger(ServerTestsMain.class);
    private static double requestHandleTime, clientHandleTime;
    private static Architecture architecture;
    private static XYChart<Number, Number> charts[] = new XYChart[3];

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        JFrame frame = new JFrame("ServerTests");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.add(createGraph());
        jPanel.add(createOptions(frame));

        frame.add(jPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel createGraph() {
        JPanel chartPanel = new JPanel();
        for (int i = 0; i < charts.length; i++) {
            JFXPanel jfxPanel = new JFXPanel();
            jfxPanel.setScene(new Scene(charts[i] = new LineChart<>(new NumberAxis(), new NumberAxis())));
            chartPanel.add(jfxPanel);
        }
        return chartPanel;
    }

    private static JTextField addParameter(JPanel options, ButtonGroup group, String name,
                                           String value, boolean selected) {
        JRadioButton button = new JRadioButton(name, selected);
        group.add(button);
        options.add(button);
        JTextField field = new JTextField(value);
        options.add(field);
        return field;
    }

    private static JTextField addParameter(JPanel options, String name, String value) {
        options.add(new JLabel(name));
        JTextField field = new JTextField(value);
        options.add(field);
        return field;
    }

    private static JPanel createOptions(JFrame frame) {
        JPanel options = new JPanel(new GridLayout(10, 6));
        ButtonGroup group = new ButtonGroup();

        JTextField arraySize = addParameter(options, group, Config.parameters[0].getName(), "10", true);
        JTextField clientsCount = addParameter(options, group, Config.parameters[1].getName(), "2", false);
        JTextField delay = addParameter(options, group, Config.parameters[2].getName(), "100", false);
        JTextField requestsCount = addParameter(options, group, Config.parameters[3].getName(), "4", false);

        JTextField upperBoundField = addParameter(options, "Upper bound", "50");
        JTextField stepField = addParameter(options, "Step", "10");
        JTextField serverAddress = addParameter(options, "Server's IP", "127.0.0.1");

        JButton startButton = new JButton("Start");
        options.add(startButton);

        JComboBox<String> jComboBox = new JComboBox<>(
                new Vector<>(Arrays.stream(Config.ARCHITECTURES).map(Architecture::getName).collect(Collectors.toList()))
        );
        options.add(jComboBox);

        startButton.addActionListener((event) -> {
            Config.arraySize.set(arraySize.getText());
            Config.clientsCount.set(clientsCount.getText());
            Config.delay.set(delay.getText());
            Config.requestsCount.set(requestsCount.getText());
            Config.serverAddress = serverAddress.getText();
            int toChangeId = 0;
            for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements(); toChangeId++) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    break;
                }
            }
            final Config.Parameter toChange = Config.parameters[toChangeId];
            final int step = Integer.valueOf(stepField.getText()),
                    upperBound = Integer.valueOf(upperBoundField.getText());
            architecture = Config.ARCHITECTURES[jComboBox.getSelectedIndex()];
            new Thread(() -> {
                try {
                    run(toChange, step, upperBound);
                } catch (FileNotFoundException e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                            "Can't create output files. Check log for details", "IO error", JOptionPane.ERROR_MESSAGE));
                    LOG.error(Throwables.getStackTraceAsString(e));
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                            "Unable to connect to server. Check log for details", "Connection error", JOptionPane.ERROR_MESSAGE));
                    LOG.error(Throwables.getStackTraceAsString(e));
                }
            }).start();
        });

        return options;
    }

    private static void updateGraph(int id, String name, ArrayList<XYChart.Data<Number, Double>> data) {
        charts[id].setData(FXCollections.observableArrayList(new XYChart.Series(name, FXCollections.observableArrayList(data))));
    }

    private static void run(Config.Parameter toChange, int step, int upperBound) throws IOException {
        try (PrintWriter description = new PrintWriter("output-description.txt")) {
            description.println("Start values:");

            for (Config.Parameter parameter : Config.parameters) {
                description.printf("%s = %d\n", parameter.getName(), parameter.get());
            }
            description.printf("Change %s from %d to %d with step %d\n",
                    toChange.getName(), toChange.get(), upperBound, step);
        }

        ArrayList<XYChart.Data<Number, Double>> requestHandleTimes = new ArrayList<>(),
                clientHandleTimes = new ArrayList<>(), clientTimes = new ArrayList<>();
        try (PrintWriter requestHandleFile = new PrintWriter("output-requestHandle.txt");
             PrintWriter clientHandleFile = new PrintWriter("output-clientHandle.txt");
             PrintWriter clientFile = new PrintWriter("output-client.txt")) {
            int changingValue = toChange.get();
            for (; changingValue <= upperBound; changingValue += step) {
                toChange.set(changingValue);
                sendToServer(ServerMain.REQUEST_OPEN);
                final ExecutorService taskExecutor = Executors.newCachedThreadPool();
                List<Future<Long>> results = new ArrayList<>();
                for (int i = 0; i < Config.clientsCount.get(); i++) {
                    results.add(taskExecutor.submit(() -> {
                        Timekeeper clientTimekeeper = new Timekeeper();
                        int timerId = clientTimekeeper.start();
                        architecture.clientType.constructor.get().run();
                        clientTimekeeper.finish(timerId);
                        return clientTimekeeper.getSum();
                    }));
                    if (architecture.serverType == ServerType.TCPProcess) {
                        try {
                            Thread.sleep(Config.NEW_PROCESS_DELAY);
                        } catch (InterruptedException e) {
                            LOG.error(Throwables.getStackTraceAsString(e));
                        }
                    }
                }
                taskExecutor.shutdown();

                long sum = 0;
                for (int i = 0; i < Config.clientsCount.get(); i++) {
                    try {
                        sum += results.get(i).get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.error(Throwables.getStackTraceAsString(e));
                    }
                }

                sendToServer(ServerMain.REQUEST_CLOSE);
                double clientTime = (double) sum / Config.clientsCount.get();

                requestHandleTimes.add(new XYChart.Data<>(changingValue, requestHandleTime));
                requestHandleFile.printf("%d\t%f\n", changingValue, requestHandleTime);

                clientHandleTimes.add(new XYChart.Data<>(changingValue, clientHandleTime));
                clientHandleFile.printf("%d\t%f\n", changingValue, clientHandleTime);

                clientTimes.add(new XYChart.Data<>(changingValue, clientTime));
                clientFile.printf("%d\t%f\n", changingValue, clientTime);

                Platform.runLater(() -> {
                    updateGraph(0, "requestHandleTime", requestHandleTimes);
                    updateGraph(1, "clientHandleTime", clientHandleTimes);
                    updateGraph(2, "clientTime", clientTimes);
                });
            }
        }
    }

    private ServerTestsMain() {
    }

    private static void sendToServer(int requestType) throws IOException {
        LOG.info("sendToServer {} {}", requestType, architecture.getName());
        try (Socket socket = new Socket(Config.serverAddress, Config.MAIN_SERVER_PORT);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeInt(requestType);
            if (requestType == ServerMain.REQUEST_OPEN) {
                dataOutputStream.writeInt(architecture.serverType.ordinal());
                dataOutputStream.writeInt(architecture.runnerType.ordinal());
            }
            dataOutputStream.flush();
            if (requestType == ServerMain.REQUEST_OPEN) {
                if (dataInputStream.readInt() != ServerMain.CONFIRM_SIGNAL) {
                    throw new IllegalStateException();
                }
            } else {
                requestHandleTime = dataInputStream.readDouble();
                clientHandleTime = dataInputStream.readDouble();
            }
        }
    }
}
