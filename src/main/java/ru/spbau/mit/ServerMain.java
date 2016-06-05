package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerMain {
    public static final int REQUEST_OPEN = 0;
    public static final int REQUEST_CLOSE = 1;
    private static final Logger LOG = LogManager.getLogger(ServerMain.class);
    private static TCPServer tcpServer;
    private static UDPServer udpServer;
    private static NIOServer nioServer;
    private static int serverType;

    public static void main(String[] args) {
        LOG.info("I will occupy {}", Config.MAIN_SERVER_PORT);
        try (ServerSocket serverSocket = new ServerSocket(Config.MAIN_SERVER_PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                    int requestType = dataInputStream.readInt();
                    LOG.info("type = {}", requestType);
                    if (requestType == REQUEST_OPEN) {
                        int handleType = dataInputStream.readInt(), handlerType = dataInputStream.readInt(), serverType=dataInputStream.readInt();
                        LOG.info("handle = {}, handler = {}", handleType, handlerType);
                        close();
                        open(handleType, handlerType, serverType);
                        dataOutputStream.writeInt(0);
                    } else {
                        dataOutputStream.writeDouble(tcpServer.requestHandleTime());
                        dataOutputStream.writeDouble(tcpServer.clientHandleTime());
                        tcpServer.close();
                    }
                    dataOutputStream.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void open(int handleType, int handlerType, int serverType) {
        ServerMain.serverType = serverType;
        if(serverType==0) {
            tcpServer = new TCPServer(handleType, handlerType);
            new Thread(tcpServer::run).start();
        }else if(serverType==1) {
            nioServer = new NIOServer();
            new Thread(nioServer::run).start();
        }else if(serverType==2) {
            udpServer = new UDPServer(handleType);
            new Thread(udpServer::run).start();
        }
    }

    private static void close() {
        if (tcpServer != null) {
            tcpServer.close();
        }
        tcpServer = null;
        if (udpServer != null) {
            udpServer.close();
        }
        udpServer = null;
        if (nioServer != null) {
            nioServer.close();
        }
        nioServer = null;
    }

    private ServerMain() {
    }
}
