package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.properties.DisruptorProperties;
import org.springframework.context.SmartLifecycle;

/**
 * Spring lifecycle bridge to start and stop Disruptor rings with the context.
 */
public class DisruptorLifecycle implements SmartLifecycle {
    private final DisruptorManager manager;
    private final DisruptorProperties properties;
    private boolean running = false;

    public DisruptorLifecycle(DisruptorManager manager, DisruptorProperties properties) {
        this.manager = manager;
        this.properties = properties;
    }

    @Override
    public void start() {
        manager.start();
        running = true;
    }

    @Override
    public void stop() {
        manager.stop(properties.getShutdownTimeout(), properties.getShutdownStrategy());
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
