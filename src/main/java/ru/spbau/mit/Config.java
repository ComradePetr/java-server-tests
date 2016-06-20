package ru.spbau.mit;

import ru.spbau.mit.architecture.Architecture;
import ru.spbau.mit.architecture.ClientType;
import ru.spbau.mit.architecture.RunnerType;
import ru.spbau.mit.architecture.ServerType;

public final class Config {
    public static final int MAIN_SERVER_PORT = 17238, SERVER_PORT = 17239;
    public static final int FIXED_THREAD_POOL_SIZE = 4;
    public static final int UDP_PACKET_MAX_SIZE = 1024, UDP_TIMEOUT = 1000;
    public static final int NEW_PROCESS_DELAY = 1500;

    public static final Architecture ARCHITECTURES[] = new Architecture[]{
            new Architecture(ServerType.TCPProcess, RunnerType.MAIN_THREAD, ClientType.TCPOneConnection),

            new Architecture(ServerType.TCP, RunnerType.MANY_THREADS, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.CACHED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.MAIN_THREAD, ClientType.TCPConnectionPerRequest),

            new Architecture(ServerType.NIO, RunnerType.FIXED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.MAIN_THREAD, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.MANY_THREADS, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.CACHED_POOL, ClientType.TCPOneConnection),

            new Architecture(ServerType.UDP, RunnerType.MANY_THREADS, ClientType.UDP),
            new Architecture(ServerType.UDP, RunnerType.FIXED_POOL, ClientType.UDP)
    };

    public static final class Parameter {
        private String name;
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

    public static Parameter
            arraySize = new Parameter("Array size (N)"),
            clientsCount = new Parameter("Clients count (M)"),
            delay = new Parameter("Delay (∆)"),
            requestsCount = new Parameter("Requests count (X)");
    public static Parameter parameters[] = new Parameter[]{arraySize, clientsCount, delay, requestsCount};
    public static String serverAddress;

    private Config() {
    }
}
