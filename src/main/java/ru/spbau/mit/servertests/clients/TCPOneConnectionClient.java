package ru.spbau.mit.servertests.clients;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servertests.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Класс TCP-клиента, устанавливающего одно соединение с сервером на все запросы.
 */
public class TCPOneConnectionClient extends Client {
    private final Logger log = LogManager.getLogger(this);

    @Override
    public void run() {
        try (Socket socket = new Socket(Config.serverAddress, Config.SERVER_PORT);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            for (int r = 0; r < Config.REQUESTS_COUNT.get(); r++, hangOn()) {
                sendArray(dataOutputStream);
                checkArray(receiveArray(dataInputStream));
            }
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
