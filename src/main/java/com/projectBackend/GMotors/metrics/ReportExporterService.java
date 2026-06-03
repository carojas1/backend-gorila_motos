package com.projectBackend.GMotors.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportExporterService {
    
    private static final Logger log = LoggerFactory.getLogger(ReportExporterService.class);
    
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private GitLabReportService gitLabReportService;
    
    @Value("${metrics.reports.output-dir:./metrics-reports/incremental}")
    private String outputDir;
    
    @Value("${gitlab.enabled:false}")
    private boolean gitlabEnabled;

    // Constructor
    public ReportExporterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Exporta el reporte a JSON en el filesystem y opcionalmente a GitLab
     */
    public Path exportReport(DailyReport report, String label) throws IOException {
        // Configurar ObjectMapper
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 1. Guardar localmente
        Path localPath = exportLocalReport(report, label);
        
        // 2. Subir a GitLab si está habilitado
        if (gitlabEnabled && gitLabReportService != null) {
            try {
                String gitlabUrl = gitLabReportService.uploadReport(report, label);
                log.info("Report also uploaded to GitLab: {}", gitlabUrl);
            } catch (Exception e) {
                log.error("Failed to upload to GitLab, but local copy saved", e);
            }
        }
        
        return localPath;
    }
    
    /**
     * Exporta solo localmente
     */
    private Path exportLocalReport(DailyReport report, String label) throws IOException {
        // Crear directorio
        Path dirPath = Paths.get(outputDir);
        Files.createDirectories(dirPath);
        
        // Nombre del archivo
        String filename = generateFilename(label);
        Path filePath = dirPath.resolve(filename);
        
        // Escribir JSON
        objectMapper.writeValue(filePath.toFile(), report);
        
        log.info("Report exported locally: {}", filePath.toAbsolutePath());
        log.info("File size: {} KB", Files.size(filePath) / 1024);
        
        return filePath;
    }

    /**
     * Carga un reporte desde el filesystem
     */
    public DailyReport loadReport(String filename) throws IOException {
        Path filePath = Paths.get(outputDir, filename);
        
        if (!Files.exists(filePath)) {
            throw new IOException("Report not found: " + filename);
        }
        
        log.info("Loading report from: {}", filePath.toAbsolutePath());
        return objectMapper.readValue(filePath.toFile(), DailyReport.class);
    }

    /**
     * Lista todos los reportes disponibles
     */
    public List<String> listReports() throws IOException {
        Path dirPath = Paths.get(outputDir);
        
        if (!Files.exists(dirPath)) {
            return Collections.emptyList();
        }
        
        return Files.list(dirPath)
            .filter(path -> path.toString().endsWith(".json"))
            .map(path -> path.getFileName().toString())
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
    }

    /**
     * Elimina reportes antiguos
     */
    public void cleanOldReports(int daysToKeep) throws IOException {
        Path dirPath = Paths.get(outputDir);
        
        if (!Files.exists(dirPath)) {
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        Files.list(dirPath)
            .filter(path -> path.toString().endsWith(".json"))
            .filter(path -> {
                try {
                    LocalDateTime fileTime = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(path).toInstant(),
                        java.time.ZoneId.systemDefault()
                    );
                    return fileTime.isBefore(cutoffDate);
                } catch (IOException e) {
                    return false;
                }
            })
            .forEach(path -> {
                try {
                    Files.delete(path);
                    log.info("Deleted old report: {}", path.getFileName());
                } catch (IOException e) {
                    log.error("Failed to delete report: {}", path.getFileName(), e);
                }
            });
    }

    /**
     * Genera el nombre del archivo
     */
    private String generateFilename(String label) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        if (label != null && !label.isEmpty()) {
            return String.format("%s_%s.json", timestamp, label);
        } else {
            return String.format("%s.json", timestamp);
        }
    }

    /**
     * Obtiene el directorio de salida
     */
    public String getOutputDirectory() {
        return outputDir;
    }

    /**
     * Verifica si el directorio es escribible
     */
    public boolean isOutputDirectoryWritable() {
        try {
            Path dirPath = Paths.get(outputDir);
            Files.createDirectories(dirPath);
            return Files.isWritable(dirPath);
        } catch (IOException e) {
            log.error("Output directory is not writable: {}", outputDir, e);
            return false;
        }
    }
}