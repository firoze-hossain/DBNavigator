package com.roze.dbnavigator.util;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Central background thread pool for all database work (never block the FX thread). */
public final class AppExecutor {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "db-worker");
        t.setDaemon(true);
        return t;
    });

    private AppExecutor() {}

    public static void run(Task<?> task) {
        POOL.submit(task);
    }

    public static void run(Runnable runnable) {
        POOL.submit(runnable);
    }

    public static void shutdown() {
        POOL.shutdownNow();
    }
}
