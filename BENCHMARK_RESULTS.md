# Benchmark Results

## Environment
- Machine: HP Victus 16, 16GB RAM
- JVM: Eclipse Temurin 25 (Docker)
- Redis: 7-alpine (Docker)
- Spring Boot: 4.0.5
- Tomcat threads: 400 (max)
- Redis pool: 20 (max-active connections)
- Test tool: Apache JMeter 5.6.3

## Test Configuration
- Concurrent users: 500
- Ramp-up: 10 seconds
- Loops per user: 20
- Total requests per test: 10,000

---

## Results

### Test 1 — Baseline
| Metric              | Value         |
|---------------------|---------------|
| Throughput          | 422.5 req/sec |
| Avg response time   | 468ms         |
| 99th percentile     | 935ms         |
| Error rate          | 0.00%         |

### Test 2 — Fixed Window
| Metric              | Value         |
|---------------------|---------------|
| Throughput          | 375.5 req/sec |
| Avg response time   | 832ms         |
| 99th percentile     | 1216ms        |
| Error rate (429s)   | 40%           |
| Redis overhead      | 364ms         |

### Test 3 — Token Bucket
| Metric              | Value         |
|---------------------|---------------|
| Throughput          | 375.7 req/sec |
| Avg response time   | 838ms         |
| 99th percentile     | 1266ms        |
| Error rate (429s)   | 59.56%        |
| Redis overhead      | 370ms         |

### Test 4 — Sliding Window
| Metric              | Value         |
|---------------------|---------------|
| Throughput          | 375.5 req/sec |
| Avg response time   | 915ms         |
| 99th percentile     | 1321ms        |
| Error rate (429s)   | 39.88%        |
| Redis overhead      | 447ms         |

### Test 5 — Concurrent same key
| Metric              | Value                               |
|---------------------|-------------------------------------|
| Throughput          | 425.2 req/sec                       |
| Total requests sent | 5,000                               |


---