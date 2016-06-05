package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class UDPClient {
    private static final Logger LOG = LogManager.getLogger(UDPClient.class);
    private final Random rnd = new Random();

    public UDPClient() {
    }

    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            try {
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                     DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
                    List<Integer> list = rnd.ints(Config.arraySize).boxed().collect(Collectors.toList());
                    LOG.info("I send array (size = {})", list.size());
                    byte[] byteArray = Protocol.Array.newBuilder().addAllContent(list).build().toByteArray();
                    LOG.info("length = {}", byteArray.length);
                    dataOutputStream.writeInt(byteArray.length);
                    dataOutputStream.write(byteArray);
                    dataOutputStream.flush();
                    byte[] buf = byteArrayOutputStream.toByteArray();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress(Config.serverAddress, Config.SERVER_PORT));
                    socket.send(packet);
                }

                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                byte[] data = packet.getData();
                try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data))) {
                    int size = dataInputStream.readInt();
                    byte[] byteArray = new byte[size];
                    dataInputStream.readFully(byteArray);
                    List<Integer> array = new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());

                    for (int i = 1; i < array.size(); i++) {
                        if (array.get(i) < array.get(i - 1)) {
                            throw new IllegalStateException();
                        }
                    }
                    LOG.info("Array (size = {}) is ok", array.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}