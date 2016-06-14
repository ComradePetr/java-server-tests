package ru.spbau.mit.architecture;

import ru.spbau.mit.clients.Client;
import ru.spbau.mit.clients.TCPConnectionPerRequestClient;
import ru.spbau.mit.clients.TCPOneConnectionClient;
import ru.spbau.mit.clients.UDPClient;

import java.util.function.Supplier;

public enum ClientType {
    TCPConnectionPerRequest(TCPConnectionPerRequestClient::new),
    TCPOneConnection(TCPOneConnectionClient::new),
    UDP(UDPClient::new);

    private Supplier<Client> constructor;

    ClientType(Supplier<Client> constructor) {
        this.constructor = constructor;
    }
}
