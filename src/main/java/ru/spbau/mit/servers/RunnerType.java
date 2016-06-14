package ru.spbau.mit.servers;

import ru.spbau.mit.ServerMain;

import java.util.function.Consumer;

public enum RunnerType {
    MAIN_THREAD(Runnable::run),
    MANY_THREADS((r) -> new Thread(r).start()),
    CACHED_POOL(ServerMain.cachedThreadPool::execute),
    FIXED_POOL(ServerMain.fixedThreadPool::execute);

    private Consumer<Runnable> runner;

    RunnerType(Consumer<Runnable> runner) {
        this.runner = runner;
    }

    void run(Runnable runnable) {
        runner.accept(runnable);
    }
}
