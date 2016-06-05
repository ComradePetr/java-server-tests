package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ConnectionPerClientHandler extends TCPHandler {
    public ConnectionPerClientHandler(Socket socket, Timekeeper requestTimekeeper) {
        super(socket, requestTimekeeper);
    }

    @Override
    public void run() {
        try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                handleArray(dataInputStream, dataOutputStream);
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
