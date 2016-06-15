package ru.spbau.mit.architecture;

import ru.spbau.mit.servers.NIOServer;
import ru.spbau.mit.servers.Server;
import ru.spbau.mit.servers.TCPServer;
import ru.spbau.mit.servers.UDPServer;

import java.util.function.Function;

public enum ServerType {
    TCP(TCPServer::new),
    NIO(NIOServer::new),
    UDP(UDPServer::new);

    public Function<RunnerType, Server> constructor;

    ServerType(Function<RunnerType, Server> constructor) {
        this.constructor = constructor;
    }
}
