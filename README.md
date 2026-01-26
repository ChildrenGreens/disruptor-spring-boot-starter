# Disruptor Spring Boot Starter

[中文文档](README_CN.md)

A lightweight, production-ready Spring Boot starter for [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor). It provides configuration-driven rings, annotation-based subscribers, publish templates, graceful shutdown, and metrics/Actuator integration for observability.

## Features

- **Out-of-the-box**: Just add dependency and configure via `application.yml`
- **Unified abstraction**: Business code only focuses on "publish events / subscribe events"
- **Multi-ring support**: Multiple RingBuffers with different configurations (by topic/name)
- **Flexible strategies**: Configurable WaitStrategy, ProducerType per ring
- **Observability**: Exposes queue depth, publish/consume counts, latency metrics
- **Graceful shutdown**: Drain or halt strategies with configurable timeout

## Modules

```
disruptor-spring-boot-starter
├── disruptor-spring-boot-autoconfigure   # Auto-configuration and core infrastructure
│   ├── annotation                        # @DisruptorSubscriber, @DisruptorEventType, etc.
│   ├── properties                        # DisruptorProperties, RingProperties
│   ├── core                              # DisruptorManager, DisruptorTemplate, DisruptorLifecycle
│   ├── consumer                          # SubscriberBeanPostProcessor, HandlerAdapter, etc.
│   ├── autoconfigure                     # DisruptorAutoConfiguration
│   └── metrics                           # DisruptorMeterBinder, DisruptorEndpoint
└── disruptor-spring-boot-starter         # Starter dependency (POM)
```

## Requirements

- Java 17+
- Spring Boot 3.x

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
    shutdown-strategy: DRAIN  # DRAIN | HALT
    rings:
      default:
        buffer-size: 65536          # Must be power of 2
        producer-type: MULTI        # SINGLE | MULTI
        wait-strategy: BLOCKING
        exception-handler: LOG_AND_CONTINUE
      audit:
        buffer-size: 16384
        producer-type: SINGLE
        wait-strategy: YIELDING
        exception-handler: LOG_AND_HALT
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `enabled` | Enable/disable the starter | `true` |
| `shutdown-timeout` | Graceful shutdown timeout | `30s` |
| `shutdown-strategy` | `DRAIN` (consume remaining) or `HALT` (stop immediately) | `DRAIN` |
| `rings.<name>.buffer-size` | Ring buffer size (must be power of 2) | `1024` |
| `rings.<name>.producer-type` | `SINGLE` or `MULTI` producer | `MULTI` |
| `rings.<name>.wait-strategy` | Consumer wait strategy | `BLOCKING` |
| `rings.<name>.exception-handler` | `LOG_AND_CONTINUE` or `LOG_AND_HALT` | `LOG_AND_CONTINUE` |
| `rings.<name>.wait-strategy-config.timeout-blocking-timeout` | TimeoutBlockingWaitStrategy timeout | `1ms` |
| `rings.<name>.wait-strategy-config.lite-timeout-blocking-timeout` | LiteTimeoutBlockingWaitStrategy timeout | `1ms` |
| `rings.<name>.wait-strategy-config.phased-backoff-spin-timeout` | PhasedBackoff spin timeout | `1us` |
| `rings.<name>.wait-strategy-config.phased-backoff-yield-timeout` | PhasedBackoff yield timeout | `1000us` |
| `rings.<name>.wait-strategy-config.phased-backoff-fallback` | PhasedBackoff fallback strategy | `YIELDING` |

Note: rings are created only when configured under `spring.disruptor.rings` or discovered
via `@DisruptorSubscriber`. There is no implicit default ring.

### Wait Strategy Options

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `BLOCKING` | Lock-based, lowest CPU | Low latency not critical |
| `TIMEOUT_BLOCKING` | Blocking with timeout | Need periodic wake-up |
| `LITE_BLOCKING` | Lightweight blocking | Moderate throughput |
| `LITE_TIMEOUT_BLOCKING` | Lite blocking with timeout | Moderate with timeout |
| `SLEEPING` | Spin then yield then sleep | Balanced CPU/latency |
| `YIELDING` | Spin then yield | Low latency, more CPU |
| `BUSY_SPIN` | Continuous spinning | Lowest latency, dedicated CPU |
| `PHASED_BACKOFF` | Progressive backoff | Adaptive latency |

### Wait Strategy Config (Advanced)

```yaml
spring:
  disruptor:
    rings:
      default:
        wait-strategy: PHASED_BACKOFF
        wait-strategy-config:
          timeout-blocking-timeout: 1ms
          lite-timeout-blocking-timeout: 1ms
          phased-backoff-spin-timeout: 1us
          phased-backoff-yield-timeout: 1000us
          phased-backoff-fallback: YIELDING
```

## Usage

### Publish Events

```java
@Autowired
private DisruptorTemplate disruptorTemplate;

// Publish to specific ring
disruptorTemplate.publish("default", new OrderCreated(...));

// Publish to default ring
disruptorTemplate.publish("default", event);
```

### Event Type Annotation (Optional)

```java
@DisruptorEventType("order.created")
public class OrderCreated {
    // ...
}
```

If the event class is annotated with `@DisruptorEventType`, that value is used as the logical event type. Otherwise the fully qualified class name is used.

### Subscribe to Events

#### Method-based (Recommended)

```java
@Component
public class OrderEventHandler {

    @DisruptorSubscriber(ring = "default", order = 100)
    public void onOrderCreated(OrderCreated event) {
        // handle event (broadcast mode by default)
    }
}
```

#### Worker Mode (Competing Consumers)

```java
@DisruptorSubscriber(ring = "default", mode = Concurrency.MODE_WORKER)
public void onOrderCreated(OrderCreated event) {
    // Multiple workers compete to consume events (load balancing)
}
```

#### Batch Processing

```java
@DisruptorSubscriber(ring = "default", batch = true, batchSize = 100)
public void onBatch(List<OrderCreated> events) {
    // Process events in batches (handler mode only)
}
```

#### Event Type Filtering

```java
@DisruptorSubscriber(ring = "default", eventType = "order.created")
public void onOrderCreated(OrderCreated event) {
    // Only receives events matching the specified eventType
}
```

#### Per-subscriber Exception Policy

```java
@DisruptorSubscriber(ring = "default", exceptionPolicy = ExceptionPolicy.LOG_AND_CONTINUE)
public void onOrderCreated(OrderCreated event) {
    // Exceptions are logged and suppressed for this subscriber
}
```

### @DisruptorSubscriber Attributes

| Attribute | Description | Default |
|-----------|-------------|---------|
| `ring` | Target ring name | `"default"` |
| `mode` | `MODE_HANDLER` (broadcast) or `MODE_WORKER` (competing) | `MODE_HANDLER` |
| `order` | Execution order (lower = earlier) | `0` |
| `batch` | Enable batch delivery | `false` |
| `batchSize` | Max batch size (0 = flush on endOfBatch) | `0` |
| `eventType` | Filter by event type | `""` (no filter) |
| `exceptionPolicy` | `DELEGATE`, `LOG_AND_CONTINUE`, or `THROW` | `DELEGATE` |

### Concurrency Models

| Mode | Description |
|------|-------------|
| `MODE_HANDLER` | Each handler sees all events (broadcast). Multiple handlers with same order run in parallel. |
| `MODE_WORKER` | Multiple workers compete to consume events (single-consumer load balancing). |

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    DisruptorAutoConfiguration                    │
│  (Creates all beans when spring.disruptor.enabled=true)         │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│DisruptorManager│    │DisruptorTemplate│     │SubscriberBean   │
│               │     │                 │     │PostProcessor    │
│ - builds rings│     │ - publish API   │     │                 │
│ - starts/stops│     │ - converts event│     │ - scans @Disrup-│
│ - holds buffers│    │ - records metrics│    │   torSubscriber │
└───────────────┘     └─────────────────┘     │ - registers to  │
        │                       │             │   registry      │
        │                       │             └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  RingBuffer   │◄────│  EventPublisher │     │SubscriberRegistry│
│  (per ring)   │     │                 │     │                 │
└───────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        │                                               ▼
        │                                     ┌─────────────────┐
        │                                     │  HandlerAdapter │
        │                                     │                 │
        │                                     │ - adapts methods│
        │                                     │   to EventHandler│
        │                                     │   or WorkHandler│
        │                                     └─────────────────┘
        │                                               │
        └───────────────────────────────────────────────┘
                    (handlers consume from ring)
```

### Lifecycle

1. **Startup**: `DisruptorLifecycle` (SmartLifecycle) starts all rings after Spring context is ready
2. **Running**: `DisruptorTemplate.publish()` pushes events; handlers consume from RingBuffer
3. **Shutdown**: Configurable strategy (DRAIN or HALT) with timeout

## Metrics & Actuator

When Micrometer/Actuator is present, the starter automatically registers:

### Micrometer Metrics

| Metric | Description |
|--------|-------------|
| `disruptor.ringbuffer.remainingCapacity{ring=...}` | Available slots in ring buffer |
| `disruptor.ringbuffer.cursor{ring=...}` | Current cursor position |
| `disruptor.ringbuffer.backlog{ring=...}` | Unconsumed event count |
| `disruptor.publish.count{ring=...}` | Total published events |
| `disruptor.consume.count{ring=...}` | Total consumed events |
| `disruptor.event.latency.avg{ring=...}` | Average event latency |

### Actuator Endpoint

```
GET /actuator/disruptor
```

Returns ring details including buffer size, cursor, backlog, publish/consume counters, average latency, and per-handler statistics.

## Notes

- Batch mode is supported for handler mode only (not worker mode)
- Publishing before Disruptor lifecycle starts throws `IllegalStateException`
- Ring buffer size must be a power of two (validated at startup)
- Disruptor is an in-process high-performance queue, not a replacement for message brokers

## License

Apache License 2.0
