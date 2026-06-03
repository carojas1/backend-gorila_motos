package com.projectBackend.GMotors.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectBackend.GMotors.metrics.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics/reports")
public class MetricsReportController {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsReportController.class);
    
    private final DailyReportScheduler scheduler;
    private final MetricsCollectorService collectorService;
    private final ReportExporterService exporterService;
    private final GitLabReportService gitLabReportService;
    
    @Value("${metrics.reports.timezone:America/Guayaquil}")
    private String timezone;

    // Constructor
    public MetricsReportController(DailyReportScheduler scheduler,
                                   MetricsCollectorService collectorService,
                                   ReportExporterService exporterService,
                                   GitLabReportService gitLabReportService) {
        this.scheduler = scheduler;
        this.collectorService = collectorService;
        this.exporterService = exporterService;
        this.gitLabReportService = gitLabReportService;
    }

    /**
     * Genera un reporte manual inmediatamente
     * 
     * GET /api/metrics/reports/generate?label=test
     */
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam(required = false, defaultValue = "manual") String label) {
        
        log.info("Manual report generation requested with label: {}", label);
        
        try {
            scheduler.generateReportManually(label);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report generated successfully");
            response.put("label", label);
            response.put("outputDirectory", exporterService.getOutputDirectory());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating manual report", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error generating report: " + e.getMessage());
            response.put("label", label);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Genera el reporte de mediodía manualmente
     * 
     * POST /api/metrics/reports/midday
     */
    @PostMapping("/midday")
    public ResponseEntity<Map<String, Object>> generateMiddayReport() {
        log.info("Manual midday report triggered");
        
        try {
            scheduler.generateMiddayReport();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Midday report generated");
            response.put("label", "midday");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating midday report", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Genera el reporte de tarde manualmente
     * 
     * POST /api/metrics/reports/afternoon
     */
    @PostMapping("/afternoon")
    public ResponseEntity<Map<String, Object>> generateAfternoonReport() {
        log.info("Manual afternoon report triggered");
        
        try {
            scheduler.generateAfternoonReport();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Afternoon report generated");
            response.put("label", "afternoon");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating afternoon report", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtiene un preview del reporte sin guardarlo
     * 
     * GET /api/metrics/reports/preview?label=test
     */
    @GetMapping("/preview")
    public ResponseEntity<?> previewReport(
            @RequestParam(required = false, defaultValue = "preview") String label) {
        
        log.info("Report preview requested with label: {}", label);
        
        try {
            DailyReport report = collectorService.collectMetrics(label, timezone);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Error generating preview", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error generating preview: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lista todos los reportes generados
     * 
     * GET /api/metrics/reports/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listReports() {
        log.info("Listing all generated reports");
        
        try {
            List<String> reports = exporterService.listReports();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", reports.size());
            response.put("reports", reports);
            response.put("outputDirectory", exporterService.getOutputDirectory());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error listing reports", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Carga un reporte específico por nombre de archivo
     * 
     * GET /api/metrics/reports/load/2026-01-23_12-00-00_midday.json
     */
    @GetMapping("/load/{filename}")
    public ResponseEntity<?> loadReport(@PathVariable String filename) {
        log.info("Loading report: {}", filename);
        
        try {
            DailyReport report = exporterService.loadReport(filename);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Error loading report: {}", filename, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Report not found or error loading: " + e.getMessage());
            response.put("filename", filename);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Limpia reportes antiguos (más de N días)
     * 
     * DELETE /api/metrics/reports/cleanup?days=30
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldReports(
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("Cleanup requested for reports older than {} days", days);
        
        try {
            exporterService.cleanOldReports(days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Old reports cleaned up successfully");
            response.put("daysKept", days);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error cleaning up reports", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtiene información sobre la configuración actual
     * 
     * GET /api/metrics/reports/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        log.info("Configuration info requested");
        
        Map<String, Object> info = new HashMap<>();
        info.put("timezone", timezone);
        info.put("outputDirectory", exporterService.getOutputDirectory());
        info.put("isWritable", exporterService.isOutputDirectoryWritable());
        
        try {
            List<String> reports = exporterService.listReports();
            info.put("totalReports", reports.size());
        } catch (Exception e) {
            info.put("totalReports", 0);
        }
        
        return ResponseEntity.ok(info);
    }

    /**
     * Health check del sistema de métricas
     * 
     * GET /api/metrics/reports/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "UP");
        health.put("outputDirectory", exporterService.getOutputDirectory());
        health.put("writable", exporterService.isOutputDirectoryWritable());
        health.put("timezone", timezone);
        
        // GitLab status
        if (gitLabReportService != null) {
            health.put("gitlabEnabled", true);
            health.put("gitlabConnected", gitLabReportService.testConnection());
        } else {
            health.put("gitlabEnabled", false);
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Test de conexión con GitLab
     * 
     * GET /api/metrics/reports/gitlab/test
     */
    @GetMapping("/gitlab/test")
    public ResponseEntity<Map<String, Object>> testGitLab() {
        Map<String, Object> response = new HashMap<>();
        
        if (gitLabReportService == null) {
            response.put("success", false);
            response.put("message", "GitLab is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        try {
            boolean connected = gitLabReportService.testConnection();
            String projectInfo = gitLabReportService.getProjectInfo();
            
            response.put("success", connected);
            response.put("connected", connected);
            response.put("projectInfo", projectInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("GitLab connection test failed", e);
            
            response.put("success", false);
            response.put("message", "Connection failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Lista reportes en GitLab
     * 
     * GET /api/metrics/reports/gitlab/list
     */
    @GetMapping("/gitlab/list")
    public ResponseEntity<?> listGitLabReports() {
        if (gitLabReportService == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "GitLab is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        try {
            String reportsJson = gitLabReportService.listReports();
            return ResponseEntity.ok(reportsJson);
            
        } catch (Exception e) {
            log.error("Error listing GitLab reports", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}