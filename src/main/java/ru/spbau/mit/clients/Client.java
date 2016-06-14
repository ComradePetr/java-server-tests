package ru.spbau.mit.clients;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.Config;
import ru.spbau.mit.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class Client {
    private final Logger LOG = LogManager.getLogger(this);
    private final Random RND = new Random();

    public abstract void run();

    protected void sendArray(DataOutputStream dataOutputStream) throws IOException {
        List<Integer> list = RND.ints(Config.arraySize.get()).boxed().collect(Collectors.toList());
        byte[] byteArray = Protocol.Array.newBuilder().addAllContent(list).build().toByteArray();
        dataOutputStream.writeInt(byteArray.length);
        dataOutputStream.write(byteArray);
        dataOutputStream.flush();

        LOG.info("I just wrote array (size = {})", list.size());
    }

    protected List<Integer> receiveArray(DataInputStream dataInputStream) throws IOException {
        int size = dataInputStream.readInt();
        byte[] byteArray = new byte[size];
        dataInputStream.readFully(byteArray);
        return Protocol.Array.parseFrom(byteArray).getContentList();
    }

    protected void checkArray(List<Integer> array) {
        boolean first = true;
        int last = 0;
        for (int x : array) {
            if (!first && last > x) {
                throw new IllegalStateException("Unsorted array");
            }
            first = false;
            last = x;
        }
        LOG.info("Array (size = {}) is ok", array.size());
    }

    protected void hangOn() {
        try {
            Thread.sleep(Config.delay.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
