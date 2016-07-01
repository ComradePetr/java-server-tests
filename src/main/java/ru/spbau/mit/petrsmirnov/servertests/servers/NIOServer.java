package ru.spbau.mit.petrsmirnov.servertests.servers;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Config;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Сервер-обработчик для TCP-соединений, используя неблокирующую обработку клиентов.
 * Создаёт селектор, на который регистрируются канал сервера и каждый из каналов, полученных при подключении нового клиента.
 * Обрабатываем получаемые от селектора события:
 * либо подключение нового клиента, регистрируем его,
 * либо можно получать данные от клиента, читаем, сколько возможно,
 * либо мы уже отправляем клиенту ответ, отправляем, сколько возможно.
 */
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
                try {
                    selector.select();
                    for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); iterator.remove()) {
                        final SelectionKey selectionKey = iterator.next();

                        if (selectionKey.isValid()) {
                            if (selectionKey.isAcceptable()) {
                                try {
                                    SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
                                    socketChannel.configureBlocking(false);
                                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new ClientData());
                                } catch (IOException e) {
                                    log.error(Throwables.getStackTraceAsString(e));
                                }
                            } else {
                                final ClientData clientData;
                                synchronized (selectionKey) {
                                    clientData = (ClientData) selectionKey.attachment();
                                }
                                if (selectionKey.isReadable() && !clientData.writeMode) {
                                    try {
                                        int read = clientData.read((SocketChannel) selectionKey.channel());
                                        if (read == 0) {
                                            runner.run(() -> {
                                                ArrayHandler arrayHandler = new ArrayHandler(false);
                                                try {
                                                    ClientData newClientData = new ClientData(clientData,
                                                            arrayHandler.encode(arrayHandler.handleArray(
                                                                    arrayHandler.decode(clientData.getByteArray()))));
                                                    synchronized (selectionKey) {
                                                        selectionKey.attach(newClientData);
                                                    }
                                                } catch (IOException e) {
                                                    log.error(Throwables.getStackTraceAsString(e));
                                                    synchronized (selectionKey) {
                                                        selectionKey.cancel();
                                                    }
                                                }
                                            });
                                        } else if (read == -1) {
                                            selectionKey.cancel();
                                        }
                                    } catch (IOException e) {
                                        log.error(Throwables.getStackTraceAsString(e));
                                        selectionKey.cancel();
                                    }
                                } else if (selectionKey.isWritable() && clientData.writeMode) {
                                    try {
                                        if (!clientData.write((SocketChannel) selectionKey.channel())) {
                                            clientTimekeeper.finish(clientData.timerId);
                                            selectionKey.attach(new ClientData());
                                        }
                                    } catch (IOException e) {
                                        log.error(Throwables.getStackTraceAsString(e));
                                        selectionKey.cancel();
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

        public ClientData() {
            writeMode = false;
        }

        public ClientData(ClientData clientData, byte[] byteArray) {
            timerId = clientData.timerId;
            dataBuffer = ByteBuffer.allocate(Integer.BYTES + byteArray.length);
            dataBuffer.putInt(byteArray.length);
            dataBuffer.put(byteArray);
            dataBuffer.flip();
            writeMode = true;
        }

        public int read(SocketChannel socketChannel) throws IOException {
            int wasRead;
            do {
                if (sizeBuffer == null) {
                    timerId = clientTimekeeper.start();
                    sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
                }
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
            return sizeBuffer == null || sizeBuffer.hasRemaining() || dataBuffer == null || dataBuffer.hasRemaining() ? 1 : 0;
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
