package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TCPHandler {
    private static final Logger LOG = LogManager.getLogger(TCPHandler.class);
    protected final Timekeeper requestTimekeeper;
    protected Socket socket;

    public TCPHandler(Socket socket, Timekeeper requestTimekeeper) {
        this.requestTimekeeper = requestTimekeeper;
        this.socket = socket;
    }

    public abstract void run();

    protected void handleArray(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        int size = dataInputStream.readInt();
        byte[] byteArray = new byte[size];
        dataInputStream.readFully(byteArray);

        requestTimekeeper.start();
        List<Integer> array = new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
        LOG.info("Server just read array (size = {})", array.size());
        Collections.sort(array);
        byteArray = Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
        requestTimekeeper.finish();

        dataOutputStream.writeInt(byteArray.length);
        dataOutputStream.write(byteArray);
        dataOutputStream.flush();
    }
}
