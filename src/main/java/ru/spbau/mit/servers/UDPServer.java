package ru.spbau.mit.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer extends Server {
    private final Logger LOG = LogManager.getLogger(this);
    private DatagramSocket serverSocket;

    public UDPServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        LOG.info("I will occupy {}", Config.SERVER_PORT);
        try (DatagramSocket serverSocket = new DatagramSocket(Config.SERVER_PORT)) {
            this.serverSocket = serverSocket;
            while (!serverSocket.isClosed()) {
                try {
                    byte[] byteArray = new byte[Config.UDP_PACKET_MAX_SIZE];
                    final DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length);
                    serverSocket.receive(packet);
                    final int timerId = clientTimekeeper.start();

                    runnerType.run(() -> {
                        byte[] data = packet.getData();
                        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
                             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
                            sendArray(handleArray(receiveArray(dataInputStream)), dataOutputStream);
                            data = byteArrayOutputStream.toByteArray();
                            DatagramPacket packetResponse = new DatagramPacket(data, data.length,
                                    packet.getAddress(), packet.getPort());
                            serverSocket.send(packetResponse);
                        } catch (IOException e) {
                            LOG.error(Throwables.getStackTraceAsString(e));
                        } finally {
                            clientTimekeeper.finish(timerId);
                        }
                    });
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    LOG.error(Throwables.getStackTraceAsString(e));
                }
            }
        } catch (SocketException e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}