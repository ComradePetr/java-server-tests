package ru.spbau.mit.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servertests.Config;
import ru.spbau.mit.servertests.ServerMain;
import ru.spbau.mit.servertests.architecture.RunnerType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Сервер-обработчик для TCP-соединений, обрабатывает клиентов в разных процессах.
 * Ожидает клиента, после чего запускает новый сервер, а сам осуществляет обработку клиента в текущем процессе.
 * Статистика складывается в файл, соответствующий этому процессу;
 * при закрытии сервера-обработчика он собирает эту статистику из файлов.
 */
public class TCPProcessServer extends TCPServer {
    private final Logger log = LogManager.getLogger(this);
    private static final Path TIMEKEEPERS_DIR = Paths.get("..", "timekeepers");
    private static Double requestHandleTime = null, clientHandleTime = null;

    public TCPProcessServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        log.info("I will occupy {}", Config.SERVER_PORT);
        try {
            serverSocket = new ServerSocket(Config.SERVER_PORT);
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                return;
            }
            ServerMain.spawn();

            runner.run(() -> {
                try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                    while (!socket.isClosed()) {
                        try {
                            new ArrayHandler(true).receiveHandleSendArray(dataInputStream, dataOutputStream);
                        } catch (IOException e) {
                            return;
                        }
                    }
                } catch (IOException e) {
                    log.error(Throwables.getStackTraceAsString(e));
                } finally {
                    try {
                        saveTimekeepers();
                        socket.close();
                    } catch (IOException e) {
                        log.error(Throwables.getStackTraceAsString(e));
                    }
                }
            });
        } catch (SocketException e) {
            return;
        } catch (IOException | URISyntaxException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }finally {
            close();
        }
    }

    @Override
    public double requestHandleTime() {
        if (requestHandleTime == null) {
            loadTimekeepers();
        }
        return requestHandleTime;
    }

    @Override
    public double clientHandleTime() {
        if (clientHandleTime == null) {
            loadTimekeepers();
        }
        return clientHandleTime;
    }

    private void saveTimekeepers() throws FileNotFoundException {
        File timekeeperFile = TIMEKEEPERS_DIR.resolve(String.format("timekeepers-%d.txt", ServerMain.processNumber)).toFile();
        try (PrintWriter printWriter = new PrintWriter(timekeeperFile)) {
            printWriter.printf("%f %f\n", requestTimekeeper.getAverage(), clientTimekeeper.getAverage());
        }
    }

    private void loadTimekeepers() {
        requestHandleTime = clientHandleTime = 0.0;
        double requestHandleTimesSum = 0, clientHandleTimesSum = 0;
        int count = 0;
        try {
            for (Path filePath : Files.walk(TIMEKEEPERS_DIR).collect(Collectors.toList())) {
                if (Files.isRegularFile(filePath)) {
                    try (Scanner scanner = new Scanner(filePath)) {
                        double a = scanner.nextDouble(), b = scanner.nextDouble();
                        requestHandleTimesSum += a;
                        clientHandleTimesSum += b;
                        log.info("I read from {} : {} {}", filePath.toString(), a, b);
                    }
                    ++count;
                    filePath.toFile().delete();
                }
            }
            if (count > 0) {
                requestHandleTime = requestHandleTimesSum / count;
                clientHandleTime = clientHandleTimesSum / count;
            }
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
