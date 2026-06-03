package com.projectBackend.GMotors.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MetricsCollectorService {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorService.class);
    
    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    // Constructor
    public MetricsCollectorService(MeterRegistry meterRegistry, HealthEndpoint healthEndpoint) {
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
    }

    /**
     * Recopila todas las métricas del sistema y genera un reporte completo
     */
    public DailyReport collectMetrics(String label, String timezone) {
        log.info("Collecting metrics with label: {}", label);
        
        DailyReport report = new DailyReport();
        
        // Metadata
        report.setMetadata(buildMetadata(label, timezone));
        
        // Period
        report.setPeriod(buildPeriod(timezone));
        
        // HTTP Traffic
        Map<EndpointKey, MetricData> endpointMetrics = collectHttpMetrics();
        report.setTrafficSummary(buildTrafficSummary(endpointMetrics));
        report.setEndpointTraffic(buildEndpointTraffic(endpointMetrics));
        
        // JVM Stats
        report.setJvmStats(collectJvmStats());
        
        // System Health
        report.setSystemHealth(collectSystemHealth());
        
        // Top Errors
        report.setTopErrors(buildTopErrors(endpointMetrics));
        
        // Performance Summary
        report.setPerformanceSummary(buildPerformanceSummary(endpointMetrics));
        
        // Notes
        report.setNotes(new ArrayList<>());
        
        log.info("Metrics collected successfully. Total requests: {}", 
                 report.getTrafficSummary().getTotalRequests());
        
        return report;
    }

    // ========================================================================
    // METADATA
    // ========================================================================
    private DailyReport.Metadata buildMetadata(String label, String timezone) {
        DailyReport.Metadata metadata = new DailyReport.Metadata();
        metadata.setGeneratedAt(Instant.now());
        metadata.setTimezone(timezone);
        metadata.setApplicationName(getApplicationName());
        metadata.setApplicationVersion(getApplicationVersion());
        metadata.setHostname(getHostname());
        metadata.setLabel(label);
        
        // Server uptime
        Gauge uptime = meterRegistry.find("process.uptime").gauge();
        if (uptime != null) {
            metadata.setServerUptimeSeconds((long) uptime.value());
        }
        
        return metadata;
    }

    // ========================================================================
    // PERIOD
    // ========================================================================
    private DailyReport.Period buildPeriod(String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(zone);
        
        DailyReport.Period period = new DailyReport.Period();
        period.setDate(now.toLocalDate().format(DateTimeFormatter.ISO_DATE));
        period.setStartTime(startOfDay.toInstant());
        period.setEndTime(now.toInstant());
        period.setDurationSeconds(Duration.between(startOfDay, now).getSeconds());
        
        return period;
    }

    // ========================================================================
    // HTTP METRICS - CORE DEL REPORTE
    // ========================================================================
    private Map<EndpointKey, MetricData> collectHttpMetrics() {
        Map<EndpointKey, MetricData> metrics = new HashMap<>();
        
        // Buscar todos los timers http.server.requests
        Collection<io.micrometer.core.instrument.Timer> timers = 
            meterRegistry.find("http.server.requests").timers();
        
        if (timers.isEmpty()) {
            log.warn("No HTTP metrics found. Server may not have received requests yet.");
            return metrics;
        }
        
        for (io.micrometer.core.instrument.Timer timer : timers) {
            String uri = timer.getId().getTag("uri");
            String method = timer.getId().getTag("method");
            String status = timer.getId().getTag("status");
            
            if (uri == null || method == null) {
                continue;
            }
            
            // Normalizar URI
            uri = normalizeUri(uri);
            
            EndpointKey key = new EndpointKey(uri, method);
            MetricData data = metrics.computeIfAbsent(key, k -> new MetricData());
            
            long count = timer.count();
            data.totalRequests += count;
            
            // Clasificar por status code
            if (status != null) {
                int statusCode = Integer.parseInt(status);
                if (statusCode >= 400) {
                    data.errorRequests += count;
                    data.errorsByStatus.merge(status, count, Long::sum);
                } else {
                    data.successfulRequests += count;
                }
            } else {
                data.successfulRequests += count;
            }
            
            // Latencia
            data.totalLatencyMs += timer.totalTime(TimeUnit.MILLISECONDS);
            double currentMax = timer.max(TimeUnit.MILLISECONDS);
            data.maxLatencyMs = Math.max(data.maxLatencyMs, currentMax);
            
            // Percentiles - usando snapshot
            try {
                HistogramSnapshot snapshot = timer.takeSnapshot();
                ValueAtPercentile[] percentileValues = snapshot.percentileValues();
                
                if (percentileValues != null && percentileValues.length >= 3) {
                    data.p50Ms = percentileValues[0].value(TimeUnit.MILLISECONDS);
                    data.p95Ms = percentileValues[1].value(TimeUnit.MILLISECONDS);
                    data.p99Ms = percentileValues[2].value(TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                log.debug("Could not extract percentiles for {}: {}", uri, e.getMessage());
            }
        }
        
        return metrics;
    }

    // ========================================================================
    // TRAFFIC SUMMARY
    // ========================================================================
    private DailyReport.TrafficSummary buildTrafficSummary(Map<EndpointKey, MetricData> metrics) {
        DailyReport.TrafficSummary summary = new DailyReport.TrafficSummary();
        
        long totalRequests = metrics.values().stream()
            .mapToLong(m -> m.totalRequests)
            .sum();
        
        long successfulRequests = metrics.values().stream()
            .mapToLong(m -> m.successfulRequests)
            .sum();
        
        long errorRequests = metrics.values().stream()
            .mapToLong(m -> m.errorRequests)
            .sum();
        
        summary.setTotalRequests(totalRequests);
        summary.setSuccessfulRequests(successfulRequests);
        summary.setErrorRequests(errorRequests);
        summary.setErrorRate(totalRequests > 0 ? (double) errorRequests / totalRequests : 0.0);
        
        // Requests por segundo
        Gauge uptime = meterRegistry.find("process.uptime").gauge();
        if (uptime != null && uptime.value() > 0) {
            summary.setRequestsPerSecondAvg(totalRequests / uptime.value());
        }
        
        return summary;
    }

    // ========================================================================
    // ENDPOINT TRAFFIC
    // ========================================================================
    private List<DailyReport.EndpointTraffic> buildEndpointTraffic(Map<EndpointKey, MetricData> metrics) {
        return metrics.entrySet().stream()
            .map(entry -> {
                EndpointKey key = entry.getKey();
                MetricData data = entry.getValue();
                
                DailyReport.EndpointTraffic traffic = new DailyReport.EndpointTraffic();
                traffic.setUri(key.uri);
                traffic.setMethod(key.method);
                traffic.setRequests(data.totalRequests);
                traffic.setSuccessfulRequests(data.successfulRequests);
                traffic.setErrorRequests(data.errorRequests);
                traffic.setErrorRate(data.totalRequests > 0 ? 
                    (double) data.errorRequests / data.totalRequests : 0.0);
                traffic.setErrorsByStatus(data.errorsByStatus);
                
                // Latency stats
                DailyReport.LatencyStats latency = new DailyReport.LatencyStats();
                latency.setAvgMs(data.totalRequests > 0 ? 
                    data.totalLatencyMs / data.totalRequests : 0.0);
                latency.setMaxMs(data.maxLatencyMs);
                latency.setP50Ms(data.p50Ms);
                latency.setP95Ms(data.p95Ms);
                latency.setP99Ms(data.p99Ms);
                traffic.setLatency(latency);
                
                return traffic;
            })
            .sorted(Comparator.comparingLong(DailyReport.EndpointTraffic::getRequests).reversed())
            .collect(Collectors.toList());
    }

    // ========================================================================
    // JVM STATS
    // ========================================================================
    private DailyReport.JvmStats collectJvmStats() {
        DailyReport.JvmStats stats = new DailyReport.JvmStats();
        stats.setMemory(collectMemoryStats());
        stats.setThreads(collectThreadStats());
        stats.setGarbageCollection(collectGcStats());
        stats.setClasses(collectClassStats());
        return stats;
    }

    private DailyReport.MemoryStats collectMemoryStats() {
        DailyReport.MemoryStats memory = new DailyReport.MemoryStats();
        
        Gauge heapUsed = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge();
        if (heapUsed != null) {
            double valueMb = heapUsed.value() / (1024 * 1024);
            memory.setHeapUsedAvgMb(valueMb);
            memory.setHeapUsedPeakMb(valueMb);
        }
        
        Gauge heapMax = meterRegistry.find("jvm.memory.max").tag("area", "heap").gauge();
        if (heapMax != null) {
            memory.setHeapMaxMb(heapMax.value() / (1024 * 1024));
        }
        
        Gauge heapCommitted = meterRegistry.find("jvm.memory.committed").tag("area", "heap").gauge();
        if (heapCommitted != null) {
            memory.setHeapCommittedMb(heapCommitted.value() / (1024 * 1024));
        }
        
        Gauge nonHeapUsed = meterRegistry.find("jvm.memory.used").tag("area", "nonheap").gauge();
        if (nonHeapUsed != null) {
            double valueMb = nonHeapUsed.value() / (1024 * 1024);
            memory.setNonHeapUsedAvgMb(valueMb);
            memory.setNonHeapUsedPeakMb(valueMb);
        }
        
        return memory;
    }

    private DailyReport.ThreadStats collectThreadStats() {
        DailyReport.ThreadStats threads = new DailyReport.ThreadStats();
        
        Gauge liveThreads = meterRegistry.find("jvm.threads.live").gauge();
        if (liveThreads != null) {
            int value = (int) liveThreads.value();
            threads.setLiveAvg(value);
            threads.setLivePeak(value);
        }
        
        Gauge daemonThreads = meterRegistry.find("jvm.threads.daemon").gauge();
        if (daemonThreads != null) {
            threads.setDaemonAvg((int) daemonThreads.value());
        }
        
        Counter startedThreads = meterRegistry.find("jvm.threads.started").counter();
        if (startedThreads != null) {
            threads.setStartedTotal((long) startedThreads.count());
        }
        
        return threads;
    }

    private DailyReport.GarbageCollectionStats collectGcStats() {
        DailyReport.GarbageCollectionStats gc = new DailyReport.GarbageCollectionStats();
        
        io.micrometer.core.instrument.Timer youngGc = meterRegistry.find("jvm.gc.pause")
            .tag("action", "end of minor GC").timer();
        
        if (youngGc == null) {
            youngGc = meterRegistry.find("jvm.gc.pause").tag("cause", "Allocation Failure").timer();
        }
        
        if (youngGc != null) {
            DailyReport.GcGeneration young = new DailyReport.GcGeneration();
            young.setCount(youngGc.count());
            young.setTotalTimeMs((long) youngGc.totalTime(TimeUnit.MILLISECONDS));
            young.setAvgTimeMs(youngGc.count() > 0 ? youngGc.mean(TimeUnit.MILLISECONDS) : 0);
            gc.setYoungGeneration(young);
        } else {
            gc.setYoungGeneration(new DailyReport.GcGeneration());
        }
        
        io.micrometer.core.instrument.Timer oldGc = meterRegistry.find("jvm.gc.pause")
            .tag("action", "end of major GC").timer();
        
        if (oldGc != null) {
            DailyReport.GcGeneration old = new DailyReport.GcGeneration();
            old.setCount(oldGc.count());
            old.setTotalTimeMs((long) oldGc.totalTime(TimeUnit.MILLISECONDS));
            old.setAvgTimeMs(oldGc.count() > 0 ? oldGc.mean(TimeUnit.MILLISECONDS) : 0);
            gc.setOldGeneration(old);
        } else {
            gc.setOldGeneration(new DailyReport.GcGeneration());
        }
        
        return gc;
    }

    private DailyReport.ClassStats collectClassStats() {
        DailyReport.ClassStats classes = new DailyReport.ClassStats();
        
        Gauge loadedClasses = meterRegistry.find("jvm.classes.loaded").gauge();
        if (loadedClasses != null) {
            classes.setLoaded((long) loadedClasses.value());
        }
        
        Counter unloadedClasses = meterRegistry.find("jvm.classes.unloaded").counter();
        if (unloadedClasses != null) {
            classes.setUnloaded((long) unloadedClasses.count());
        }
        
        return classes;
    }

    // ========================================================================
    // SYSTEM HEALTH
    // ========================================================================
    private DailyReport.SystemHealth collectSystemHealth() {
    DailyReport.SystemHealth health = new DailyReport.SystemHealth();
    
    try {
        var healthStatus = healthEndpoint.health();
        health.setOverallStatus(healthStatus.getStatus().getCode());
        health.setComponents(new HashMap<>()); // Sin componentes por ahora
    } catch (Exception e) {
        health.setOverallStatus("UNKNOWN");
        health.setComponents(new HashMap<>());
    }
    
    // ... uptime ...
    return health;
}

    // ========================================================================
    // TOP ERRORS
    // ========================================================================
    private List<DailyReport.TopError> buildTopErrors(Map<EndpointKey, MetricData> metrics) {
        long totalErrors = metrics.values().stream().mapToLong(m -> m.errorRequests).sum();
        
        if (totalErrors == 0) {
            return new ArrayList<>();
        }
        
        return metrics.entrySet().stream()
            .flatMap(entry -> entry.getValue().errorsByStatus.entrySet().stream()
                .map(error -> {
                    DailyReport.TopError topError = new DailyReport.TopError();
                    topError.setEndpoint(entry.getKey().uri);
                    topError.setMethod(entry.getKey().method);
                    topError.setStatus(Integer.parseInt(error.getKey()));
                    topError.setCount(error.getValue());
                    topError.setPercentage((double) error.getValue() / totalErrors * 100);
                    return topError;
                }))
            .sorted(Comparator.comparingLong(DailyReport.TopError::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // PERFORMANCE SUMMARY
    // ========================================================================
    private DailyReport.PerformanceSummary buildPerformanceSummary(Map<EndpointKey, MetricData> metrics) {
        DailyReport.PerformanceSummary summary = new DailyReport.PerformanceSummary();
        
        if (metrics.isEmpty()) {
            return summary;
        }
        
        // Fastest
        metrics.entrySet().stream()
            .filter(e -> e.getValue().totalRequests > 0)
            .min(Comparator.comparingDouble(e -> e.getValue().totalLatencyMs / e.getValue().totalRequests))
            .ifPresent(entry -> {
                DailyReport.EndpointSummary fastest = new DailyReport.EndpointSummary();
                fastest.setUri(entry.getKey().uri);
                fastest.setMethod(entry.getKey().method);
                fastest.setAvgLatencyMs(entry.getValue().totalLatencyMs / entry.getValue().totalRequests);
                summary.setFastestEndpoint(fastest);
            });
        
        // Slowest
        metrics.entrySet().stream()
            .filter(e -> e.getValue().totalRequests > 0)
            .max(Comparator.comparingDouble(e -> e.getValue().totalLatencyMs / e.getValue().totalRequests))
            .ifPresent(entry -> {
                DailyReport.EndpointSummary slowest = new DailyReport.EndpointSummary();
                slowest.setUri(entry.getKey().uri);
                slowest.setMethod(entry.getKey().method);
                slowest.setAvgLatencyMs(entry.getValue().totalLatencyMs / entry.getValue().totalRequests);
                summary.setSlowestEndpoint(slowest);
            });
        
        // Most used
        metrics.entrySet().stream()
            .max(Comparator.comparingLong(e -> e.getValue().totalRequests))
            .ifPresent(entry -> {
                DailyReport.EndpointSummary mostUsed = new DailyReport.EndpointSummary();
                mostUsed.setUri(entry.getKey().uri);
                mostUsed.setMethod(entry.getKey().method);
                mostUsed.setRequests(entry.getValue().totalRequests);
                summary.setMostUsedEndpoint(mostUsed);
            });
        
        return summary;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private String normalizeUri(String uri) {
        if (uri == null) return "unknown";
        if (uri.contains("{") && uri.contains("}")) return uri;
        
        uri = uri.replaceAll("/\\d+(/|$)", "/{id}$1");
        uri = uri.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(/|$)", "/{uuid}$1");
        
        return uri;
    }

    private String getApplicationName() {
        // Obtener tags de la configuración
        try {
            java.lang.reflect.Method getCommonTagsMethod = 
                meterRegistry.config().getClass().getMethod("commonTags");
            Object tagsObj = getCommonTagsMethod.invoke(meterRegistry.config());
            
            if (tagsObj instanceof Iterable) {
                @SuppressWarnings("unchecked")
                Iterable<Tag> tags = (Iterable<Tag>) tagsObj;
                for (Tag tag : tags) {
                    if ("application".equals(tag.getKey())) {
                        return tag.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not get application name from tags", e);
        }
        return "GMotors";
    }

    private String getApplicationVersion() {
        return "1.0.0";
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return String.format("%d days, %d hours", days, hours);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================
    
    private static class EndpointKey {
        final String uri;
        final String method;
        
        EndpointKey(String uri, String method) {
            this.uri = uri;
            this.method = method;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndpointKey that = (EndpointKey) o;
            return Objects.equals(uri, that.uri) && Objects.equals(method, that.method);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(uri, method);
        }
    }

    private static class MetricData {
        long totalRequests = 0;
        long successfulRequests = 0;
        long errorRequests = 0;
        double totalLatencyMs = 0;
        double maxLatencyMs = 0;
        double p50Ms = 0;
        double p95Ms = 0;
        double p99Ms = 0;
        Map<String, Long> errorsByStatus = new HashMap<>();
    }
}