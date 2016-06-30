package ru.spbau.mit.petrsmirnov.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Сервер-обработчик для TCP-соединений, обрабатывает клиентов в текущем процессе.
 * Ожидает клиента, после чего запускает обработчик для этого нового клиента, используя runner,
 * и снова ждёт клиента.
 */
public class TCPServer extends Server {
    private final Logger log = LogManager.getLogger(this);
    protected ServerSocket serverSocket;

    public TCPServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        log.info("I will occupy {}", Config.SERVER_PORT);
        try {
            serverSocket = new ServerSocket(Config.SERVER_PORT);
            while (true) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketException e) {
                    return;
                }

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
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error(Throwables.getStackTraceAsString(e));
            }
        }
    }
}
