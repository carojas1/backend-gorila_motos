package com.projectBackend.GMotors.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import okhttp3.*;
import java.io.IOException;
import java.util.UUID;
import okhttp3.MediaType;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket}")
    private String bucketName;

    private final OkHttpClient httpClient = new OkHttpClient();

    public String subirImagenUsuario(MultipartFile file) {
        return subirArchivo(file, "usuarios/perfil/");
    }

    public String subirImagenProducto(MultipartFile file) {
        return subirArchivo(file, "productos/imagen/");
    }

    public String subirImagenMoto(MultipartFile file) {
        return subirArchivo(file, "motos/perfil/");
    }

    private String subirArchivo(MultipartFile file, String carpeta) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("El archivo está vacío");
            }

            String nombreArchivo = generarNombreArchivo(file.getOriginalFilename());
            // carpeta ya termina en "/" → concatenar directo evita el doble slash que rompía la subida
            String ruta = carpeta + nombreArchivo;
            byte[] fileContent = file.getBytes();

            String uploadUrl = String.format(
                    "%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, ruta
            );

            String contentType = (file.getContentType() != null && !file.getContentType().isBlank())
                    ? file.getContentType() : "application/octet-stream";
            System.out.println("[SUPABASE] Subiendo a: " + uploadUrl);
            System.out.println("[SUPABASE] Bucket: " + bucketName + " | ContentType: " + contentType + " | Size: " + fileContent.length);
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(RequestBody.create(fileContent, MediaType.parse(contentType)))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                    System.out.println("[SUPABASE] ERROR " + response.code() + ": " + errorBody);

                    throw new RuntimeException("Error de Supabase " + response.code() + ": " + errorBody);
                }
                System.out.println("[SUPABASE] Upload OK → " + construirUrlPublica(ruta));

                return construirUrlPublica(ruta);
            }

        } catch (IOException e) {
            // ⚠️ ALERTA: Problema de conexión
            throw new RuntimeException(
                    "No se pudo conectar con Supabase. " +
                    "Por favor, contacte con un administrador. Detalle técnico: " + e.getMessage()
            );
        }
    }

    public void eliminarImagen(String urlImagen) {
        try {
            String ruta = extraerRutaDeUrl(urlImagen);

            String deleteUrl = String.format(
                    "%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, ruta
            );

            Request request = new Request.Builder()
                    .url(deleteUrl)
                    .delete()
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() && response.code() != 204) {
                    System.err.println(
                            "No se pudo eliminar la imagen. El servicio de Supabase podría estar apagado. " +
                            "Por favor contacte con un administrador. Código: " + response.code()
                    );
                }
            }

        } catch (IOException e) {
            System.err.println(
                    "Error al eliminar imagen. Posible problema de conexión con Supabase. " +
                    "Contacte con un administrador. Detalle: " + e.getMessage()
            );
        }
    }

    // ============== MÉTODOS PRIVADOS ==============

    private String generarNombreArchivo(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isEmpty()) {
            return UUID.randomUUID().toString() + ".jpg";
        }

        int lastDotIndex = nombreOriginal.lastIndexOf(".");
        String extension = lastDotIndex > 0 ? nombreOriginal.substring(lastDotIndex) : ".jpg";

        return UUID.randomUUID().toString() + extension;
    }

    private String construirUrlPublica(String ruta) {
        return String.format(
                "%s/storage/v1/object/public/%s/%s",
                supabaseUrl, bucketName, ruta
        );
    }

    private String extraerRutaDeUrl(String url) {
        try {
            String[] partes = url.split("/public/");
            if (partes.length > 1) {
                return partes[1].substring(bucketName.length() + 1);
            }
        } catch (Exception e) {
            System.err.println("Error al extraer ruta de URL: " + e.getMessage());
        }
        return url;
    }
}
