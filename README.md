# Disruptor Spring Boot Starter

A lightweight, production‑ready Spring Boot starter for LMAX Disruptor. It provides
configuration‑driven rings, annotation‑based subscribers, publish templates, and
basic metrics/Actuator integration for observability.

## Modules

- `disruptor-spring-boot-autoconfigure`: Auto‑configuration and core infrastructure
- `disruptor-spring-boot-starter`: Starter dependency

## Requirements

- Java 17
- Spring Boot 3.5.9

## Installation

```xml
<dependency>
  <groupId>com.childrengreens</groupId>
  <artifactId>disruptor-spring-boot-starter</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Configuration

```yaml
spring:
  disruptor:
    enabled: true
    shutdown-timeout: 30s
    shutdown-strategy: DRAIN # DRAIN | HALT
    rings:
      default:
        buffer-size: 65536
        producer-type: MULTI
        wait-strategy: BLOCKING
        threads: 4
        exception-handler: LOG_AND_CONTINUE
      audit:
        buffer-size: 16384
        producer-type: SINGLE
        wait-strategy: YIELDING
        threads: 2
        exception-handler: LOG_AND_HALT
```

## Publish Events

```java
@Autowired
private DisruptorTemplate disruptorTemplate;

disruptorTemplate.publish("default", new OrderCreated(...));
```

If the event class is annotated with `@DisruptorEventType`, that value is used as the
logical event type. Otherwise the class name is used.

## Subscribe to Events

### Method‑based

```java
@DisruptorSubscriber(ring = "default", order = 100)
public void onOrderCreated(OrderCreated event) {
    // handle event
}
```

### Worker mode (competing consumers)

```java
@DisruptorSubscriber(ring = "default", mode = Concurrency.MODE_WORKER)
public void onOrderCreated(OrderCreated event) {
    // handle event
}
```

### Batch processing

```java
@DisruptorSubscriber(ring = "default", batch = true, batchSize = 100)
public void onBatch(List<OrderCreated> events) {
    // handle batch
}
```

### Event type filtering

```java
@DisruptorSubscriber(ring = "default", eventType = "order.created")
public void onOrderCreated(OrderCreated event) {
    // only receives matching eventType
}
```

### Exception policy

```java
@DisruptorSubscriber(ring = "default", exceptionPolicy = ExceptionPolicy.LOG_AND_CONTINUE)
public void onOrderCreated(OrderCreated event) {
    // exceptions are logged and suppressed for this subscriber
}
```

## Metrics & Actuator

When Micrometer/Actuator is present, the starter registers the following:

- `disruptor.ringbuffer.remainingCapacity{ring=...}`
- `disruptor.ringbuffer.cursor{ring=...}`
- `disruptor.ringbuffer.backlog{ring=...}`
- `disruptor.publish.count{ring=...}`
- `disruptor.consume.count{ring=...}`
- `disruptor.event.latency.avg{ring=...}`

Actuator endpoint:

```
GET /actuator/disruptor
```

Returns ring details, backlog, publish/consume counters, average latency, and
per‑handler counts.

## Notes

- Batch mode is supported for handler mode only.
- If you publish before the Disruptor lifecycle starts, an exception is thrown.
- Ring buffer size must be a power of two.

