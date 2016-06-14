package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servers.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ServerMain {
    public static final int REQUEST_OPEN = 0;
    public static final int REQUEST_CLOSE = 1;
    public static final int CONFIRM_SIGNAL = 17;
    private static final Logger LOG = LogManager.getLogger(ServerMain.class);

    public static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(),
            fixedThreadPool = Executors.newFixedThreadPool(Config.FIXED_THREAD_POOL_SIZE);

    private static Server server;

    public static void main(String[] args) {
        LOG.info("I will occupy {}", Config.MAIN_SERVER_PORT);
        try (ServerSocket serverSocket = new ServerSocket(Config.MAIN_SERVER_PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                    int requestType = dataInputStream.readInt();
                    LOG.info("type = {}", requestType);
                    if (requestType == REQUEST_OPEN) {
                        Config.ServerType serverType = Config.ServerType.values()[dataInputStream.readInt()];
                        RunnerType runnerType = RunnerType.values()[dataInputStream.readInt()];
                        LOG.info("server = {}, runner = {}", serverType, runnerType);

                        close();
                        open(serverType, runnerType);
                        dataOutputStream.writeInt(CONFIRM_SIGNAL);
                    } else {
                        dataOutputStream.writeDouble(server.requestHandleTime());
                        dataOutputStream.writeDouble(server.clientHandleTime());
                        close();
                    }
                    dataOutputStream.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ServerMain() {
    }

    private static void open(Config.ServerType serverType, RunnerType runnerType) {
        server = serverType.constructor.apply(runnerType);
        new Thread(server::run).start();
    }

    private static void close() {
        if (server != null) {
            server.close();
        }
        server = null;
    }
}
