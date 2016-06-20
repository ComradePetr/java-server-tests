package ru.spbau.mit.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;
import ru.spbau.mit.architecture.RunnerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TCPServer extends Server {
    private final Logger log = LogManager.getLogger(this);
    protected ServerSocket serverSocket;

    public TCPServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        log.info("I will occupy {}", Config.SERVER_PORT);
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            this.serverSocket = serverSocket;
            while (true) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketException e) {
                    return;
                }
                int timerId = clientTimekeeper.start();

                runner.run(() -> {
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
                            socket.close();
                        } catch (IOException e) {
                            log.error(Throwables.getStackTraceAsString(e));
                        }
                    }
                });
            }
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                return;
            }
        }
    }
}
