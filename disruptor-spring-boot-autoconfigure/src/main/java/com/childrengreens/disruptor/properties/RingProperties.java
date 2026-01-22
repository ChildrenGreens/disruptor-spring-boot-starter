package com.childrengreens.disruptor.properties;

/**
 * Configuration for a single Disruptor ring.
 */
public class RingProperties {
    private int bufferSize = 1024;
    private ProducerType producerType = ProducerType.MULTI;
    private WaitStrategyType waitStrategy = WaitStrategyType.BLOCKING;
    private ExceptionHandlerType exceptionHandler = ExceptionHandlerType.LOG_AND_CONTINUE;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public ProducerType getProducerType() {
        return producerType;
    }

    public void setProducerType(ProducerType producerType) {
        this.producerType = producerType;
    }

    public WaitStrategyType getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(WaitStrategyType waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    public ExceptionHandlerType getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandlerType exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
}
