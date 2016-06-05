package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UDPServer {
    public static final int HANDLE_TYPES_COUNT = 3;
    public static final int HANDLE_MANY_THREADS = 1;
    public static final int HANDLE_FIXED_POOL = 2;

    public static final int HANDLERS_COUNT = 2;
    public static final int HANDLER_CONNECTION_PER_REQUEST = 1;
    private static final int THREAD_POOL_SIZE = 4;

    private final int handleType;

    private static final Logger LOG = LogManager.getLogger(UDPClient.class);

    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final Timekeeper requestTimekeeper = new Timekeeper(), clientTimekeeper = new Timekeeper();

    private DatagramSocket serverSocket;

    public UDPServer(int handleType) {
        this.handleType = handleType;
    }

    public void run() {
        LOG.info("I will occupy {}", Config.SERVER_PORT);
        try (DatagramSocket serverSocket = new DatagramSocket(Config.SERVER_PORT)) {
            this.serverSocket = serverSocket;
            byte[] buf = new byte[1024];
            while (!serverSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    serverSocket.receive(packet);
                    clientTimekeeper.start();
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            byte[] data = packet.getData();

                            try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
                                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                 DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
                                int size = dataInputStream.readInt();
                                byte[] byteArray = new byte[size];
                                dataInputStream.readFully(byteArray);

                                requestTimekeeper.start();
                                List<Integer> array = new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
                                LOG.info("Server just read array (size = {})", array.size());
                                Collections.sort(array);
                                byteArray = Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
                                requestTimekeeper.finish();

                                byteArray = Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
                                dataOutputStream.writeInt(byteArray.length);
                                dataOutputStream.write(byteArray);
                                dataOutputStream.flush();
                                data = byteArrayOutputStream.toByteArray();
                                DatagramPacket packetResponse = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                                serverSocket.send(packetResponse);
                                clientTimekeeper.finish();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    if(handleType==HANDLE_FIXED_POOL)
                        taskExecutor.execute(r);
                    else
                        new Thread(r).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    public double requestHandleTime() {
        return requestTimekeeper.average();
    }

    public double clientHandleTime() {
        return clientTimekeeper.average();
    }
}