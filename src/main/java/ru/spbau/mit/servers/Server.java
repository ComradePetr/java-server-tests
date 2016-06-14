package ru.spbau.mit.servers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Protocol;
import ru.spbau.mit.Timekeeper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Server {
    protected final RunnerType runnerType;
    protected final Timekeeper requestTimekeeper = new Timekeeper(), clientTimekeeper = new Timekeeper();
    private final Logger LOG = LogManager.getLogger(this);

    public Server(RunnerType runnerType) {
        this.runnerType = runnerType;
    }

    public abstract void run();

    public abstract void close();

    public double requestHandleTime() {
        return requestTimekeeper.average();
    }

    public double clientHandleTime() {
        return clientTimekeeper.average();
    }

    protected ArrayList<Integer> receiveArray(DataInputStream dataInputStream) throws IOException {
        int size = dataInputStream.readInt();
        byte[] byteArray = new byte[size];
        dataInputStream.readFully(byteArray);
        return new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
    }

    protected ArrayList<Integer> handleArray(ArrayList<Integer> array) {
        int timerId = requestTimekeeper.start();
        Collections.sort(array);
        LOG.info("Server just sorted array (size = {})", array.size());
        requestTimekeeper.finish(timerId);
        return array;
    }

    protected void sendArray(ArrayList<Integer> array, DataOutputStream dataOutputStream) throws IOException {
        byte[] byteArray = Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
        dataOutputStream.writeInt(byteArray.length);
        dataOutputStream.write(byteArray);
        dataOutputStream.flush();

        LOG.info("I just wrote array (size = {})", array.size());
    }
}
