package com.projectBackend.GMotors.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.nio.file.Path;

@Component
public class DailyReportScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);
    
    private final MetricsCollectorService collectorService;
    private final ReportExporterService exporterService;
    
    @Value("${metrics.reports.timezone:America/Guayaquil}")
    private String timezone;

    // Constructor
    public DailyReportScheduler(MetricsCollectorService collectorService, 
                                ReportExporterService exporterService) {
        this.collectorService = collectorService;
        this.exporterService = exporterService;
    }

    /**
     * Genera reporte al mediodía (12:00 PM)
     * Cron: segundo minuto hora día mes día-semana
     */
    @Scheduled(cron = "${metrics.reports.cron-midday:0 0 12 * * ?}")
    public void generateMiddayReport() {
        log.info("========================================");
        log.info("Starting MIDDAY metrics report generation...");
        log.info("========================================");
        
        generateReport("midday");
    }

    /**
     * Genera reporte a las 4:00 PM
     */
    @Scheduled(cron = "${metrics.reports.cron-afternoon:0 0 16 * * ?}")
    public void generateAfternoonReport() {
        log.info("========================================");
        log.info("Starting AFTERNOON metrics report generation...");
        log.info("========================================");
        
        generateReport("afternoon");
    }

    /**
     * Método privado que realiza la generación
     */
    private void generateReport(String label) {
        try {
            // Recopilar métricas
            DailyReport report = collectorService.collectMetrics(label, timezone);
            
            // Exportar a JSON
            Path filePath = exporterService.exportReport(report, label);
            
            // Log de éxito
            log.info("========================================");
            log.info("✓ Report generated successfully!");
            log.info("  Label: {}", label);
            log.info("  Date: {}", report.getPeriod().getDate());
            log.info("  File: {}", filePath.getFileName());
            log.info("  Total Requests: {}", report.getTrafficSummary().getTotalRequests());
            log.info("  Error Rate: {:.2f}%", report.getTrafficSummary().getErrorRate() * 100);
            log.info("  Uptime: {}", report.getSystemHealth().getUptimeHuman());
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ Error generating {} report", label, e);
            log.error("========================================");
        }
    }

    /**
     * Método público para generar reporte manual (útil para testing)
     */
    public void generateReportManually(String label) {
        log.info("Manual report generation triggered with label: {}", label);
        generateReport(label);
    }

    /**
     * Genera reporte para una fecha específica (útil para históricos)
     */
    public void generateReportForDate(LocalDate date, String label) {
        log.info("Generating report for date: {} with label: {}", date, label);
        
        try {
            // Recopilar métricas con la fecha especificada
            DailyReport report = collectorService.collectMetrics(label, timezone);
            
            // Exportar
            Path filePath = exporterService.exportReport(report, label);
            
            log.info("Historical report generated: {}", filePath.getFileName());
            
        } catch (Exception e) {
            log.error("Error generating historical report for date: {}", date, e);
        }
    }
}