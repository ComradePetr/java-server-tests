package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ConnectionPerRequestTCPClient {
    private final Logger LOG = LogManager.getLogger(this);
    private final Random rnd = new Random();

    public ConnectionPerRequestTCPClient() {
    }

    public void run() {
        for (int r = 0; r < Config.requestsCount; ++r) {
            try (Socket socket = new Socket(Config.serverAddress, Config.SERVER_PORT);
                 DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                List<Integer> list = rnd.ints(Config.arraySize).boxed().collect(Collectors.toList());
                LOG.info("I send array (size = {})", list.size());
                byte[] byteArray = Protocol.Array.newBuilder().addAllContent(
                        list
                ).build().toByteArray();
                dataOutputStream.writeInt(byteArray.length);
                dataOutputStream.write(byteArray);
                dataOutputStream.flush();

                int size = dataInputStream.readInt();
                byteArray = new byte[size];
                dataInputStream.readFully(byteArray);
                ArrayList<Integer> array = new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
                for (int i = 1; i < array.size(); i++) {
                    if (array.get(i) < array.get(i - 1)) {
                        throw new IllegalStateException();
                    }
                }
                LOG.info("I got sorted array");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(Config.delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
