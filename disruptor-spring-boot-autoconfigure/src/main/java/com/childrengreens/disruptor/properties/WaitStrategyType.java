package com.childrengreens.disruptor.properties;

/**
 * Wait strategies for Disruptor consumers.
 */
public enum WaitStrategyType {
    BLOCKING,
    SLEEPING,
    YIELDING,
    BUSY_SPIN,
    PHASED_BACKOFF
}
