package com.projectBackend.GMotors.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DailyReport {
    
    @JsonProperty("schema_version")
    private String schemaVersion = "1.0";
    
    private Metadata metadata;
    private Period period;
    
    @JsonProperty("traffic_summary")
    private TrafficSummary trafficSummary;
    
    @JsonProperty("endpoint_traffic")
    private List<EndpointTraffic> endpointTraffic;
    
    @JsonProperty("jvm_stats")
    private JvmStats jvmStats;
    
    @JsonProperty("system_health")
    private SystemHealth systemHealth;
    
    @JsonProperty("top_errors")
    private List<TopError> topErrors;
    
    @JsonProperty("performance_summary")
    private PerformanceSummary performanceSummary;
    
    private List<String> notes;

    // Getters y Setters principales
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
    
    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }
    
    public TrafficSummary getTrafficSummary() { return trafficSummary; }
    public void setTrafficSummary(TrafficSummary trafficSummary) { this.trafficSummary = trafficSummary; }
    
    public List<EndpointTraffic> getEndpointTraffic() { return endpointTraffic; }
    public void setEndpointTraffic(List<EndpointTraffic> endpointTraffic) { this.endpointTraffic = endpointTraffic; }
    
    public JvmStats getJvmStats() { return jvmStats; }
    public void setJvmStats(JvmStats jvmStats) { this.jvmStats = jvmStats; }
    
    public SystemHealth getSystemHealth() { return systemHealth; }
    public void setSystemHealth(SystemHealth systemHealth) { this.systemHealth = systemHealth; }
    
    public List<TopError> getTopErrors() { return topErrors; }
    public void setTopErrors(List<TopError> topErrors) { this.topErrors = topErrors; }
    
    public PerformanceSummary getPerformanceSummary() { return performanceSummary; }
    public void setPerformanceSummary(PerformanceSummary performanceSummary) { this.performanceSummary = performanceSummary; }
    
    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    // ========================================================================
    // METADATA
    // ========================================================================
    public static class Metadata {
        @JsonProperty("report_type")
        private String reportType = "incremental_snapshot";
        
        @JsonProperty("generated_at")
        private Instant generatedAt;
        
        private String timezone;
        
        @JsonProperty("application_name")
        private String applicationName;
        
        @JsonProperty("application_version")
        private String applicationVersion;
        
        private String hostname;
        private String label;
        
        @JsonProperty("server_uptime_seconds")
        private Long serverUptimeSeconds;

        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
        
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        
        public String getApplicationVersion() { return applicationVersion; }
        public void setApplicationVersion(String applicationVersion) { this.applicationVersion = applicationVersion; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public Long getServerUptimeSeconds() { return serverUptimeSeconds; }
        public void setServerUptimeSeconds(Long serverUptimeSeconds) { this.serverUptimeSeconds = serverUptimeSeconds; }
    }

    // ========================================================================
    // PERIOD
    // ========================================================================
    public static class Period {
        private String date;
        
        @JsonProperty("start_time")
        private Instant startTime;
        
        @JsonProperty("end_time")
        private Instant endTime;
        
        @JsonProperty("duration_seconds")
        private long durationSeconds;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
    }

    // ========================================================================
    // TRAFFIC SUMMARY
    // ========================================================================
    public static class TrafficSummary {
        @JsonProperty("total_requests")
        private long totalRequests;
        
        @JsonProperty("successful_requests")
        private long successfulRequests;
        
        @JsonProperty("error_requests")
        private long errorRequests;
        
        @JsonProperty("error_rate")
        private double errorRate;
        
        @JsonProperty("requests_per_second_avg")
        private double requestsPerSecondAvg;
        
        @JsonProperty("requests_per_second_peak")
        private Double requestsPerSecondPeak;

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
        
        public long getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(long successfulRequests) { this.successfulRequests = successfulRequests; }
        
        public long getErrorRequests() { return errorRequests; }
        public void setErrorRequests(long errorRequests) { this.errorRequests = errorRequests; }
        
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        
        public double getRequestsPerSecondAvg() { return requestsPerSecondAvg; }
        public void setRequestsPerSecondAvg(double requestsPerSecondAvg) { this.requestsPerSecondAvg = requestsPerSecondAvg; }
        
        public Double getRequestsPerSecondPeak() { return requestsPerSecondPeak; }
        public void setRequestsPerSecondPeak(Double requestsPerSecondPeak) { this.requestsPerSecondPeak = requestsPerSecondPeak; }
    }

    // ========================================================================
    // ENDPOINT TRAFFIC
    // ========================================================================
    public static class EndpointTraffic {
        private String uri;
        private String method;
        private long requests;
        
        @JsonProperty("successful_requests")
        private long successfulRequests;
        
        @JsonProperty("error_requests")
        private long errorRequests;
        
        @JsonProperty("error_rate")
        private double errorRate;
        
        @JsonProperty("errors_by_status")
        private Map<String, Long> errorsByStatus;
        
        private LatencyStats latency;

        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public long getRequests() { return requests; }
        public void setRequests(long requests) { this.requests = requests; }
        
        public long getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(long successfulRequests) { this.successfulRequests = successfulRequests; }
        
        public long getErrorRequests() { return errorRequests; }
        public void setErrorRequests(long errorRequests) { this.errorRequests = errorRequests; }
        
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        
        public Map<String, Long> getErrorsByStatus() { return errorsByStatus; }
        public void setErrorsByStatus(Map<String, Long> errorsByStatus) { this.errorsByStatus = errorsByStatus; }
        
        public LatencyStats getLatency() { return latency; }
        public void setLatency(LatencyStats latency) { this.latency = latency; }
    }

    // ========================================================================
    // LATENCY STATS
    // ========================================================================
    public static class LatencyStats {
        @JsonProperty("avg_ms")
        private double avgMs;
        
        @JsonProperty("min_ms")
        private Double minMs;
        
        @JsonProperty("max_ms")
        private double maxMs;
        
        @JsonProperty("p50_ms")
        private double p50Ms;
        
        @JsonProperty("p95_ms")
        private double p95Ms;
        
        @JsonProperty("p99_ms")
        private double p99Ms;

        public double getAvgMs() { return avgMs; }
        public void setAvgMs(double avgMs) { this.avgMs = avgMs; }
        
        public Double getMinMs() { return minMs; }
        public void setMinMs(Double minMs) { this.minMs = minMs; }
        
        public double getMaxMs() { return maxMs; }
        public void setMaxMs(double maxMs) { this.maxMs = maxMs; }
        
        public double getP50Ms() { return p50Ms; }
        public void setP50Ms(double p50Ms) { this.p50Ms = p50Ms; }
        
        public double getP95Ms() { return p95Ms; }
        public void setP95Ms(double p95Ms) { this.p95Ms = p95Ms; }
        
        public double getP99Ms() { return p99Ms; }
        public void setP99Ms(double p99Ms) { this.p99Ms = p99Ms; }
    }

    // ========================================================================
    // JVM STATS
    // ========================================================================
    public static class JvmStats {
        private MemoryStats memory;
        private ThreadStats threads;
        
        @JsonProperty("garbage_collection")
        private GarbageCollectionStats garbageCollection;
        
        private ClassStats classes;

        public MemoryStats getMemory() { return memory; }
        public void setMemory(MemoryStats memory) { this.memory = memory; }
        
        public ThreadStats getThreads() { return threads; }
        public void setThreads(ThreadStats threads) { this.threads = threads; }
        
        public GarbageCollectionStats getGarbageCollection() { return garbageCollection; }
        public void setGarbageCollection(GarbageCollectionStats garbageCollection) { this.garbageCollection = garbageCollection; }
        
        public ClassStats getClasses() { return classes; }
        public void setClasses(ClassStats classes) { this.classes = classes; }
    }

    public static class MemoryStats {
        @JsonProperty("heap_used_avg_mb")
        private double heapUsedAvgMb;
        
        @JsonProperty("heap_used_peak_mb")
        private double heapUsedPeakMb;
        
        @JsonProperty("heap_max_mb")
        private double heapMaxMb;
        
        @JsonProperty("heap_committed_mb")
        private double heapCommittedMb;
        
        @JsonProperty("non_heap_used_avg_mb")
        private double nonHeapUsedAvgMb;
        
        @JsonProperty("non_heap_used_peak_mb")
        private double nonHeapUsedPeakMb;

        public double getHeapUsedAvgMb() { return heapUsedAvgMb; }
        public void setHeapUsedAvgMb(double heapUsedAvgMb) { this.heapUsedAvgMb = heapUsedAvgMb; }
        
        public double getHeapUsedPeakMb() { return heapUsedPeakMb; }
        public void setHeapUsedPeakMb(double heapUsedPeakMb) { this.heapUsedPeakMb = heapUsedPeakMb; }
        
        public double getHeapMaxMb() { return heapMaxMb; }
        public void setHeapMaxMb(double heapMaxMb) { this.heapMaxMb = heapMaxMb; }
        
        public double getHeapCommittedMb() { return heapCommittedMb; }
        public void setHeapCommittedMb(double heapCommittedMb) { this.heapCommittedMb = heapCommittedMb; }
        
        public double getNonHeapUsedAvgMb() { return nonHeapUsedAvgMb; }
        public void setNonHeapUsedAvgMb(double nonHeapUsedAvgMb) { this.nonHeapUsedAvgMb = nonHeapUsedAvgMb; }
        
        public double getNonHeapUsedPeakMb() { return nonHeapUsedPeakMb; }
        public void setNonHeapUsedPeakMb(double nonHeapUsedPeakMb) { this.nonHeapUsedPeakMb = nonHeapUsedPeakMb; }
    }

    public static class ThreadStats {
        @JsonProperty("live_avg")
        private int liveAvg;
        
        @JsonProperty("live_peak")
        private int livePeak;
        
        @JsonProperty("daemon_avg")
        private int daemonAvg;
        
        @JsonProperty("started_total")
        private Long startedTotal;

        public int getLiveAvg() { return liveAvg; }
        public void setLiveAvg(int liveAvg) { this.liveAvg = liveAvg; }
        
        public int getLivePeak() { return livePeak; }
        public void setLivePeak(int livePeak) { this.livePeak = livePeak; }
        
        public int getDaemonAvg() { return daemonAvg; }
        public void setDaemonAvg(int daemonAvg) { this.daemonAvg = daemonAvg; }
        
        public Long getStartedTotal() { return startedTotal; }
        public void setStartedTotal(Long startedTotal) { this.startedTotal = startedTotal; }
    }

    public static class GarbageCollectionStats {
        @JsonProperty("young_generation")
        private GcGeneration youngGeneration;
        
        @JsonProperty("old_generation")
        private GcGeneration oldGeneration;

        public GcGeneration getYoungGeneration() { return youngGeneration; }
        public void setYoungGeneration(GcGeneration youngGeneration) { this.youngGeneration = youngGeneration; }
        
        public GcGeneration getOldGeneration() { return oldGeneration; }
        public void setOldGeneration(GcGeneration oldGeneration) { this.oldGeneration = oldGeneration; }
    }

    public static class GcGeneration {
        private long count;
        
        @JsonProperty("total_time_ms")
        private long totalTimeMs;
        
        @JsonProperty("avg_time_ms")
        private double avgTimeMs;

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        
        public long getTotalTimeMs() { return totalTimeMs; }
        public void setTotalTimeMs(long totalTimeMs) { this.totalTimeMs = totalTimeMs; }
        
        public double getAvgTimeMs() { return avgTimeMs; }
        public void setAvgTimeMs(double avgTimeMs) { this.avgTimeMs = avgTimeMs; }
    }

    public static class ClassStats {
        private long loaded;
        private long unloaded;

        public long getLoaded() { return loaded; }
        public void setLoaded(long loaded) { this.loaded = loaded; }
        
        public long getUnloaded() { return unloaded; }
        public void setUnloaded(long unloaded) { this.unloaded = unloaded; }
    }

    // ========================================================================
    // SYSTEM HEALTH
    // ========================================================================
    public static class SystemHealth {
        @JsonProperty("overall_status")
        private String overallStatus;
        
        private Map<String, ComponentHealth> components;
        
        @JsonProperty("uptime_seconds")
        private long uptimeSeconds;
        
        @JsonProperty("uptime_human")
        private String uptimeHuman;

        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        
        public Map<String, ComponentHealth> getComponents() { return components; }
        public void setComponents(Map<String, ComponentHealth> components) { this.components = components; }
        
        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
        
        public String getUptimeHuman() { return uptimeHuman; }
        public void setUptimeHuman(String uptimeHuman) { this.uptimeHuman = uptimeHuman; }
    }

    public static class ComponentHealth {
        private String status;
        private Map<String, Object> details;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    // ========================================================================
    // TOP ERRORS
    // ========================================================================
    public static class TopError {
        private String endpoint;
        private String method;
        private int status;
        private long count;
        private double percentage;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    // ========================================================================
    // PERFORMANCE SUMMARY
    // ========================================================================
    public static class PerformanceSummary {
        @JsonProperty("fastest_endpoint")
        private EndpointSummary fastestEndpoint;
        
        @JsonProperty("slowest_endpoint")
        private EndpointSummary slowestEndpoint;
        
        @JsonProperty("most_used_endpoint")
        private EndpointSummary mostUsedEndpoint;

        public EndpointSummary getFastestEndpoint() { return fastestEndpoint; }
        public void setFastestEndpoint(EndpointSummary fastestEndpoint) { this.fastestEndpoint = fastestEndpoint; }
        
        public EndpointSummary getSlowestEndpoint() { return slowestEndpoint; }
        public void setSlowestEndpoint(EndpointSummary slowestEndpoint) { this.slowestEndpoint = slowestEndpoint; }
        
        public EndpointSummary getMostUsedEndpoint() { return mostUsedEndpoint; }
        public void setMostUsedEndpoint(EndpointSummary mostUsedEndpoint) { this.mostUsedEndpoint = mostUsedEndpoint; }
    }

    public static class EndpointSummary {
        private String uri;
        private String method;
        
        @JsonProperty("avg_latency_ms")
        private Double avgLatencyMs;
        
        private Long requests;

        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Double getAvgLatencyMs() { return avgLatencyMs; }
        public void setAvgLatencyMs(Double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
        
        public Long getRequests() { return requests; }
        public void setRequests(Long requests) { this.requests = requests; }
    }
}