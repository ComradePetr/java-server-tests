package ru.spbau.mit.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;
import ru.spbau.mit.ServerMain;
import ru.spbau.mit.architecture.RunnerType;

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
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            this.serverSocket = serverSocket;
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                return;
            }
            int timerId = clientTimekeeper.start();
            ServerMain.spawn();
            try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                while (!socket.isClosed()) {
                    try {
                        sendArray(handleArray(receiveArray(dataInputStream)), dataOutputStream);
                    } catch (IOException e) {
                        return;
                    }
                }
            } catch (IOException e) {
                log.error(Throwables.getStackTraceAsString(e));
            } finally {
                clientTimekeeper.finish(timerId);
                try {
                    saveTimekeepers();
                    socket.close();
                } catch (IOException e) {
                    log.error(Throwables.getStackTraceAsString(e));
                }
            }
        } catch (SocketException e) {
            return;
        } catch (IOException | URISyntaxException e) {
            log.error(Throwables.getStackTraceAsString(e));
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
            printWriter.printf("%d %d\n", requestTimekeeper.getSum(), clientTimekeeper.getSum());
        }
    }

    private void loadTimekeepers() {
        requestHandleTime = clientHandleTime = 0.0;
        long requestHandleTimesSum = 0, clientHandleTimesSum = 0;
        int count = 0;
        try {
            for (Path filePath : Files.walk(TIMEKEEPERS_DIR).collect(Collectors.toList())) {
                if (Files.isRegularFile(filePath)) {
                    try (Scanner scanner = new Scanner(filePath)) {
                        long a = scanner.nextLong(), b = scanner.nextLong();
                        requestHandleTimesSum += a;
                        clientHandleTimesSum += b;
                        log.info("I read from {} : {} {}", filePath.toString(), a, b);
                    }
                    ++count;
                    filePath.toFile().delete();
                }
            }
            if (count > 0) {
                requestHandleTime = (double) requestHandleTimesSum / count;
                clientHandleTime = (double) clientHandleTimesSum / count;
            }
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
