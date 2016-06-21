package ru.spbau.mit.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servertests.Config;
import ru.spbau.mit.servertests.architecture.RunnerType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class NIOServer extends Server {
    private final Logger log = LogManager.getLogger(this);
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public NIOServer(RunnerType runnerType) {
        super(runnerType);
    }

    @Override
    public void run() {
        log.info("I will occupy {}", Config.SERVER_PORT);
        try {
            selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(Config.SERVER_PORT));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (selector.isOpen() && serverSocketChannel.isOpen()) {
                selector.select();
                for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); iterator.remove()) {
                    final SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isValid()) {
                        if (selectionKey.isAcceptable()) {
                            SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                                    new ClientData(clientTimekeeper.start()));
                        } else {
                            final ClientData clientData;
                            synchronized (selectionKey) {
                                clientData = (ClientData) selectionKey.attachment();
                            }
                            if (selectionKey.isReadable() && !clientData.writeMode) {
                                int read = clientData.read((SocketChannel) selectionKey.channel());
                                if (read == 0) {
                                    runner.run(() -> {
                                        try {
                                            ClientData newClientData = new ClientData(
                                                    clientData.timerId, encode(handleArray(decode(clientData.getByteArray())))
                                            );
                                            synchronized (selectionKey) {
                                                selectionKey.attach(newClientData);
                                            }
                                        } catch (IOException e) {
                                            return;
                                        }
                                    });
                                } else if (read == -1) {
                                    clientTimekeeper.finish(clientData.timerId);
                                    selectionKey.interestOps(0);
                                }
                            } else if (selectionKey.isWritable() && clientData.writeMode) {
                                if (!clientData.write((SocketChannel) selectionKey.channel())) {
                                    selectionKey.attach(new ClientData(clientData.timerId));
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClosedSelectorException e) {
            return;
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void close() {
        try {
            selector.close();
            serverSocketChannel.close();
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    private class ClientData {
        private int timerId;
        private ByteBuffer sizeBuffer, dataBuffer;
        private boolean writeMode;

        public ClientData(int timerId) {
            this.timerId = timerId;
            sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
            writeMode = false;
        }

        public ClientData(int timerId, byte[] byteArray) {
            this.timerId = timerId;
            dataBuffer = ByteBuffer.allocate(Integer.BYTES + byteArray.length);
            dataBuffer.putInt(byteArray.length);
            dataBuffer.put(byteArray);
            dataBuffer.flip();
            writeMode = true;
        }

        public int read(SocketChannel socketChannel) throws IOException {
            int wasRead;
            do {
                if (sizeBuffer.hasRemaining()) {
                    wasRead = socketChannel.read(sizeBuffer);
                } else {
                    if (dataBuffer == null) {
                        sizeBuffer.flip();
                        int x = sizeBuffer.getInt();
                        dataBuffer = ByteBuffer.allocate(x);
                    }
                    wasRead = socketChannel.read(dataBuffer);
                }
            } while (wasRead > 0);
            if (wasRead == -1) {
                return -1;
            }
            return sizeBuffer.hasRemaining() || dataBuffer == null || dataBuffer.hasRemaining() ? 1 : 0;
        }

        public byte[] getByteArray() {
            return dataBuffer.array();
        }

        public boolean write(SocketChannel socketChannel) throws IOException {
            socketChannel.write(dataBuffer);
            return dataBuffer.hasRemaining();
        }
    }
}
