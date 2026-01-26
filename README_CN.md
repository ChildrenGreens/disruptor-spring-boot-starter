# Disruptor Spring Boot Starter

一个轻量级、生产就绪的 [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor) Spring Boot Starter。提供配置驱动的 Ring 管理、基于注解的订阅者、发布模板、优雅停机以及监控/Actuator 集成。

## 特性

- **开箱即用**：引入依赖，配置 `application.yml` 即可运行
- **统一抽象**：业务代码只需关注"发布事件/订阅事件"
- **多 Ring 支持**：支持多个 RingBuffer，可按 topic/name 区分配置
- **灵活策略**：每个 Ring 可独立配置 WaitStrategy、ProducerType
- **可观测性**：暴露队列深度、发布/消费计数、延迟指标
- **优雅停机**：支持 DRAIN（消费完）或 HALT（立即停止）策略，可配置超时时间

## 模块结构

```
disruptor-spring-boot-starter
├── disruptor-spring-boot-autoconfigure   # 自动配置和核心基础设施
│   ├── annotation                        # @DisruptorSubscriber, @DisruptorEventType 等注解
│   ├── properties                        # DisruptorProperties, RingProperties 配置类
│   ├── core                              # DisruptorManager, DisruptorTemplate, DisruptorLifecycle
│   ├── consumer                          # SubscriberBeanPostProcessor, HandlerAdapter 等
│   ├── autoconfigure                     # DisruptorAutoConfiguration 自动配置
│   └── metrics                           # DisruptorMeterBinder, DisruptorEndpoint 监控
└── disruptor-spring-boot-starter         # Starter 依赖（POM）
```

## 环境要求

- Java 17+
- Spring Boot 3.x

## 安装

```xml
<dependency>
  <groupId>com.childrengreens</groupId>
  <artifactId>disruptor-spring-boot-starter</artifactId>
  <version>${project.version}</version>
</dependency>
```

## 配置

```yaml
spring:
  disruptor:
    enabled: true
    shutdown-timeout: 30s
    shutdown-strategy: DRAIN  # DRAIN | HALT
    rings:
      default:
        buffer-size: 65536          # 必须是 2 的幂次
        producer-type: MULTI        # SINGLE | MULTI
        wait-strategy: BLOCKING
        exception-handler: LOG_AND_CONTINUE
      audit:
        buffer-size: 16384
        producer-type: SINGLE
        wait-strategy: YIELDING
        exception-handler: LOG_AND_HALT
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `enabled` | 启用/禁用 Starter | `true` |
| `shutdown-timeout` | 优雅停机超时时间 | `30s` |
| `shutdown-strategy` | `DRAIN`（消费完剩余事件）或 `HALT`（立即停止） | `DRAIN` |
| `rings.<name>.buffer-size` | Ring Buffer 大小（必须是 2 的幂次） | `1024` |
| `rings.<name>.producer-type` | `SINGLE`（单生产者）或 `MULTI`（多生产者） | `MULTI` |
| `rings.<name>.wait-strategy` | 消费者等待策略 | `BLOCKING` |
| `rings.<name>.exception-handler` | `LOG_AND_CONTINUE` 或 `LOG_AND_HALT` | `LOG_AND_CONTINUE` |
| `rings.<name>.wait-strategy-config.timeout-blocking-timeout` | TimeoutBlockingWaitStrategy 超时 | `1ms` |
| `rings.<name>.wait-strategy-config.lite-timeout-blocking-timeout` | LiteTimeoutBlockingWaitStrategy 超时 | `1ms` |
| `rings.<name>.wait-strategy-config.phased-backoff-spin-timeout` | PhasedBackoff 自旋超时 | `1us` |
| `rings.<name>.wait-strategy-config.phased-backoff-yield-timeout` | PhasedBackoff yield 超时 | `1000us` |
| `rings.<name>.wait-strategy-config.phased-backoff-fallback` | PhasedBackoff 回退策略 | `YIELDING` |

注意：只有在 `spring.disruptor.rings` 中显式配置，或通过 `@DisruptorSubscriber` 动态发现时才会创建 ring，不再隐式创建默认 ring。

### 等待策略选择

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `BLOCKING` | 基于锁，CPU 占用最低 | 对延迟要求不高 |
| `TIMEOUT_BLOCKING` | 带超时的阻塞 | 需要定期唤醒 |
| `LITE_BLOCKING` | 轻量级阻塞 | 中等吞吐量 |
| `LITE_TIMEOUT_BLOCKING` | 带超时的轻量级阻塞 | 中等吞吐量 + 超时需求 |
| `SLEEPING` | 自旋 → yield → sleep | CPU/延迟平衡 |
| `YIELDING` | 自旋 → yield | 低延迟，CPU 占用较高 |
| `BUSY_SPIN` | 持续自旋 | 最低延迟，需独占 CPU |
| `PHASED_BACKOFF` | 渐进式退避 | 自适应延迟 |

### 等待策略高级配置

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

## 使用方式

### 发布事件

```java
@Autowired
private DisruptorTemplate disruptorTemplate;

// 发布到指定 ring
disruptorTemplate.publish("default", new OrderCreated(...));

// 发布到默认 ring
disruptorTemplate.publish("default", event);
```

### 事件类型注解（可选）

```java
@DisruptorEventType("order.created")
public class OrderCreated {
    // ...
}
```

如果事件类标注了 `@DisruptorEventType`，该值会作为逻辑事件类型。否则使用类的全限定名。

### 订阅事件

#### 方法注解方式（推荐）

```java
@Component
public class OrderEventHandler {

    @DisruptorSubscriber(ring = "default", order = 100)
    public void onOrderCreated(OrderCreated event) {
        // 处理事件（默认广播模式）
    }
}
```

#### Worker 模式（竞争消费）

```java
@DisruptorSubscriber(ring = "default", mode = Concurrency.MODE_WORKER)
public void onOrderCreated(OrderCreated event) {
    // 多个 Worker 竞争消费事件（负载均衡）
}
```

#### 批量处理

```java
@DisruptorSubscriber(ring = "default", batch = true, batchSize = 100)
public void onBatch(List<OrderCreated> events) {
    // 批量处理事件（仅 handler 模式支持）
}
```

#### 事件类型过滤

```java
@DisruptorSubscriber(ring = "default", eventType = "order.created")
public void onOrderCreated(OrderCreated event) {
    // 只接收匹配指定 eventType 的事件
}
```

#### 订阅者级异常策略

```java
@DisruptorSubscriber(ring = "default", exceptionPolicy = ExceptionPolicy.LOG_AND_CONTINUE)
public void onOrderCreated(OrderCreated event) {
    // 该订阅者的异常会被记录并忽略
}
```

### @DisruptorSubscriber 注解属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `ring` | 目标 Ring 名称 | `"default"` |
| `mode` | `MODE_HANDLER`（广播）或 `MODE_WORKER`（竞争） | `MODE_HANDLER` |
| `order` | 执行顺序（值越小越先执行） | `0` |
| `batch` | 启用批量投递 | `false` |
| `batchSize` | 最大批量大小（0 表示在 endOfBatch 时刷新） | `0` |
| `eventType` | 按事件类型过滤 | `""`（不过滤） |
| `exceptionPolicy` | `DELEGATE`、`LOG_AND_CONTINUE` 或 `THROW` | `DELEGATE` |

### 并发模型

| 模式 | 说明 |
|------|------|
| `MODE_HANDLER` | 每个 handler 都能看到所有事件（广播）。相同 order 的多个 handler 并行执行。 |
| `MODE_WORKER` | 多个 worker 竞争消费事件（单消费者负载均衡）。 |

## 架构设计

### 核心组件

```
┌─────────────────────────────────────────────────────────────────┐
│                    DisruptorAutoConfiguration                    │
│         （当 spring.disruptor.enabled=true 时创建所有 Bean）       │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│DisruptorManager│     │DisruptorTemplate│     │SubscriberBean   │
│               │     │                 │     │PostProcessor    │
│ - 构建 rings  │     │ - 发布 API     │     │                 │
│ - 启动/停止   │     │ - 转换事件     │     │ - 扫描 @Disrup- │
│ - 持有 buffers│     │ - 记录指标     │     │   torSubscriber │
└───────────────┘     └─────────────────┘     │ - 注册到 registry│
        │                       │             └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  RingBuffer   │◄────│  EventPublisher │     │SubscriberRegistry│
│  (每个 ring)  │     │                 │     │                 │
└───────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        │                                               ▼
        │                                     ┌─────────────────┐
        │                                     │  HandlerAdapter │
        │                                     │                 │
        │                                     │ - 将方法适配为  │
        │                                     │   EventHandler  │
        │                                     │   或 WorkHandler│
        │                                     └─────────────────┘
        │                                               │
        └───────────────────────────────────────────────┘
                      （handler 从 ring 消费事件）
```

### 生命周期

1. **启动**：`DisruptorLifecycle`（SmartLifecycle）在 Spring 上下文就绪后启动所有 rings
2. **运行**：`DisruptorTemplate.publish()` 推送事件；handler 从 RingBuffer 消费
3. **停机**：可配置策略（DRAIN 或 HALT）+ 超时时间

## 监控与指标

当 Micrometer/Actuator 存在时，Starter 会自动注册以下内容：

### Micrometer 指标

| 指标 | 说明 |
|------|------|
| `disruptor.ringbuffer.remainingCapacity{ring=...}` | Ring Buffer 剩余容量 |
| `disruptor.ringbuffer.cursor{ring=...}` | 当前 cursor 位置 |
| `disruptor.ringbuffer.backlog{ring=...}` | 未消费事件数量 |
| `disruptor.publish.count{ring=...}` | 发布事件总数 |
| `disruptor.consume.count{ring=...}` | 消费事件总数 |
| `disruptor.event.latency.avg{ring=...}` | 平均事件延迟 |

### Actuator 端点

```
GET /actuator/disruptor
```

返回 Ring 详情，包括 buffer 大小、cursor、积压量、发布/消费计数、平均延迟以及每个 handler 的统计信息。

## 注意事项

- 批量模式仅支持 handler 模式（不支持 worker 模式）
- 在 Disruptor 生命周期启动前发布事件会抛出 `IllegalStateException`
- Ring Buffer 大小必须是 2 的幂次（启动时校验）
- Disruptor 是进程内高性能队列，不是消息中间件的替代品

## 许可证

Apache License 2.0
