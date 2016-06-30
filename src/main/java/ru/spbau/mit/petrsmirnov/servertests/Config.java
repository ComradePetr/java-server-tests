package ru.spbau.mit.petrsmirnov.servertests;

import ru.spbau.mit.petrsmirnov.servertests.architecture.Architecture;
import ru.spbau.mit.petrsmirnov.servertests.architecture.ClientType;
import ru.spbau.mit.petrsmirnov.servertests.architecture.RunnerType;
import ru.spbau.mit.petrsmirnov.servertests.architecture.ServerType;

/**
 * Класс конфигурации.
 * Хранит необходимые для работы программы значения и константы,
 * виды поддерживаемых архитектур.
 */
public final class Config {
    public static final int MAIN_SERVER_PORT = 17238, SERVER_PORT = 17239;
    public static final int FIXED_THREAD_POOL_SIZE = 4;
    public static final int UDP_PACKET_MAX_SIZE = 65536, UDP_TIMEOUT = 1000;
    public static final int NEW_PROCESS_DELAY = 1500;

    public static final Architecture ARCHITECTURES[] = new Architecture[]{
            new Architecture(ServerType.TCP, RunnerType.MANY_THREADS, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.CACHED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.MAIN_THREAD, ClientType.TCPConnectionPerRequest),

            new Architecture(ServerType.TCPProcess, RunnerType.MAIN_THREAD, ClientType.TCPOneConnection),

            new Architecture(ServerType.NIO, RunnerType.FIXED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.MAIN_THREAD, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.MANY_THREADS, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.CACHED_POOL, ClientType.TCPOneConnection),

            new Architecture(ServerType.UDP, RunnerType.MANY_THREADS, ClientType.UDP),
            new Architecture(ServerType.UDP, RunnerType.FIXED_POOL, ClientType.UDP)
    };

    public static final class Parameter {
        private final String name;
        private int value;

        public Parameter(String name) {
            this.name = name;
        }

        public void set(int value) {
            this.value = value;
        }

        public void set(String value) {
            this.value = Integer.parseInt(value);
        }

        public int get() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    public static final Parameter
            ARRAY_SIZE = new Parameter("Array size (N)"),
            CLIENTS_COUNT = new Parameter("Clients count (M)"),
            DELAY = new Parameter("Delay (∆)"),
            REQUESTS_COUNT = new Parameter("Requests count (X)");
    public static final Parameter PARAMETERS[] = new Parameter[]{ARRAY_SIZE, CLIENTS_COUNT, DELAY, REQUESTS_COUNT};
    public static String serverAddress;

    private Config() {
    }
}
