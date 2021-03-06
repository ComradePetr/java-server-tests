package ru.spbau.mit.petrsmirnov.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Сервер-обработчик для UDP-соединений.
 * Ожидает клиента, после чего запускает обработчик для этого нового клиента, используя runner,
 * и снова ждёт клиента.
 */
public class UDPServer extends Server {
    private final Logger log = LogManager.getLogger(this);
    private DatagramSocket serverSocket;

    public UDPServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        log.info("I will occupy {}", Config.SERVER_PORT);
        try {
            serverSocket = new DatagramSocket(Config.SERVER_PORT);
            while (!serverSocket.isClosed()) {
                byte[] byteArray = new byte[Config.UDP_PACKET_MAX_SIZE];
                final DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length);
                try {
                    serverSocket.receive(packet);
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    log.error(Throwables.getStackTraceAsString(e));
                    continue;
                }
                int timerId = clientTimekeeper.start();

                runner.run(() -> {
                    byte[] data = packet.getData();
                    try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
                         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
                        new ArrayHandler(false).receiveHandleSendArray(dataInputStream, dataOutputStream);
                        data = byteArrayOutputStream.toByteArray();
                        DatagramPacket packetResponse = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                        serverSocket.send(packetResponse);
                        clientTimekeeper.finish(timerId);
                    } catch (IOException e) {
                        log.error(Throwables.getStackTraceAsString(e));
                    }
                });
            }
        } catch (SocketException e) {
            log.error(Throwables.getStackTraceAsString(e));
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
