package ru.spbau.mit.servers;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Protocol;
import ru.spbau.mit.Timekeeper;
import ru.spbau.mit.architecture.RunnerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public abstract class Server {
    protected final RunnerType runner;
    protected final Timekeeper requestTimekeeper = new Timekeeper(), clientTimekeeper = new Timekeeper();
    private final Logger log = LogManager.getLogger(this);

    public Server(RunnerType runner) {
        this.runner = runner;
    }

    public abstract void run();

    public abstract void close();

    public double requestHandleTime() {
        return requestTimekeeper.average();
    }

    public double clientHandleTime() {
        return clientTimekeeper.average();
    }

    protected ArrayList<Integer> decode(byte[] byteArray) throws InvalidProtocolBufferException {
        return new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
    }

    protected byte[] encode(ArrayList<Integer> array) {
        return Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
    }

    protected ArrayList<Integer> receiveArray(DataInputStream dataInputStream) throws IOException {
        int size = dataInputStream.readInt();
        byte[] byteArray = new byte[size];
        dataInputStream.readFully(byteArray);
        return decode(byteArray);
    }

    protected ArrayList<Integer> handleArray(ArrayList<Integer> array) {
        int timerId = requestTimekeeper.start();
        Collections.sort(array);
        log.info("Server just sorted array (size = {})", array.size());
        requestTimekeeper.finish(timerId);
        return array;
    }

    protected void sendArray(ArrayList<Integer> array, DataOutputStream dataOutputStream) throws IOException {
        byte[] byteArray = encode(array);
        dataOutputStream.writeInt(byteArray.length);
        dataOutputStream.write(byteArray);
        dataOutputStream.flush();

        log.info("I just wrote array (size = {})", array.size());
    }
}
