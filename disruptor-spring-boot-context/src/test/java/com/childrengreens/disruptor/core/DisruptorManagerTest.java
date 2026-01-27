/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;
import com.childrengreens.disruptor.consumer.ExceptionHandlerSupport;
import com.childrengreens.disruptor.consumer.HandlerAdapter;
import com.childrengreens.disruptor.consumer.SubscriberDefinition;
import com.childrengreens.disruptor.consumer.SubscriberRegistry;
import com.childrengreens.disruptor.consumer.WorkerPoolSupport;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.RingProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import com.childrengreens.disruptor.properties.WaitStrategyType;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisruptorManagerTest {
    @Test
    void startCreatesDefaultRingWhenNoConfigurationProvided() {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        assertThat(manager.getRingBuffer("default")).isNull();
        manager.stop(Duration.ofMillis(100), ShutdownStrategy.HALT);
    }

    @Test
    void startAddsRingFromSubscriberDefinitions() {
        DisruptorProperties properties = new DisruptorProperties();
        SubscriberRegistry registry = new SubscriberRegistry();
        registry.register(new SubscriberDefinition(
                new NoopEventHandler(),
                "noopHandler",
                null,
                DisruptorEvent.class,
                "alpha",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE));

        DisruptorManager manager = newManager(properties, registry);
        manager.start();

        assertThat(manager.getRingBuffer("alpha")).isNotNull();
        manager.stop(Duration.ofMillis(100), ShutdownStrategy.HALT);
    }

    @Test
    void startRejectsNonPowerOfTwoBufferSize() {
        DisruptorProperties properties = new DisruptorProperties();
        RingProperties ring = new RingProperties();
        ring.setBufferSize(1000);
        properties.setRings(Map.of("bad", ring));
        DisruptorManager manager = newManager(properties, new SubscriberRegistry());

        assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bufferSize must be a power of two");
    }

    @Test
    void stopIsNoopWhenNotRunning() {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        manager.stop(Duration.ofMillis(10), ShutdownStrategy.DRAIN);
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void resolvesWaitStrategyVariants() throws Exception {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        RingProperties ring = new RingProperties();
        RingProperties.WaitStrategyConfig config = new RingProperties.WaitStrategyConfig();
        ring.setWaitStrategyConfig(config);

        assertThat(invokeWaitStrategy(manager, WaitStrategyType.BLOCKING, ring))
                .isInstanceOf(BlockingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.TIMEOUT_BLOCKING, ring))
                .isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.LITE_BLOCKING, ring))
                .isInstanceOf(LiteBlockingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.LITE_TIMEOUT_BLOCKING, ring))
                .isInstanceOf(LiteTimeoutBlockingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.SLEEPING, ring))
                .isInstanceOf(SleepingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.YIELDING, ring))
                .isInstanceOf(YieldingWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.BUSY_SPIN, ring))
                .isInstanceOf(BusySpinWaitStrategy.class);
        assertThat(invokeWaitStrategy(manager, WaitStrategyType.PHASED_BACKOFF, ring))
                .isInstanceOf(PhasedBackoffWaitStrategy.class);
    }

    @Test
    void resolvesFallbackWaitStrategyVariants() throws Exception {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        RingProperties.WaitStrategyConfig config = new RingProperties.WaitStrategyConfig();

        assertThat(invokeFallbackWaitStrategy(manager, null, config))
                .isInstanceOf(YieldingWaitStrategy.class);
        assertThat(invokeFallbackWaitStrategy(manager, WaitStrategyType.PHASED_BACKOFF, config))
                .isInstanceOf(YieldingWaitStrategy.class);
        assertThat(invokeFallbackWaitStrategy(manager, WaitStrategyType.BLOCKING, config))
                .isInstanceOf(BlockingWaitStrategy.class);
        assertThat(invokeFallbackWaitStrategy(manager, WaitStrategyType.SLEEPING, config))
                .isInstanceOf(SleepingWaitStrategy.class);
    }

    @Test
    void resolvesTimeoutFallbacks() throws Exception {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        long defaultNanos = (long) invokeToTimeout(manager, null, Duration.ofMillis(5));
        assertThat(defaultNanos).isEqualTo(Duration.ofMillis(5).toNanos());

        long zeroNanos = (long) invokeToTimeout(manager, Duration.ZERO, Duration.ofMillis(3));
        assertThat(zeroNanos).isEqualTo(Duration.ofMillis(3).toNanos());

        long negativeNanos = (long) invokeToTimeout(manager, Duration.ofSeconds(-1), Duration.ofMillis(2));
        assertThat(negativeNanos).isEqualTo(Duration.ofMillis(2).toNanos());

        long valueNanos = (long) invokeToTimeout(manager, Duration.ofNanos(7), Duration.ofMillis(1));
        assertThat(valueNanos).isEqualTo(7L);
    }

    private DisruptorManager newManager(
            DisruptorProperties properties, SubscriberRegistry registry) {
        HandlerAdapter handlerAdapter = new HandlerAdapter(new DisruptorMetrics());
        ExceptionHandlerSupport exceptionHandlerSupport = new ExceptionHandlerSupport();
        WorkerPoolSupport workerPoolSupport = new WorkerPoolSupport();
        return new DisruptorManager(
                properties, registry, handlerAdapter, exceptionHandlerSupport, workerPoolSupport);
    }

    static class NoopEventHandler implements EventHandler<DisruptorEvent> {
        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
        }
    }

    private WaitStrategy invokeWaitStrategy(
            DisruptorManager manager, WaitStrategyType type, RingProperties ring) throws Exception {
        java.lang.reflect.Method method = DisruptorManager.class.getDeclaredMethod(
                "toWaitStrategy", WaitStrategyType.class, RingProperties.class);
        method.setAccessible(true);
        return (WaitStrategy) method.invoke(manager, type, ring);
    }

    private WaitStrategy invokeFallbackWaitStrategy(
            DisruptorManager manager,
            WaitStrategyType type,
            RingProperties.WaitStrategyConfig config) throws Exception {
        java.lang.reflect.Method method = DisruptorManager.class.getDeclaredMethod(
                "toFallbackWaitStrategy", WaitStrategyType.class, RingProperties.WaitStrategyConfig.class);
        method.setAccessible(true);
        return (WaitStrategy) method.invoke(manager, type, config);
    }

    private Object invokeToTimeout(DisruptorManager manager, Duration value, Duration defaultValue)
            throws Exception {
        java.lang.reflect.Method method = DisruptorManager.class.getDeclaredMethod(
                "toTimeout", Duration.class, Duration.class);
        method.setAccessible(true);
        return method.invoke(manager, value, defaultValue);
    }
}
