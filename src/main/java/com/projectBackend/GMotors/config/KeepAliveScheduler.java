package com.projectBackend.GMotors.config;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Motor anti-sueño: Render (plan free) apaga el servicio tras ~15 min sin tráfico
 * de entrada, y el arranque en frío tarda ~170s. Este componente hace un auto-ping
 * a su propia URL de salud cada 10 min, lo que cuenta como tráfico de entrada y
 * evita que Render lo duerma. Así el backend nunca se apaga ni demora en cargar.
 *
 * La URL se toma de RENDER_EXTERNAL_URL (Render la inyecta sola) con fallback a la
 * URL pública conocida. Si KEEPALIVE_ENABLED=false se desactiva (p.ej. en local).
 */
@Component
public class KeepAliveScheduler {

    @Value("${keepalive.enabled:true}")
    private boolean enabled;

    @Value("${RENDER_EXTERNAL_URL:https://backend-gorila-motos.onrender.com}")
    private String baseUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    /** Cada 10 min (600000 ms). El primer ping sale 10 min tras el arranque. */
    @Scheduled(fixedRate = 600_000L, initialDelay = 600_000L)
    public void ping() {
        if (!enabled) return;
        String url = baseUrl + "/actuator/health";
        Request req = new Request.Builder().url(url).get().build();
        try (Response r = client.newCall(req).execute()) {
            System.out.println("[KEEPALIVE] ping " + url + " -> " + r.code());
        } catch (Exception e) {
            System.out.println("[KEEPALIVE] error: " + e.getMessage());
        }
    }
}
