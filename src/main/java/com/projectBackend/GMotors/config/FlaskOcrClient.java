package com.projectBackend.GMotors.config;

import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FlaskOcrClient {

    private static final String FLASK_URL = "http://localhost:5000/api/ocr/placa";

    public String detectarPlaca(MultipartFile image) throws Exception {

    	System.out.println("-[OCR] Iniciando envío de imagen a Flask...");
        System.out.println("-[OCR] URL: " + FLASK_URL);
        System.out.println("-[OCR] Nombre archivo: " + image.getOriginalFilename());
        System.out.println("-[OCR] Tamaño (bytes): " + image.getSize());
        System.out.println("-[OCR] Content-Type: " + image.getContentType());

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("image", new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;

        try {
            response = restTemplate.postForEntity(
                    FLASK_URL,
                    request,
                    Map.class
            );
        } catch (Exception e) {
            System.out.println("❌ [OCR] Error al llamar a Flask");
            System.err.println("❌ [OCR] Mensaje: " + e.getMessage());
            System.err.println("❌ [OCR] Tipo: " + e.getClass().getName());
            e.printStackTrace();
            
            
         // Verificar si Flask está corriendo
            System.err.println("⚠️ [OCR] ¿Está Flask corriendo en " + FLASK_URL + "?");
            return null;
        }

        System.out.println("⬅️ [OCR] Status Flask: " + response.getStatusCode());
        System.out.println("⬅️ [OCR] Body Flask: " + response.getBody());

        if (response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null) {

            Object placa = response.getBody().get("placa");
            System.out.println("✅ [OCR] Placa recibida: " + placa);

            return placa != null ? placa.toString() : null;
        }

        System.out.println("⚠️ [OCR] Respuesta inválida de Flask");
        return null;
    }
}
