package ru.spbau.mit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ServerTestsMain {
    private static final Logger LOG = LogManager.getLogger(ServerTestsMain.class);
    private static double requestHandleTime, clientHandleTime, clientTime;
    private static final String values[] = new String[]{"N (arraySize)", "M (clientsCount)", "∆ (delay)", "X (requestsCount)"};
    private static XYChart<Number, Number> chart;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        JFrame frame = new JFrame("ServerTests");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.add(createGraph());
        jPanel.add(createOptions());

        frame.add(jPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel createGraph() {
        JPanel chartPanel = new JPanel();
        JFXPanel jfxPanel = new JFXPanel();
        jfxPanel.setScene(new Scene(chart = new LineChart<>(new NumberAxis(), new NumberAxis())));
        chartPanel.add(jfxPanel);
        return chartPanel;
    }

    private static JPanel createOptions() {
        JPanel options = new JPanel(new GridLayout(10, 6));

        ButtonGroup group = new ButtonGroup();

        JRadioButton nButton = new JRadioButton(values[0] + ":");
        group.add(nButton);
        options.add(nButton);
        JTextField arraySize = new JTextField("10");
        options.add(arraySize);

        JRadioButton mButton = new JRadioButton(values[1] + ":");
        group.add(mButton);
        options.add(mButton);
        JTextField clientsCount = new JTextField("2");
        options.add(clientsCount);

        JRadioButton dButton = new JRadioButton(values[2] + ":");
        group.add(dButton);
        options.add(dButton);
        JTextField delay = new JTextField("100");
        options.add(delay);

        options.add(new JLabel(values[3] + ":"));
        JTextField requestsCount = new JTextField("4");
        options.add(requestsCount);

        options.add(new JLabel("Upper bound:"));
        JTextField upperBound = new JTextField("50");
        options.add(upperBound);

        options.add(new JLabel("Step:"));
        JTextField step = new JTextField("10");
        options.add(step);

        options.add(new JLabel("Server's IP:"));
        JTextField serverAddress = new JTextField("127.0.0.1");
        options.add(serverAddress);

        JButton startButton = new JButton("Start");
        options.add(startButton);

        JComboBox<String> jComboBox = new JComboBox<>(new String[]{
                "TCP-one-thread",
                "TCP-one-cachedpool",
                "TCP-nonblocking",
                "TCP-each-request-onethread",
                "UDP-thread",
                "UDP-fixpool"
        });
        options.add(jComboBox);

        startButton.addActionListener((e) -> {
            Config.arraySize = Integer.parseInt(arraySize.getText());
            Config.clientsCount = Integer.parseInt(clientsCount.getText());
            Config.delay = Integer.parseInt(delay.getText());
            Config.requestsCount = Integer.parseInt(requestsCount.getText());
            Config.serverAddress = serverAddress.getText();
            int handleType, handlerType, serverType;
            switch (jComboBox.getSelectedIndex()) {
                case 0:
                    handleType = TCPServer.HANDLE_MANY_THREADS;
                    handlerType = TCPServer.HANDLER_CONNECTION_PER_CLIENT;
                    serverType = 0;
                    break;
                case 1:
                    handleType = TCPServer.HANDLE_CACHED_POOL;
                    handlerType = TCPServer.HANDLER_CONNECTION_PER_CLIENT;
                    serverType = 0;
                    break;
                case 2:
                    handleType = 0;
                    handlerType = 1;
                    serverType = 1;
                    break;
                case 3:
                    handleType = TCPServer.HANDLE_MAIN_THREAD;
                    handlerType = TCPServer.HANDLER_CONNECTION_PER_REQUEST;
                    serverType = 0;
                    break;
                case 4:
                    handleType = UDPServer.HANDLE_MANY_THREADS;
                    handlerType = 0;
                    serverType = 2;
                    break;
                case 5:
                    handleType = UDPServer.HANDLE_FIXED_POOL;
                    handlerType = 0;
                    serverType = 2;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            new Thread(() -> start(
                    nButton.isSelected() ? 0 : mButton.isSelected() ? 1 : 2,
                    Integer.valueOf(step.getText()),
                    Integer.valueOf(upperBound.getText()),
                    handleType, handlerType, serverType
            )
            ).start();
        });

        return options;
    }

    private static void start(int number, int step, int upperBound, int handleType, int handlerType, int serverType) {
        Config.reloadValues();
        try (PrintWriter description = new PrintWriter("output-description.txt")) {
            description.println("Start values:");

            for (int i = 0; i < values.length; i++) {
                description.printf("%s = %d\n", values[i], Config.values[i]);
            }
            description.printf("Change %s from %d to %d with step %d\n",
                    values[number], Config.values[number], upperBound, step);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<XYChart.Data<Number, Double>> requestHandleTimes = new ArrayList<>(),
                clientHandleTimes = new ArrayList<>(), clientTimes = new ArrayList<>();
        try (PrintWriter requestHandleFile = new PrintWriter("output-requestHandle.txt");
             PrintWriter clientHandleFile = new PrintWriter("output-clientHandle.txt");
             PrintWriter clientFile = new PrintWriter("output-client.txt")) {
            int changing = Config.values[number];
            for (; changing <= upperBound; changing += step) {
                Config.set(number, changing);
                sendToServer(ServerMain.REQUEST_OPEN, handleType, handlerType, serverType);
                final ExecutorService taskExecutor = Executors.newCachedThreadPool();
                Future<Long> results[] = new Future[Config.clientsCount];
                for (int i = 0; i < Config.clientsCount; i++) {
                    results[i] = taskExecutor.submit(() -> {
                        Timekeeper clientTimekeeper = new Timekeeper();
                        clientTimekeeper.start();

                        if (serverType == 0 || serverType == 1) {
                            if (handlerType == TCPServer.HANDLER_CONNECTION_PER_CLIENT) {
                                new OneConnectionTCPClient().run();
                            } else {
                                new ConnectionPerRequestTCPClient().run();
                            }
                        } else {
                            new UDPClient().run();
                        }

                        clientTimekeeper.finish();
                        return clientTimekeeper.getSum();
                    });
                }
                taskExecutor.shutdown();

                long sum = 0;
                for (int i = 0; i < Config.clientsCount; i++) {
                    try {
                        sum += results[i].get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                sendToServer(ServerMain.REQUEST_CLOSE, -1, -1, 0);
                clientTime = (double)sum / Config.clientsCount;

                requestHandleTimes.add(new XYChart.Data<>(changing, requestHandleTime));
                requestHandleFile.printf("%d\t%f\n", changing, requestHandleTime);

                clientHandleTimes.add(new XYChart.Data<>(changing, clientHandleTime));
                clientHandleFile.printf("%d\t%f\n", changing, clientHandleTime);

                clientTimes.add(new XYChart.Data<>(changing, clientTime));
                clientFile.printf("%d\t%f\n", changing, clientTime);

                Platform.runLater(() ->
                        chart.setData(FXCollections.observableArrayList(
                                new XYChart.Series("requestHandleTime", FXCollections.observableArrayList(requestHandleTimes)),
                                new XYChart.Series("clientHandleTime", FXCollections.observableArrayList(clientHandleTimes)),
                                new XYChart.Series("clientTime", FXCollections.observableArrayList(clientTimes))
                        ))
                );
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private ServerTestsMain() {
    }

    private static void sendToServer(int type, int handleType, int handlerType, int serverType) {
        LOG.info("sendToServer {} {} {} {}", type, handleType, handlerType, serverType);
        try (Socket socket = new Socket(Config.serverAddress, Config.MAIN_SERVER_PORT);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeInt(type);
            if (type == ServerMain.REQUEST_OPEN) {
                dataOutputStream.writeInt(handleType);
                dataOutputStream.writeInt(handlerType);
                dataOutputStream.writeInt(serverType);
            }
            dataOutputStream.flush();
            if (type == ServerMain.REQUEST_OPEN) {
                if (dataInputStream.readInt() != 0) {
                    throw new IllegalStateException();
                }
            } else {
                requestHandleTime = dataInputStream.readDouble();
                clientHandleTime = dataInputStream.readDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
