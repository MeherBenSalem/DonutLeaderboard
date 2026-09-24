package io.nightbeam.donutleaderboard.util;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerAdapter {

    private final Plugin plugin;
    private final ExecutorService asyncExecutor;
    private final boolean folia;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() / 2), new NamedThreadFactory());
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public Executor asyncExecutor() {
        return asyncExecutor;
    }

    public void runAsync(Runnable runnable) {
        if (folia) {
            invokeFoliaAsync(runnable);
            return;
        }
        asyncExecutor.execute(runnable);
    }

    public void runGlobal(Runnable runnable) {
        if (folia) {
            invokeGlobalFolia(runnable);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runEntity(Entity entity, Runnable runnable) {
        if (folia) {
            invokeEntityFolia(entity, runnable);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public CancellableTask runGlobalRepeating(Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (folia) {
            return invokeGlobalRepeatingFolia(runnable, initialDelayTicks, periodTicks);
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, initialDelayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    public void shutdown() {
        asyncExecutor.shutdownNow();
    }

    private void invokeFoliaAsync(Runnable runnable) {
        try {
            Object server = Bukkit.getServer();
            Method getAsyncScheduler = server.getClass().getMethod("getAsyncScheduler");
            Object scheduler = getAsyncScheduler.invoke(server);
            Method runNow = scheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class);
            runNow.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
        } catch (ReflectiveOperationException ex) {
            asyncExecutor.execute(runnable);
        }
    }

    private void invokeGlobalFolia(Runnable runnable) {
        try {
            Method getGlobal = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = getGlobal.invoke(null);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            execute.invoke(scheduler, plugin, runnable);
        } catch (ReflectiveOperationException ex) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private void invokeEntityFolia(Entity entity, Runnable runnable) {
        try {
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(entity);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
            execute.invoke(scheduler, plugin, runnable, null, 1L);
        } catch (ReflectiveOperationException ex) {
            invokeGlobalFolia(runnable);
        }
    }

    private CancellableTask invokeGlobalRepeatingFolia(Runnable runnable, long initialDelayTicks, long periodTicks) {
        try {
            Method getGlobal = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = getGlobal.invoke(null);
            Method runAtFixedRate = scheduler.getClass().getMethod(
                    "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class);
            Object task = runAtFixedRate.invoke(
                    scheduler, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), initialDelayTicks, periodTicks);
            Method cancel = task.getClass().getMethod("cancel");
            return () -> {
                try {
                    cancel.invoke(task);
                } catch (ReflectiveOperationException ignored) {
                }
            };
        } catch (ReflectiveOperationException ex) {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, initialDelayTicks, periodTicks);
            return bukkitTask::cancel;
        }
    }

    private static boolean detectFolia() {
        try {
            String serverClass = Bukkit.getServer().getClass().getName().toLowerCase(Locale.ROOT);
            return serverClass.contains("folia");
        } catch (Throwable ex) {
            return false;
        }
    }

    @FunctionalInterface
    public interface CancellableTask {
        void cancel();
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "DonutLeaderboard-Async-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
