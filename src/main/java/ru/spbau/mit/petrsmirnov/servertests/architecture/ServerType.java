package ru.spbau.mit.petrsmirnov.servertests.architecture;

import ru.spbau.mit.petrsmirnov.servertests.servers.*;

import java.util.function.Function;

/**
 * Перечисление типов серверов. Про каждый тип сервера хранится конструктор его класса.
 */
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
