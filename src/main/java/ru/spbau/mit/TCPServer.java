package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TCPServer {
    public static final int HANDLE_TYPES_COUNT = 3;
    public static final int HANDLE_MAIN_THREAD = 0;
    public static final int HANDLE_MANY_THREADS = 1;
    public static final int HANDLE_CACHED_POOL = 2;

    public static final int HANDLERS_COUNT = 2;
    public static final int HANDLER_CONNECTION_PER_CLIENT = 0;
    public static final int HANDLER_CONNECTION_PER_REQUEST = 1;

    private final int handleType;
    private final int handlerType;

    private static final Logger LOG = LogManager.getLogger(TCPServer.class);

    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final Consumer<Runnable> runners[] = new Consumer[HANDLE_TYPES_COUNT];
    private final Class<? extends TCPHandler> handlers[] = new Class[HANDLERS_COUNT];
    private final Timekeeper requestTimekeeper = new Timekeeper(), clientTimekeeper = new Timekeeper();

    private ServerSocket serverSocket;

    public TCPServer(int handleType, int handlerType) {
        runners[HANDLE_MAIN_THREAD] = Runnable::run;
        runners[HANDLE_MANY_THREADS] = (r) -> new Thread(r).start();
        runners[HANDLE_CACHED_POOL] = taskExecutor::execute;

        handlers[HANDLER_CONNECTION_PER_CLIENT] = ConnectionPerClientHandler.class;
        handlers[HANDLER_CONNECTION_PER_REQUEST] = ConnectionPerRequestHandler.class;

        this.handleType = handleType;
        this.handlerType = handlerType;
    }

    public void run() {
        LOG.info("I will occupy {}", Config.SERVER_PORT);
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            this.serverSocket = serverSocket;
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    clientTimekeeper.start();
                    handle(socket);
                    clientTimekeeper.finish();
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }

    public double requestHandleTime() {
        return requestTimekeeper.average();
    }

    public double clientHandleTime() {
        return clientTimekeeper.average();
    }

    private void handle(Socket socket) {
        runners[handleType].accept(() -> {
            TCPHandler handler;
            try {
                handler = handlers[handlerType].getConstructor(Socket.class, Timekeeper.class).
                        newInstance(socket, requestTimekeeper);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException();
            }
            handler.run();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
