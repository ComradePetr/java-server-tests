package ru.spbau.mit;

public final class Config {
    public static final int MAIN_SERVER_PORT = 17238, SERVER_PORT = 17239;

    public static Integer arraySize, clientsCount, delay, requestsCount;
    public static Integer values[] = new Integer[]{arraySize, clientsCount, delay, requestsCount};
    public static String serverAddress = "127.0.0.1";

    public static void reloadValues() {
        values = new Integer[]{arraySize, clientsCount, delay, requestsCount};
    }

    public static void set(int k, int v) {
        switch (k) {
            case 0:
                arraySize = v;
                break;
            case 1:
                clientsCount = v;
                break;
            case 2:
                delay = v;
                break;
            case 3:
                requestsCount = v;
                break;
        }
        reloadValues();
    }

    private Config() {
    }
}
