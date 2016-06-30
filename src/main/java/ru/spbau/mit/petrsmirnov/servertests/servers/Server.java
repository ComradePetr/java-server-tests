package ru.spbau.mit.petrsmirnov.servertests.servers;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.petrsmirnov.servertests.Protocol;
import ru.spbau.mit.petrsmirnov.servertests.Timekeeper;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Абстрактный класс сервера-обработчика.
 * Хранит группы секундомеров для подсчёта среднего времени обработки клиента и запроса.
 * Хранит представителя RunnerType, который будет запускать переданного ему обработчика для очередного клиента.
 * Имеет функции для кодирования-декодирования массива для поддержки protobuf,
 * чтения массива из представителя DataInputStream и записи в представителя DataOutputStream,
 * обработки (сортировки) массива с запуском и остановкой секундомера, отвечающего за обработку запросов.
 */
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
        return requestTimekeeper.getAverage();
    }

    public double clientHandleTime() {
        return clientTimekeeper.getAverage();
    }

    protected class ArrayHandler {
        private int timerId;
        private boolean needClientTimekeeper;

        public ArrayHandler(boolean needClientTimekeeper) {
            this.needClientTimekeeper = needClientTimekeeper;
        }

        public ArrayList<Integer> decode(byte[] byteArray) throws InvalidProtocolBufferException {
            return new ArrayList<>(Protocol.Array.parseFrom(byteArray).getContentList());
        }

        public byte[] encode(ArrayList<Integer> array) {
            return Protocol.Array.newBuilder().addAllContent(array).build().toByteArray();
        }

        public ArrayList<Integer> receiveArray(DataInputStream dataInputStream) throws IOException {
            int size = dataInputStream.readInt();
            if (needClientTimekeeper) {
                timerId = clientTimekeeper.start();
            }
            byte[] byteArray = new byte[size];
            dataInputStream.readFully(byteArray);
            return decode(byteArray);
        }

        public ArrayList<Integer> handleArray(ArrayList<Integer> array) {
            int timerId = requestTimekeeper.start();
            Collections.sort(array);
            log.info("Server just sorted array (size = {})", array.size());
            requestTimekeeper.finish(timerId);
            return array;
        }

        public void sendArray(ArrayList<Integer> array, DataOutputStream dataOutputStream) throws IOException {
            byte[] byteArray = encode(array);
            dataOutputStream.writeInt(byteArray.length);
            dataOutputStream.write(byteArray);
            dataOutputStream.flush();
            if (needClientTimekeeper) {
                clientTimekeeper.finish(timerId);
            }

            log.info("I just wrote array (size = {})", array.size());
        }

        public void receiveHandleSendArray(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
            sendArray(handleArray(receiveArray(dataInputStream)), dataOutputStream);
        }
    }
}
