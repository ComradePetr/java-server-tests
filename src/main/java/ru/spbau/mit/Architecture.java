package ru.spbau.mit;

import ru.spbau.mit.servers.RunnerType;

public class Architecture {
    public final String name;
    public final Config.ServerType serverType;
    public final RunnerType runnerType;
    public final Config.ClientType clientType;

    public Architecture(Config.ServerType serverType, RunnerType runnerType, Config.ClientType clientType) {
        this.name = String.format("%s with %s, clientType = %s",
                serverType.toString(), runnerType.toString(), clientType.toString());
        this.serverType = serverType;
        this.runnerType = runnerType;
        this.clientType = clientType;
    }

    public String getName() {
        return name;
    }
}
