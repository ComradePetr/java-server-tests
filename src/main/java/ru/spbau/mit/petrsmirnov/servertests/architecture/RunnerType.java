package ru.spbau.mit.petrsmirnov.servertests.architecture;

import ru.spbau.mit.petrsmirnov.servertests.ServerMain;

import java.util.function.Consumer;

/**
 * Перечисление типов запуска обработчика клиента.
 * Для каждого типа запуска хранится Consumer, который принимает Runnable, и запускает его.
 */
public enum RunnerType {
    MAIN_THREAD(Runnable::run),
    MANY_THREADS((r) -> new Thread(r).start()),
    CACHED_POOL(ServerMain.CACHED_THREAD_POOL::execute),
    FIXED_POOL(ServerMain.FIXED_THREAD_POOL::execute);

    private final Consumer<Runnable> runner;

    RunnerType(Consumer<Runnable> runner) {
        this.runner = runner;
    }

    public void run(Runnable runnable) {
        runner.accept(runnable);
    }
}
