package ru.spbau.mit.servertests.architecture;

import ru.spbau.mit.servertests.servers.*;

import java.util.function.Function;

public enum ServerType {
    TCP(TCPServer::new),
    NIO(NIOServer::new),
    UDP(UDPServer::new),
    TCPProcess(TCPProcessServer::new);

    public final Function<RunnerType, Server> constructor;

    ServerType(Function<RunnerType, Server> constructor) {
        this.constructor = constructor;
    }
}
