package ru.spbau.mit.architecture;

import ru.spbau.mit.servers.*;

import java.util.function.Function;

public enum ServerType {
    TCP(TCPServer::new), NIO(NIOServer::new), UDP(UDPServer::new);

    private Function<RunnerType, Server> constructor;

    ServerType(Function<RunnerType, Server> constructor) {
        this.constructor = constructor;
    }
}
