package ru.spbau.mit.servertests.clients;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servertests.Config;
import ru.spbau.mit.servertests.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Абстрактный класс клиента.
 * Включает в себя функцию записи случайного массива в представителя DataOutputStream,
 * функцию чтения присланного массива из представителя DataInputStream (используется protobuf),
 * проверки массива на отсортированность и ожидания клиента в течение заданного промежутка времени.
 */
public abstract class Client {
    private final Logger log = LogManager.getLogger(this);
    private final Random rnd = new Random();

    public abstract void run();

    protected void sendArray(DataOutputStream dataOutputStream) throws IOException {
        List<Integer> list = rnd.ints(Config.ARRAY_SIZE.get()).boxed().collect(Collectors.toList());
        byte[] byteArray = Protocol.Array.newBuilder().addAllContent(list).build().toByteArray();
        dataOutputStream.writeInt(byteArray.length);
        dataOutputStream.write(byteArray);
        dataOutputStream.flush();

        log.info("I just wrote array (size = {})", list.size());
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
        log.info("Array (size = {}) is ok", array.size());
    }

    protected void hangOn() {
        try {
            Thread.sleep(Config.DELAY.get());
        } catch (InterruptedException e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
