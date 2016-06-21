package ru.spbau.mit.servertests.architecture;

import ru.spbau.mit.servertests.clients.Client;
import ru.spbau.mit.servertests.clients.TCPConnectionPerRequestClient;
import ru.spbau.mit.servertests.clients.TCPOneConnectionClient;
import ru.spbau.mit.servertests.clients.UDPClient;

import java.util.function.Supplier;

public enum ClientType {
    TCPConnectionPerRequest(TCPConnectionPerRequestClient::new),
    TCPOneConnection(TCPOneConnectionClient::new),
    UDP(UDPClient::new);

    public final Supplier<Client> constructor;

    ClientType(Supplier<Client> constructor) {
        this.constructor = constructor;
    }
}
