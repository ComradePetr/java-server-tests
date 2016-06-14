package ru.spbau.mit.servers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;
import ru.spbau.mit.Protocol;
import ru.spbau.mit.architecture.RunnerType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;

public class NIOServer extends Server {
    private final Logger LOG = LogManager.getLogger(this);
    private ServerSocketChannel serverChannel;

    public NIOServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        LOG.info("I will occupy {}", Config.SERVER_PORT);
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(Config.SERVER_PORT));
            serverChannel.configureBlocking(false);
            while (serverChannel.isOpen()) {
                SocketChannel socketChannel = serverChannel.accept();
                if (socketChannel != null) {
                    runnerType.run(() -> {
                        int clientTimerId = clientTimekeeper.start();
                        try {
                            ByteBuffer sizeBuf = ByteBuffer.allocate(Integer.BYTES);
                            socketChannel.read(sizeBuf);
                            sizeBuf.flip();
                            int size = sizeBuf.getInt();
                            ByteBuffer arrayBuf = ByteBuffer.allocate(size * Integer.BYTES);
                            socketChannel.read(arrayBuf);
                            arrayBuf.flip();

                            int timerId = requestTimekeeper.start();
                            ArrayList<Integer> list = new ArrayList<>(Protocol.Array.parseFrom(arrayBuf.array()).getContentList());
                            Collections.sort(list);
                            sizeBuf.clear();
                            arrayBuf.clear();
                            byte[] byteArray = Protocol.Array.newBuilder().addAllContent(list).build().toByteArray();
                            requestTimekeeper.finish(timerId);

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
                            clientTimekeeper.finish(clientTimerId);
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException e) {
            return;
        }
    }
}
