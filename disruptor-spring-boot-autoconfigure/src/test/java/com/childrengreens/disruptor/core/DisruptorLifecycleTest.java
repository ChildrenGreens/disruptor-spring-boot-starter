package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DisruptorLifecycleTest {
    @Test
    void delegatesStartAndStopToManager() {
        DisruptorManager manager = mock(DisruptorManager.class);
        DisruptorProperties properties = new DisruptorProperties();
        properties.setShutdownTimeout(Duration.ofSeconds(5));
        properties.setShutdownStrategy(ShutdownStrategy.HALT);

        DisruptorLifecycle lifecycle = new DisruptorLifecycle(manager, properties);
        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
        verify(manager).start();
        verify(manager).stop(Duration.ofSeconds(5), ShutdownStrategy.HALT);
    }

    @Test
    void alwaysAutoStartsWithMaxPhase() {
        DisruptorLifecycle lifecycle =
                new DisruptorLifecycle(mock(DisruptorManager.class), new DisruptorProperties());
        assertThat(lifecycle.isAutoStartup()).isTrue();
        assertThat(lifecycle.getPhase()).isEqualTo(Integer.MAX_VALUE);
    }
}
