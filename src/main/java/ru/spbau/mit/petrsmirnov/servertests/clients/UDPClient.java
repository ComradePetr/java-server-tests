package ru.spbau.mit.petrsmirnov.servertests.clients;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;

import java.io.*;
import java.net.*;

/**
 * Класс UDP-клиента, отправляющего каждый запрос в своём пакете.
 */
public class UDPClient extends Client {
    private final Logger log = LogManager.getLogger(this);

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(Config.UDP_TIMEOUT);
            for (int r = 0; r < Config.REQUESTS_COUNT.get(); r++, hangOn()) {
                try {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
                        sendArray(dataOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length,
                                new InetSocketAddress(Config.serverAddress, Config.SERVER_PORT));
                        socket.send(packet);
                    }

                    byte[] byteArray = new byte[Config.UDP_PACKET_MAX_SIZE];
                    DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length);
                    try {
                        socket.receive(packet);
                        byte[] data = packet.getData();
                        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data))) {
                            checkArray(receiveArray(dataInputStream));
                        }
                    } catch (SocketTimeoutException e) {
                        log.warn("Can't wait for response more than {} ms", Config.UDP_TIMEOUT);
                        log.warn(Throwables.getStackTraceAsString(e));
                    }
                } catch (IOException e) {
                    log.error(Throwables.getStackTraceAsString(e));
                }
            }
        } catch (SocketException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
