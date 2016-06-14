package ru.spbau.mit.clients;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPConnectionPerRequestClient extends Client {
    private final Logger LOG = LogManager.getLogger(this);

    @Override
    public void run() {
        for (int r = 0; r < Config.requestsCount.get(); r++, hangOn()) {
            try (Socket socket = new Socket(Config.serverAddress, Config.SERVER_PORT);
                 DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                sendArray(dataOutputStream);
                checkArray(receiveArray(dataInputStream));
            } catch (IOException e) {
                LOG.error(Throwables.getStackTraceAsString(e));
            }
        }
    }
}
