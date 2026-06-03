package com.projectBackend.GMotors.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@ConditionalOnProperty(name = "gitlab.enabled", havingValue = "true")
public class GitLabReportService {
    
    private static final Logger log = LoggerFactory.getLogger(GitLabReportService.class);
    
    @Value("${gitlab.token}")
    private String gitlabToken;
    
    @Value("${gitlab.project-id}")
    private String projectId;
    
    @Value("${gitlab.base-url:https://gitlab.com}")
    private String baseUrl;
    
    @Value("${gitlab.branch:main}")
    private String branch;
    
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public GitLabReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Sube un reporte a GitLab
     * @param report El reporte a subir
     * @param label Etiqueta del reporte (midday, afternoon, etc)
     * @return URL del archivo en GitLab
     */
    public String uploadReport(DailyReport report, String label) throws IOException {
        log.info("Uploading report to GitLab with label: {}", label);
        
        // 1. Generar nombre de archivo con estructura de carpetas
        String filePath = generateFilePath(report, label);
        
        // 2. Convertir reporte a JSON
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(report);
        
        // 3. Codificar a Base64 (requerido por GitLab API)
        String base64Content = Base64.getEncoder().encodeToString(jsonContent.getBytes());
        
        // 4. Verificar si el archivo ya existe
        boolean fileExists = checkFileExists(filePath);
        
        // 5. Crear o actualizar el archivo
        String fileUrl;
        if (fileExists) {
            fileUrl = updateFile(filePath, base64Content, report);
        } else {
            fileUrl = createFile(filePath, base64Content, report);
        }
        
        log.info("Report uploaded successfully to GitLab: {}", fileUrl);
        return fileUrl;
    }

    /**
     * Genera la ruta del archivo con estructura de carpetas
     * Ejemplo: 2026/01/2026-01-23_12-00-00_midday.json
     */
    private String generateFilePath(DailyReport report, String label) {
        LocalDate date = LocalDate.parse(report.getPeriod().getDate());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        
        // Estructura: YYYY/MM/YYYY-MM-DD_HH-mm-ss_label.json
        return String.format("%d/%02d/%s_%s.json", 
            date.getYear(), 
            date.getMonthValue(),
            timestamp,
            label
        );
    }

    /**
     * Verifica si un archivo ya existe en GitLab
     */
    private boolean checkFileExists(String filePath) {
        try {
            String encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8");
            String url = String.format("%s/api/v4/projects/%s/repository/files/%s?ref=%s",
                baseUrl, projectId, encodedPath, branch);
            
            Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", gitlabToken)
                .get()
                .build();
            
            Response response = httpClient.newCall(request).execute();
            boolean exists = response.isSuccessful();
            response.close();
            
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Crea un nuevo archivo en GitLab
     */
    private String createFile(String filePath, String base64Content, DailyReport report) throws IOException {
        String encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8");
        String url = String.format("%s/api/v4/projects/%s/repository/files/%s",
            baseUrl, projectId, encodedPath);
        
        // Crear payload
        String commitMessage = String.format("Add daily report - %s - %s requests",
            report.getPeriod().getDate(),
            report.getTrafficSummary().getTotalRequests()
        );
        
        JsonObject payload = new JsonObject();
        payload.addProperty("branch", branch);
        payload.addProperty("content", base64Content);
        payload.addProperty("commit_message", commitMessage);
        payload.addProperty("encoding", "base64");
        
        RequestBody body = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", gitlabToken)
            .post(body)
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "No error details";
            throw new IOException("Failed to create file in GitLab: " + response.code() + " - " + errorBody);
        }
        
        response.close();
        
        // Generar URL del archivo
        return generateFileUrl(filePath);
    }

    /**
     * Actualiza un archivo existente en GitLab
     */
    private String updateFile(String filePath, String base64Content, DailyReport report) throws IOException {
        String encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8");
        String url = String.format("%s/api/v4/projects/%s/repository/files/%s",
            baseUrl, projectId, encodedPath);
        
        String commitMessage = String.format("Update report - %s - %s requests",
            report.getPeriod().getDate(),
            report.getTrafficSummary().getTotalRequests()
        );
        
        JsonObject payload = new JsonObject();
        payload.addProperty("branch", branch);
        payload.addProperty("content", base64Content);
        payload.addProperty("commit_message", commitMessage);
        payload.addProperty("encoding", "base64");
        
        RequestBody body = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", gitlabToken)
            .put(body)
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "No error details";
            throw new IOException("Failed to update file in GitLab: " + response.code() + " - " + errorBody);
        }
        
        response.close();
        
        return generateFileUrl(filePath);
    }

    /**
     * Descarga un reporte desde GitLab
     */
    public DailyReport downloadReport(String filePath) throws IOException {
        String encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8");
        String url = String.format("%s/api/v4/projects/%s/repository/files/%s/raw?ref=%s",
            baseUrl, projectId, encodedPath, branch);
        
        Request request = new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", gitlabToken)
            .get()
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            throw new IOException("Failed to download file from GitLab: " + response.code());
        }
        
        String jsonContent = response.body().string();
        response.close();
        
        return objectMapper.readValue(jsonContent, DailyReport.class);
    }

    /**
     * Genera la URL pública del archivo en GitLab
     */
    private String generateFileUrl(String filePath) {
        // Formato: https://gitlab.com/usuario/proyecto/-/blob/main/2026/01/archivo.json
        String encodedPath = filePath.replace(" ", "%20");
        return String.format("%s/api/v4/projects/%s/repository/files/%s/raw?ref=%s",
            baseUrl, projectId, java.net.URLEncoder.encode(filePath, java.nio.charset.StandardCharsets.UTF_8), branch);
    }

    /**
     * Lista todos los reportes disponibles en GitLab
     */
    public String listReports() throws IOException {
        String url = String.format("%s/api/v4/projects/%s/repository/tree?ref=%s&recursive=true",
            baseUrl, projectId, branch);
        
        Request request = new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", gitlabToken)
            .get()
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            throw new IOException("Failed to list files: " + response.code());
        }
        
        String responseBody = response.body().string();
        response.close();
        
        return responseBody;
    }

    /**
     * Obtiene información del proyecto
     */
    public String getProjectInfo() throws IOException {
        String url = String.format("%s/api/v4/projects/%s", baseUrl, projectId);
        
        Request request = new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", gitlabToken)
            .get()
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            throw new IOException("Failed to get project info: " + response.code());
        }
        
        String responseBody = response.body().string();
        response.close();
        
        JsonObject project = gson.fromJson(responseBody, JsonObject.class);
        
        return String.format("Project: %s\nURL: %s\nStorage: %s MB",
            project.get("name").getAsString(),
            project.get("web_url").getAsString(),
            project.getAsJsonObject("statistics").get("repository_size").getAsLong() / (1024 * 1024)
        );
    }

    /**
     * Verifica la conexión con GitLab
     */
    public boolean testConnection() {
        try {
            getProjectInfo();
            return true;
        } catch (Exception e) {
            log.error("GitLab connection test failed", e);
            return false;
        }
    }
}