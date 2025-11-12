package metrics

import (
	"fmt"
	"net/http"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Collector manages Prometheus metrics
type Collector struct {
	// HTTP metrics
	httpRequestsTotal    *prometheus.CounterVec
	httpRequestDuration  *prometheus.HistogramVec
	httpRequestsInFlight prometheus.Gauge

	// Database metrics
	dbConnectionsOpen prometheus.Gauge
	dbConnectionsIdle prometheus.Gauge
	dbQueryDuration   *prometheus.HistogramVec
	dbQueriesTotal    *prometheus.CounterVec

	// Cache metrics
	cacheHitsTotal         *prometheus.CounterVec
	cacheMissesTotal       *prometheus.CounterVec
	cacheOperationDuration *prometheus.HistogramVec

	// Business metrics
	bookingsTotal       *prometheus.CounterVec
	activeBookingsGauge prometheus.Gauge
}

// NewCollector creates a new metrics collector
func NewCollector() *Collector {
	c := &Collector{
		// HTTP metrics
		httpRequestsTotal: prometheus.NewCounterVec(
			prometheus.CounterOpts{
				Name: "http_requests_total",
				Help: "Total number of HTTP requests",
			},
			[]string{"method", "endpoint", "status"},
		),
		httpRequestDuration: prometheus.NewHistogramVec(
			prometheus.HistogramOpts{
				Name:    "http_request_duration_seconds",
				Help:    "HTTP request latency in seconds",
				Buckets: prometheus.DefBuckets,
			},
			[]string{"method", "endpoint"},
		),
		httpRequestsInFlight: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "http_requests_in_flight",
				Help: "Current number of HTTP requests being served",
			},
		),

		// Database metrics
		dbConnectionsOpen: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "db_connections_open",
				Help: "Number of open database connections",
			},
		),
		dbConnectionsIdle: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "db_connections_idle",
				Help: "Number of idle database connections",
			},
		),
		dbQueryDuration: prometheus.NewHistogramVec(
			prometheus.HistogramOpts{
				Name:    "db_query_duration_seconds",
				Help:    "Database query duration in seconds",
				Buckets: prometheus.DefBuckets,
			},
			[]string{"query_type"},
		),
		dbQueriesTotal: prometheus.NewCounterVec(
			prometheus.CounterOpts{
				Name: "db_queries_total",
				Help: "Total number of database queries",
			},
			[]string{"query_type", "status"},
		),

		// Cache metrics
		cacheHitsTotal: prometheus.NewCounterVec(
			prometheus.CounterOpts{
				Name: "cache_hits_total",
				Help: "Total number of cache hits",
			},
			[]string{"cache_type"},
		),
		cacheMissesTotal: prometheus.NewCounterVec(
			prometheus.CounterOpts{
				Name: "cache_misses_total",
				Help: "Total number of cache misses",
			},
			[]string{"cache_type"},
		),
		cacheOperationDuration: prometheus.NewHistogramVec(
			prometheus.HistogramOpts{
				Name:    "cache_operation_duration_seconds",
				Help:    "Cache operation duration in seconds",
				Buckets: prometheus.DefBuckets,
			},
			[]string{"operation", "cache_type"},
		),

		// Business metrics
		bookingsTotal: prometheus.NewCounterVec(
			prometheus.CounterOpts{
				Name: "bookings_total",
				Help: "Total number of bookings",
			},
			[]string{"status"},
		),
		activeBookingsGauge: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "active_bookings",
				Help: "Current number of active bookings",
			},
		),
	}

	// Register all metrics
	prometheus.MustRegister(
		c.httpRequestsTotal,
		c.httpRequestDuration,
		c.httpRequestsInFlight,
		c.dbConnectionsOpen,
		c.dbConnectionsIdle,
		c.dbQueryDuration,
		c.dbQueriesTotal,
		c.cacheHitsTotal,
		c.cacheMissesTotal,
		c.cacheOperationDuration,
		c.bookingsTotal,
		c.activeBookingsGauge,
	)

	return c
}

// RecordHTTPRequest records an HTTP request metric
func (c *Collector) RecordHTTPRequest(method, endpoint, status string, duration time.Duration) {
	c.httpRequestsTotal.WithLabelValues(method, endpoint, status).Inc()
	c.httpRequestDuration.WithLabelValues(method, endpoint).Observe(duration.Seconds())
}

// InFlightHTTPRequest increments/decrements in-flight request counter
func (c *Collector) InFlightHTTPRequest(delta float64) {
	if delta > 0 {
		c.httpRequestsInFlight.Inc()
	} else {
		c.httpRequestsInFlight.Dec()
	}
}

// RecordDBQuery records a database query metric
func (c *Collector) RecordDBQuery(queryType, status string, duration time.Duration) {
	c.dbQueriesTotal.WithLabelValues(queryType, status).Inc()
	c.dbQueryDuration.WithLabelValues(queryType).Observe(duration.Seconds())
}

// RecordCacheHit records a cache hit
func (c *Collector) RecordCacheHit(cacheType string) {
	c.cacheHitsTotal.WithLabelValues(cacheType).Inc()
}

// RecordCacheMiss records a cache miss
func (c *Collector) RecordCacheMiss(cacheType string) {
	c.cacheMissesTotal.WithLabelValues(cacheType).Inc()
}

// RecordCacheOperation records a cache operation duration
func (c *Collector) RecordCacheOperation(operation, cacheType string, duration time.Duration) {
	c.cacheOperationDuration.WithLabelValues(operation, cacheType).Observe(duration.Seconds())
}

// RecordBooking records a booking creation
func (c *Collector) RecordBooking(status string) {
	c.bookingsTotal.WithLabelValues(status).Inc()
}

// SetActiveBookings sets the current number of active bookings
func (c *Collector) SetActiveBookings(count float64) {
	c.activeBookingsGauge.Set(count)
}

// StartMetricsServer starts the Prometheus metrics HTTP server
func (c *Collector) StartMetricsServer(port string) {
	http.Handle("/metrics", promhttp.Handler())
	addr := fmt.Sprintf(":%s", port)
	http.ListenAndServe(addr, nil)
}
