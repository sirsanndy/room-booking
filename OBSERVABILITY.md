# Observability Guide - Meeting Room Booking Application

## Overview

This application is fully instrumented with **Prometheus** and **Grafana** for comprehensive observability. Monitor every aspect of your application including:

- ✅ HTTP Request metrics (rate, latency, errors)
- ✅ JVM metrics (memory, GC, threads)
- ✅ Database connection pool metrics
- ✅ PostgreSQL database metrics
- ✅ Redis cache metrics
- ✅ Custom business metrics (bookings, rate limiting, conflicts)
- ✅ System metrics (CPU, memory, disk)

## Architecture

```
┌─────────────────┐
│  Spring Boot    │
│   Application   │──────┐
│  (Port 8080)    │      │
└─────────────────┘      │
                         │ /actuator/prometheus
┌─────────────────┐      │
│   PostgreSQL    │      │
│  (Port 5432)    │      │
└─────────────────┘      │
         │               │
         │               │
┌─────────────────┐      │
│ Postgres        │      │
│ Exporter        │──────┤
│ (Port 9187)     │      │
└─────────────────┘      │
                         │
┌─────────────────┐      │
│     Redis       │      │
│  (Port 6379)    │      │
└─────────────────┘      │
         │               │
         │               │
┌─────────────────┐      │
│ Redis Exporter  │      │
│ (Port 9121)     │──────┤
└─────────────────┘      │
                         │
                         ▼
              ┌─────────────────┐
              │   Prometheus    │
              │   (Port 9090)   │
              └─────────────────┘
                         │
                         │
                         ▼
              ┌─────────────────┐
              │    Grafana      │
              │   (Port 3001)   │
              └─────────────────┘
```

## Quick Start

### 1. Start Infrastructure

Start all services including Prometheus and Grafana:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Prometheus (port 9090)
- Grafana (port 3001)
- PostgreSQL Exporter (port 9187)
- Redis Exporter (port 9121)

### 2. Start Spring Boot Application

```bash
cd backend
./mvnw spring-boot:run
```

The application exposes Prometheus metrics at: `http://localhost:8080/actuator/prometheus`

### 3. Access Dashboards

**Prometheus UI:**
- URL: http://localhost:9090
- Explore metrics and create queries

**Grafana UI:**
- URL: http://localhost:3001
- Username: `admin`
- Password: `admin`
- Pre-configured dashboard: "Meeting Room Booking - Application Dashboard"

## Exposed Metrics Endpoints

### Spring Boot Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics

# Prometheus format
curl http://localhost:8080/actuator/prometheus

# Environment info
curl http://localhost:8080/actuator/env

# Cache statistics
curl http://localhost:8080/actuator/caches
```

## Key Metrics Categories

### 1. HTTP Request Metrics

**Request Rate:**
```promql
rate(http_server_requests_seconds_count{application="meeting-room-booking"}[1m])
```

**Request Latency (p95, p99):**
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))
```

**Error Rate:**
```promql
rate(http_server_requests_seconds_count{status=~"4..|5.."}[1m])
```

### 2. JVM Metrics

**Heap Memory Usage:**
```promql
jvm_memory_used_bytes{area="heap"}
jvm_memory_max_bytes{area="heap"}
```

**Garbage Collection:**
```promql
rate(jvm_gc_pause_seconds_count[1m])
rate(jvm_gc_pause_seconds_sum[1m])
```

**Thread Count:**
```promql
jvm_threads_live_threads
jvm_threads_daemon_threads
```

### 3. Database Connection Pool (HikariCP)

**Connection Pool Usage:**
```promql
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_pending
hikaricp_connections_max
```

**Connection Acquisition Time:**
```promql
hikaricp_connections_acquire_seconds
```

### 4. PostgreSQL Metrics

**Database Connections:**
```promql
pg_stat_database_numbackends{datname="meetingroom_db"}
```

**Transactions:**
```promql
rate(pg_stat_database_xact_commit{datname="meetingroom_db"}[1m])
rate(pg_stat_database_xact_rollback{datname="meetingroom_db"}[1m])
```

**Cache Hit Ratio:**
```promql
pg_stat_database_blks_hit{datname="meetingroom_db"} / 
(pg_stat_database_blks_hit{datname="meetingroom_db"} + pg_stat_database_blks_read{datname="meetingroom_db"})
```

**Table Size:**
```promql
pg_table_size_bytes{datname="meetingroom_db"}
```

### 5. Redis Metrics

**Memory Usage:**
```promql
redis_memory_used_bytes
redis_memory_max_bytes
```

**Command Rate:**
```promql
rate(redis_commands_processed_total[1m])
```

**Connected Clients:**
```promql
redis_connected_clients
```

**Cache Hit Rate:**
```promql
rate(redis_keyspace_hits_total[1m]) / 
(rate(redis_keyspace_hits_total[1m]) + rate(redis_keyspace_misses_total[1m]))
```

### 6. Custom Business Metrics

**Bookings Created:**
```promql
rate(booking_created_total[1m])
```

**Bookings Cancelled:**
```promql
rate(booking_cancelled_total[1m])
```

**Booking Conflicts:**
```promql
rate(booking_conflict_total[1m])
```

**Rate Limit Exceeded:**
```promql
rate(rate_limit_exceeded_total[1m])
```

**Database Lock Timeouts:**
```promql
rate(database_lock_timeout_total[1m])
```

### 7. System Metrics

**CPU Usage:**
```promql
system_cpu_usage
process_cpu_usage
```

**System Memory:**
```promql
system_memory_used_bytes
system_memory_max_bytes
```

## Grafana Dashboard Panels

The pre-configured dashboard includes:

### Performance Panels
1. **HTTP Request Rate** - Requests per second by endpoint
2. **HTTP Request Latency** - p95 and p99 latency by endpoint
3. **HTTP Errors** - 4xx and 5xx error rate

### Resource Utilization
4. **JVM Heap Memory Available %** - Gauge showing available heap
5. **System CPU Usage %** - Overall CPU utilization
6. **Database Connection Pool Usage %** - Active connections vs max
7. **JVM Memory Usage** - Detailed heap and non-heap memory

### Database Metrics
8. **Database Connection Pool** - Active, idle, pending connections
9. **PostgreSQL I/O** - Disk reads vs cache hits
10. **PostgreSQL Connections & Transactions** - Commits, rollbacks, connections

### Cache Metrics
11. **Redis Memory Usage** - Memory consumption over time
12. **Redis Activity** - Commands/sec and connected clients

### Business Metrics
13. **Booking Endpoints Rate Limit Monitoring** - Rate limit tracking

## Alerts Configuration

### Recommended Alerts

Add these alerts to Prometheus (`alerting_rules.yml`):

```yaml
groups:
  - name: application_alerts
    interval: 30s
    rules:
      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High 5xx error rate"
          description: "Error rate is {{ $value }} req/sec"

      # High Latency
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High request latency"
          description: "P95 latency is {{ $value }}s"

      # Memory Usage
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap memory usage"
          description: "Heap usage is {{ $value | humanizePercentage }}"

      # Database Connection Pool
      - alert: HighConnectionPoolUsage
        expr: (hikaricp_connections_active / hikaricp_connections_max) > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High database connection pool usage"
          description: "Pool usage is {{ $value | humanizePercentage }}"

      # Rate Limiting
      - alert: HighRateLimitRejects
        expr: rate(rate_limit_exceeded_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit rejection rate"
          description: "Rate limit rejections: {{ $value }} req/sec"

      # Database Locks
      - alert: DatabaseLockTimeouts
        expr: rate(database_lock_timeout_total[5m]) > 1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database lock timeouts detected"
          description: "Lock timeout rate: {{ $value }} events/sec"

      # Redis Memory
      - alert: HighRedisMemoryUsage
        expr: (redis_memory_used_bytes / redis_memory_max_bytes) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High Redis memory usage"
          description: "Redis memory usage is {{ $value | humanizePercentage }}"
```

## Monitoring Best Practices

### 1. Key Performance Indicators (KPIs)

Monitor these critical metrics:

- **Availability**: `up` metric for all services
- **Latency**: p95 and p99 < 1 second for most endpoints
- **Error Rate**: < 1% of total requests
- **Throughput**: Track requests per second trends
- **Resource Usage**: CPU < 70%, Memory < 80%

### 2. Dashboard Organization

Organize your monitoring views:

1. **Overview Dashboard** - High-level health of all services
2. **Application Dashboard** - Detailed Spring Boot metrics (current dashboard)
3. **Database Dashboard** - PostgreSQL performance and queries
4. **Cache Dashboard** - Redis performance and hit rates
5. **Business Dashboard** - Custom business metrics

### 3. Alert Levels

- **Critical**: Immediate action required (page on-call)
  - Service down
  - High error rate (> 5%)
  - Database lock timeouts
  - Out of memory

- **Warning**: Investigation needed (notify team)
  - High latency (> 3s)
  - High resource usage (> 80%)
  - Rate limit frequently exceeded

- **Info**: Awareness only
  - Deployments
  - Configuration changes

### 4. Retention Policies

Configure appropriate data retention:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s     # How often to scrape targets
  evaluation_interval: 15s  # How often to evaluate rules

storage:
  tsdb:
    retention.time: 15d    # Keep data for 15 days
    retention.size: 10GB   # Maximum storage size
```

## Troubleshooting Guide

### High CPU Usage

1. Check JVM threads:
   ```promql
   jvm_threads_live_threads > 100
   ```

2. Check GC activity:
   ```promql
   rate(jvm_gc_pause_seconds_sum[1m])
   ```

3. Check request rate:
   ```promql
   rate(http_server_requests_seconds_count[1m])
   ```

### High Memory Usage

1. Check heap usage:
   ```promql
   jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
   ```

2. Check cache size:
   ```promql
   redis_memory_used_bytes
   ```

3. Review heap dump if OOM occurs

### Slow Requests

1. Identify slow endpoints:
   ```promql
   histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
   ```

2. Check database query time:
   ```promql
   hikaricp_connections_acquire_seconds
   ```

3. Check Redis latency

### Database Issues

1. Check connection pool:
   ```promql
   hikaricp_connections_pending > 0
   ```

2. Check transaction rollbacks:
   ```promql
   rate(pg_stat_database_xact_rollback[1m])
   ```

3. Check lock timeouts:
   ```promql
   rate(database_lock_timeout_total[1m])
   ```

## Advanced Configuration

### Custom Metrics in Code

Add custom metrics in your services:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Service
public class BookingService {
    private final Counter bookingCreatedCounter;
    private final Timer bookingOperationTimer;
    
    public BookingService(Counter bookingCreatedCounter, 
                         Timer bookingOperationTimer) {
        this.bookingCreatedCounter = bookingCreatedCounter;
        this.bookingOperationTimer = bookingOperationTimer;
    }
    
    public Booking createBooking(BookingRequest request) {
        return bookingOperationTimer.record(() -> {
            // Your booking logic
            Booking booking = // ... create booking
            bookingCreatedCounter.increment();
            return booking;
        });
    }
}
```

### Prometheus Federation

For multi-environment setups, use federation:

```yaml
# Central Prometheus
scrape_configs:
  - job_name: 'federate'
    scrape_interval: 15s
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="spring-boot-app"}'
    static_configs:
      - targets:
        - 'dev-prometheus:9090'
        - 'staging-prometheus:9090'
```

## Useful Queries

### Top 5 Slowest Endpoints

```promql
topk(5, histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)))
```

### Error Rate by Endpoint

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)
```

### Database Connection Pool Efficiency

```promql
hikaricp_connections_idle / hikaricp_connections_max
```

### Redis Hit Rate

```promql
rate(redis_keyspace_hits_total[5m]) / 
(rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) * 100
```

### Request Rate by Status Code

```promql
sum(rate(http_server_requests_seconds_count[1m])) by (status)
```

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)

## Support

For issues or questions:
1. Check Grafana dashboard for anomalies
2. Review Prometheus alerts
3. Check application logs
4. Review this documentation

---

**Last Updated**: 2025-01-29
**Version**: 1.0.0
