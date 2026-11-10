# shorturl · 短链接服务 + 分布式限流组件

基于 Spring Boot 的短链接服务，附带课程设计用的限流组件：四种单机限流算法 + Redis/Lua 集群限流 + AOP 注解零侵入接入。所有压测数字均为本机实测。

## 功能特性

**短链接服务**

- 长链转短链：62 进制字符集随机 6 位短码，先查后插、最多重试 100 次，数据库唯一索引作为最终兜底
- 选用 302 而非 301：保留点击统计能力与后续更换目标地址的灵活性（301 会被浏览器缓存）
- 参数校验：仅接受 `http://` / `https://` 开头的长链
- Redis 缓存热点短码：TTL 1 小时；对不存在的短码缓存空值 60 秒，防止缓存穿透
- 缓存降级：Redis 操作包 try-catch，配合 200ms 连接/读超时快速失败；Redis 故障时回源 MySQL，接口仍返回 302

**限流组件**（`com.demo.ratelimit`）

- 四种算法：固定窗口 / 滑动窗口 / 令牌桶 / 漏桶，统一实现 `RateLimiter.tryAcquire()`
- 集群限流：Redis ZSET + Lua，「清理过期 → ZCARD → 判断 → ZADD → PEXPIRE」原子执行，多实例共享计数
- 零侵入接入：`@RateLimit(key, limit, algorithm)` + Spring AOP 前置切面，超限由 `@RestControllerAdvice` 统一返回 429
- 写限读不限：仅保护 `/api/shorten` 创建接口；跳转接口不限流
- 故障开放（fail-open）：Redis 异常时放行，保证主业务可用

## 技术栈

Spring Boot 4.1.1 / Spring Data JPA / Spring AOP / MySQL 9.7 / Redis 8.10 / Python 3（压测脚本，仅用标准库）

## 快速开始

### 1. MySQL

建库建表（按实际实体字段调整）：

```sql
CREATE DATABASE IF NOT EXISTS shorturl DEFAULT CHARSET utf8mb4;
CREATE TABLE url_map (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_code VARCHAR(16) NOT NULL UNIQUE,
  long_url VARCHAR(2048) NOT NULL,
  create_time DATETIME
);
```

### 2. Redis

Windows 可用 `winget install --id taizod1024.redis-windows-fork`，启动后 `redis-cli ping` 应返回 `PONG`。

### 3. 配置

`src/main/resources/application.properties`（示例值，请替换为你自己的账号密码，**不要把真实密码提交到公开仓库**）：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/shorturl
spring.datasource.username=your-user
spring.datasource.password=your-password
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.connect-timeout=200ms
spring.data.redis.timeout=200ms
```

### 4. 启动

```bash
mvn clean spring-boot:run
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/shorten?longUrl=xxx` | 返回 6 位短码；受 `@RateLimit(key="shorten", limit=20, algorithm="redis")` 保护，超限返回 429 |
| GET | `/{code}` | 302 跳转到长链 |
| GET | `/api/expand?code=xxx` | 查询长链 |
| GET | `/api/demo/sliding` | 限流演示（limit=5，滑动窗口） |
| GET | `/api/demo/fixed` | 限流演示（limit=5，固定窗口） |

## 压测数据

### 短链接服务（50 并发 / 2000 请求，单热点短码）

| 指标 | 仅 MySQL | 连接池 10→50 | 引入 Redis 缓存 |
|---|---|---|---|
| QPS | 3127 | 2957（无显著提升） | **3529（+12.9%）** |
| 平均耗时 | 11.5 ms | 11.7 ms | 9.3 ms |
| P50 | 9.3 ms | 9.1 ms | 6.7 ms |
| P95 | 27.3 ms | 27.7 ms | 24.2 ms |
| P99 | 41.7 ms | 46.2 ms | 38.7 ms |
| Redis 宕机 | — | — | 自动回源 DB，仍返回 302 |

调大连接池后 QPS 无提升，据此排除连接池瓶颈；单热点短码场景下缓存命中率接近 100%，而本地 MySQL 主键查询本就很快，故 Redis 收益仅 12.9%——真实收益在数据库成为瓶颈时才显现。

### 限流组件（50 并发 / 200 请求）

| 指标 | 实测值 |
|---|---|
| 压测耗时 | 0.18 s |
| 放行（200） / 限流（429） | 20 / 180 |
| 拒绝率 | **90.0%** |
| 整体吞吐 | 1134 req/s |
| 异常 | 0 |

阈值为 20 次/秒滑动窗口，0.18s 内窗口未滑动，故放行量严格等于 20。串行验证中可观察到「放行 20 → 429 → 又逐步放行」，为窗口滚动、配额渐进释放所致；拒绝率必须与压测耗时一并引用。

### 四种算法对比（limit = 10 次/秒）

| 实验 | 结果 |
|---|---|
| 瞬时突发 50 次 | 四种算法均只放行 10 次 |
| 窗口边界（t=950ms 打 10 次，t=1050ms 再打 10 次） | 固定窗口合计 **20（2 倍突刺）**，滑动窗口 10 |
| 令牌桶 0.3s 后 | 仅放行 1 个（匀速回填），漏桶恒定速率 |

运行 `java -cp <out> com.demo.ratelimit.LimiterDemo` 可复现前三组数据。

## 脚本

```bash
python bench.py         # 短链接服务压测（50 并发 / 2000 请求）
python bench_limit.py   # 限流组件压测（50 并发 / 200 请求）
```

两个脚本均使用 Python 标准库并显式禁用系统代理；`bench_limit.py` 会写入短链记录，跑完可清理：

```sql
DELETE FROM url_map WHERE long_url='https://www.baidu.com';
```

## 目录结构

```
src/main/java/com/demo/
├── shorturl/     短链接服务：Controller、实体、Repository、启动类
└── ratelimit/    限流组件
    ├── RateLimiter.java                 统一接口
    ├── FixedWindowLimiter.java          固定窗口
    ├── SlidingWindowLimiter.java        滑动窗口
    ├── TokenBucketLimiter.java          令牌桶
    ├── LeakyBucketLimiter.java          漏桶
    ├── RedisSlidingWindowLimiter.java   Redis ZSET + Lua 集群版
    ├── RateLimit.java                   自定义注解
    ├── RateLimitAspect.java             前置切面
    ├── RateLimitException.java          超限异常
    ├── RateLimitExceptionHandler.java   统一返回 429
    ├── LimiterDemo.java                 四算法对比 Demo
    └── DemoController.java              限流演示接口
bench.py / bench_limit.py               压测脚本
```

## 设计说明

- **不用自增 ID 转 62 进制**：短码连续可被遍历爬取，且会泄露业务数据量；随机 6 位有 62^6 ≈ 568 亿种可能。
- **降级必须配快速失败**：只加 try-catch 不设超时时，Redis 宕机会让每个请求干等数十秒并拖垮线程池，比不用缓存更糟。
- **fail-open / fail-close 取决于业务代价**：创建短链多几条不构成资损，故放行；扣库存、支付、防刷应改为故障时拒绝。
