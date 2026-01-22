package com.childrengreens.disruptor.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisruptorEventFactoryTest {
    @Test
    void createsNewEventInstances() {
        DisruptorEventFactory factory = new DisruptorEventFactory();
        DisruptorEvent first = factory.newInstance();
        DisruptorEvent second = factory.newInstance();
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second).isNotSameAs(first);
    }
}
