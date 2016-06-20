package ru.spbau.mit.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;
import ru.spbau.mit.ServerMain;
import ru.spbau.mit.architecture.RunnerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;

public class TCPProcessServer extends TCPServer {
    private final Logger log = LogManager.getLogger(this);

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
            ServerMain.spawn();
            int timerId = clientTimekeeper.start();
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
        } catch (SocketException e) {
            return;
        } catch (IOException | URISyntaxException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
