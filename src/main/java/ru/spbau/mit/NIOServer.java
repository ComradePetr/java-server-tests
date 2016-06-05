package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NIOServer {
    private final Logger LOG = LogManager.getLogger(this);
    private final int THREAD_POOL_SIZE = 4;

    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final Timekeeper requestTimekeeper = new Timekeeper(), clientTimekeeper = new Timekeeper();
    ServerSocketChannel serverChannel;

    public NIOServer() {
    }

    public void run() {
        LOG.info("I will occupy {}", Config.SERVER_PORT);
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(Config.SERVER_PORT));
            serverChannel.configureBlocking(false);
            while (serverChannel.isOpen()) {
                SocketChannel socketChannel = serverChannel.accept();
                if (socketChannel != null) {
                    taskExecutor.submit(() -> {
                        clientTimekeeper.start();
                        try {
                            ByteBuffer sizeBuf = ByteBuffer.allocate(Integer.BYTES);
                            socketChannel.read(sizeBuf);
                            sizeBuf.flip();
                            int size = sizeBuf.getInt();
                            ByteBuffer arrayBuf = ByteBuffer.allocate(size * Integer.BYTES);
                            socketChannel.read(arrayBuf);
                            arrayBuf.flip();

                            requestTimekeeper.start();
                            ArrayList<Integer> list = new ArrayList<Integer>(Protocol.Array.parseFrom(arrayBuf.array()).getContentList());
                            Collections.sort(list);
                            sizeBuf.clear();
                            arrayBuf.clear();
                            byte[] byteArray = Protocol.Array.newBuilder().addAllContent(list).build().toByteArray();
                            requestTimekeeper.finish();

                            sizeBuf.putInt(byteArray.length);
                            arrayBuf.put(byteArray);
                            sizeBuf.flip();
                            arrayBuf.flip();
                            while (sizeBuf.hasRemaining()) {
                                socketChannel.write(sizeBuf);
                            }
                            while (arrayBuf.hasRemaining()) {
                                socketChannel.write(arrayBuf);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            clientTimekeeper.finish();
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException e) {
        }
    }

    public double requestHandleTime() {
        return requestTimekeeper.average();
    }

    public double clientHandleTime() {
        return clientTimekeeper.average();
    }
}
