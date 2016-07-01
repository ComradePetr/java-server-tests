package ru.spbau.mit.petrsmirnov.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;
import ru.spbau.mit.petrsmirnov.servertests.ServerMain;
import ru.spbau.mit.petrsmirnov.servertests.Timekeeper;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;

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
                        } catch (EOFException e) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.error(Throwables.getStackTraceAsString(e));
                } finally {
                    try {
                        saveTimekeepers();
                    } catch (IOException e) {
                        log.error(Throwables.getStackTraceAsString(e));
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.error(Throwables.getStackTraceAsString(e));
                    }
                }
            });
        } catch (IOException | URISyntaxException e) {
            log.error(Throwables.getStackTraceAsString(e));
        } finally {
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
        try {
            double requestHandleTimesSum = 0, clientHandleTimesSum = 0;
            int requestHandleTimesCount = 0, clientHandleTimesCount = 0;

            for (Path filePath : Files.walk(TIMEKEEPERS_DIR).collect(Collectors.toList())) {
                if (Files.isRegularFile(filePath)) {
                    try (Scanner scanner = new Scanner(filePath)) {
                        double a = scanner.nextDouble(), b = scanner.nextDouble();
                        if (a != Timekeeper.NO_FINISHED_TIMEKEEPERS) {
                            requestHandleTimesSum += a;
                            ++requestHandleTimesCount;
                        }
                        if (b != Timekeeper.NO_FINISHED_TIMEKEEPERS) {
                            clientHandleTimesSum += b;
                            ++clientHandleTimesCount;
                        }
                        log.info("I read from {} : {} {}", filePath.toString(), a, b);
                    }
                    filePath.toFile().delete();
                }
            }
            if (requestHandleTimesCount > 0) {
                requestHandleTime = requestHandleTimesSum / requestHandleTimesCount;
            }
            if (clientHandleTimesCount > 0) {
                clientHandleTime = clientHandleTimesSum / clientHandleTimesCount;
            }
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
