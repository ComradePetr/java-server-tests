package ru.spbau.mit.architecture;

public class Architecture {
    public final ServerType serverType;
    public final RunnerType runnerType;
    public final ClientType clientType;

    private final String name;

    public Architecture(ServerType serverType, RunnerType runnerType, ClientType clientType) {
        this.name = String.format("%s with %s, clientType = %s", serverType.toString(), runnerType.toString(), clientType.toString());
        this.serverType = serverType;
        this.runnerType = runnerType;
        this.clientType = clientType;
    }

    public String getName() {
        return name;
    }
}
