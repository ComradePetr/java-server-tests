package ru.spbau.mit;

import ru.spbau.mit.clients.Client;
import ru.spbau.mit.clients.TCPConnectionPerRequestClient;
import ru.spbau.mit.clients.TCPOneConnectionClient;
import ru.spbau.mit.clients.UDPClient;
import ru.spbau.mit.servers.*;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Config {
    public static final int MAIN_SERVER_PORT = 17238, SERVER_PORT = 17239;
    public static final int FIXED_THREAD_POOL_SIZE = 4;
    public static final int UDP_PACKET_MAX_SIZE = 1024, UDP_TIMEOUT = 1000;

    public enum ServerType {
        TCP(TCPServer::new), NIO(NIOServer::new), UDP(UDPServer::new);

        Function<RunnerType, Server> constructor;

        ServerType(Function<RunnerType, Server> constructor) {
            this.constructor = constructor;
        }
    }

    public enum ClientType {
        TCPConnectionPerRequest(TCPConnectionPerRequestClient::new),
        TCPOneConnection(TCPOneConnectionClient::new),
        UDP(UDPClient::new);

        Supplier<Client> constructor;

        ClientType(Supplier<Client> constructor) {
            this.constructor = constructor;
        }
    }

    public static final Architecture architectures[] = new Architecture[]{
            new Architecture(ServerType.TCP, RunnerType.MANY_THREADS, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.CACHED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.NIO, RunnerType.FIXED_POOL, ClientType.TCPOneConnection),
            new Architecture(ServerType.TCP, RunnerType.MAIN_THREAD, ClientType.TCPConnectionPerRequest),
            new Architecture(ServerType.UDP, RunnerType.MANY_THREADS, ClientType.UDP),
            new Architecture(ServerType.UDP, RunnerType.FIXED_POOL, ClientType.UDP),
    };

    public final static class Parameter {
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
