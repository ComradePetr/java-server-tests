package ru.spbau.mit.petrsmirnov.servertests.architecture;

import ru.spbau.mit.petrsmirnov.servertests.clients.Client;
import ru.spbau.mit.petrsmirnov.servertests.clients.TCPConnectionPerRequestClient;
import ru.spbau.mit.petrsmirnov.servertests.clients.TCPOneConnectionClient;
import ru.spbau.mit.petrsmirnov.servertests.clients.UDPClient;

import java.util.function.Supplier;

/**
 * Перечисление типов клиентов. Про каждый тип клиента хранится конструктор его класса.
 */
public enum ClientType {
    TCPConnectionPerRequest(TCPConnectionPerRequestClient::new),
    TCPOneConnection(TCPOneConnectionClient::new),
    UDP(UDPClient::new);

    public final Supplier<Client> constructor;

    ClientType(Supplier<Client> constructor) {
        this.constructor = constructor;
    }
}
