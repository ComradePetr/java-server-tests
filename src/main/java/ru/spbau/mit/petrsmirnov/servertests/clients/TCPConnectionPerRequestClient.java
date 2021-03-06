package ru.spbau.mit.petrsmirnov.servertests.clients;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Класс TCP-клиента, устанавливающего на каждый запрос новое соединение с сервером.
 */
public class TCPConnectionPerRequestClient extends Client {
    private final Logger log = LogManager.getLogger(this);

    @Override
    public void run() throws IOException {
        for (int r = 0; r < Config.REQUESTS_COUNT.get(); r++, hangOn()) {
            try (Socket socket = new Socket(Config.serverAddress, Config.SERVER_PORT);
                 DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                sendArray(dataOutputStream);
                checkArray(receiveArray(dataInputStream));
            }
        }
    }
}
