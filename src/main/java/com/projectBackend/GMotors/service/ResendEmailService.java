package com.projectBackend.GMotors.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.projectBackend.GMotors.model.DetalleFactura;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:noreply@gorilamoto.com}")
    private String fromEmail;

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    /* ── SMTP (Gmail) — bloqueado en Render free tier ── */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${mail.from-name:Gorila Motos}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESEND_URL     = "https://api.resend.com/emails";
    private static final String SENDGRID_URL   = "https://api.sendgrid.com/v3/mail/send";
    private static final String BREVO_URL      = "https://api.brevo.com/v3/smtp/email";

    /* ── Envío genérico ──
       Prioridad: 1) Brevo (sin dominio, HTTPS, 300/día gratis)
                  2) SendGrid (sin dominio, HTTPS, 100/día gratis)
                  3) Gmail SMTP (bloqueado en Render free tier)
                  4) Resend (requiere dominio propio verificado) */
    public boolean enviar(String to, String subject, String html) {
        String senderEmail = (mailUsername != null && !mailUsername.isBlank())
                ? mailUsername : "gorilamotos2026@gmail.com";

        /* 1) Brevo — HTTP API, sin dominio, 300 emails/día gratis */
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("sender", Map.of("email", senderEmail, "name", fromName));
                body.put("to", List.of(Map.of("email", to)));
                body.put("subject", subject);
                body.put("htmlContent", html);

                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> res = restTemplate.postForEntity(BREVO_URL, req, String.class);
                System.out.println("[EMAIL] (Brevo) Enviado a " + to + " — status: " + res.getStatusCode());
                return res.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                System.err.println("[EMAIL] (Brevo) Error a " + to + ": " + e.getMessage());
            }
        }

        /* 2) SendGrid — HTTP API, no necesita dominio, funciona en Render */
        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(sendgridApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))));
                body.put("from", Map.of("email", senderEmail, "name", fromName));
                body.put("subject", subject);
                body.put("content", List.of(Map.of("type", "text/html", "value", html)));

                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> res = restTemplate.postForEntity(SENDGRID_URL, req, String.class);
                System.out.println("[EMAIL] (SendGrid) Enviado a " + to + " — status: " + res.getStatusCode());
                return res.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                System.err.println("[EMAIL] (SendGrid) Error a " + to + ": " + e.getMessage());
                // cae al respaldo
            }
        }

        /* 2) Gmail SMTP (funciona en local, bloqueado en Render free) */
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(mailUsername, fromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(msg);
                System.out.println("[EMAIL] (SMTP/Gmail) Enviado a " + to);
                return true;
            } catch (Exception e) {
                System.err.println("[EMAIL] (SMTP) Error a " + to + ": " + e.getMessage());
            }
        }

        /* 3) Resend (requiere dominio verificado) */
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(resendApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("from", fromName + " <" + fromEmail + ">");
                body.put("to", to);
                body.put("subject", subject);
                body.put("html", html);

                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> res = restTemplate.postForEntity(RESEND_URL, req, String.class);
                System.out.println("[EMAIL] (Resend) Enviado a " + to + " — status: " + res.getStatusCode());
                return res.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                System.err.println("[EMAIL] (Resend) Error a " + to + ": " + e.getMessage());
            }
        }

        System.out.println("[EMAIL] Sin proveedor configurado — email no enviado a: " + to);
        return false;
    }

    /* ══════════════════════════════════════════════
       EMAIL 1 — Recuperación de contraseña
       ══════════════════════════════════════════════ */
    public boolean enviarEmailRecuperacion(String correo, String token, String nombre, String plataforma) {
        String enlace = "web".equalsIgnoreCase(plataforma)
            ? "https://pagina-web-gorila-motos.vercel.app/restablecer?token=" + token
            : "gmotors://reset-password?token=" + token;

        String html = htmlBase(
            "Recupera tu contraseña",
            "Hola <strong>" + nombre + "</strong>, recibimos una solicitud para restablecer tu contraseña.",
            "Restablecer contraseña",
            enlace,
            "Este enlace expira en 1 hora. Si no solicitaste esto, ignora este correo."
        );
        return enviar(correo, "Restablecer contraseña — Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 2 — Factura generada (notificación automática)
       ══════════════════════════════════════════════ */
    public boolean enviarFactura(String correoCliente, String nombreCliente,
                                  String placa, String tipoServicio,
                                  double costoTotal, String fecha,
                                  Long idRegistro) {
        return enviarFactura(correoCliente, nombreCliente, placa, tipoServicio, costoTotal, fecha, idRegistro, null);
    }

    public boolean enviarFactura(String correoCliente, String nombreCliente,
                                  String placa, String tipoServicio,
                                  double costoTotal, String fecha,
                                  Long idRegistro, List<DetalleFactura> detalles) {
        String html = htmlServicio(nombreCliente, placa, tipoServicio, costoTotal, fecha, idRegistro, detalles);
        String refAsunto = idRegistro != null ? String.format("ORD-%06d", idRegistro) : "";
        return enviar(correoCliente, "Comprobante de servicio " + refAsunto + " — Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 3 — Alerta de mantenimiento próximo (legacy)
       ══════════════════════════════════════════════ */
    public boolean enviarAlertaMantenimiento(String correoCliente, String nombreCliente,
                                              String placa, String marca, String modelo,
                                              int kmRestantes) {
        String html = htmlBase(
            "Tu moto necesita mantenimiento pronto",
            "Hola <strong>" + nombreCliente + "</strong>, tu moto <strong>" + marca + " " + modelo +
            "</strong> (placa <strong>" + placa + "</strong>) tiene programado un cambio de aceite en aproximadamente <strong>" +
            kmRestantes + " km</strong>.",
            "Ver detalles en el portal",
            "https://pagina-web-gorila-motos.vercel.app/mi-moto",
            "Agenda tu cita con anticipación para evitar daños en el motor."
        );
        return enviar(correoCliente, "Mantenimiento próximo — " + placa + " · Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 5 — Mantenimiento VENCIDO (umbral cruzado)
       ══════════════════════════════════════════════ */
    public boolean enviarAlertaMantenimientoVencido(String correo, String nombre,
                                                     String placa, String marca, String modelo,
                                                     String tipo, String descripcion, int kmUmbral) {
        String tipoLabel = tipoLabel(tipo);
        String html = htmlBase(
            "¡Tu moto necesita " + tipoLabel + "!",
            "Hola <strong>" + nombre + "</strong>, tu moto <strong>" + marca + " " + modelo +
            "</strong> (placa <strong>" + placa + "</strong>) ha alcanzado los <strong>" +
            String.format("%,d", kmUmbral) + " km</strong> — el límite recomendado para:<br><br>" +
            "<strong style='color:#E11428'>" + descripcion + "</strong><br><br>" +
            "Llevar tu moto al taller lo antes posible previene daños mayores y reduce costos.",
            "Agendar mantenimiento",
            "https://pagina-web-gorila-motos.vercel.app/mi-moto",
            "Recomendación técnica basada en el cilindraje y condiciones de rodadura ecuatorianas."
        );
        return enviar(correo, "⚠️ " + tipoLabel + " vencido — " + placa + " · Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 6 — Mantenimiento PRÓXIMO (se acerca el umbral)
       ══════════════════════════════════════════════ */
    public boolean enviarAlertaMantenimientoProximo(String correo, String nombre,
                                                     String placa, String marca, String modelo,
                                                     String tipo, String descripcion,
                                                     int kmRestantes, int proximoKm) {
        String tipoLabel = tipoLabel(tipo);
        String html = htmlBase(
            "Próximamente: " + tipoLabel,
            "Hola <strong>" + nombre + "</strong>, a tu moto <strong>" + marca + " " + modelo +
            "</strong> (placa <strong>" + placa + "</strong>) le quedan aproximadamente <strong>" +
            String.format("%,d", kmRestantes) + " km</strong> para necesitar:<br><br>" +
            "<strong>" + descripcion + "</strong><br><br>" +
            "El cambio estará vencido al alcanzar los <strong>" + String.format("%,d", proximoKm) + " km</strong>.",
            "Ver estado de mi moto",
            "https://pagina-web-gorila-motos.vercel.app/mi-moto",
            "Agenda con anticipación para evitar esperas y proteger tu motor."
        );
        return enviar(correo, "Próximo: " + tipoLabel + " — " + placa + " · Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 9 — RESUMEN de mantenimiento (UN SOLO correo con TODO)
       Reemplaza el envío de un correo por cada componente para no saturar
       la bandeja ni el límite del proveedor de correo.
       ══════════════════════════════════════════════ */
    /** Número de WhatsApp que recibe las citas (EN PRUEBA — cambiar al de Gorila Motos). */
    private static final String WHATSAPP_CITAS = "593980834367";

    public static class ItemMantenimiento {
        public final String tipo;
        public final String descripcion;
        public final int    porcentaje;   // desgaste 0-100+
        public final int    kmRestante;
        public final String estado;       // "VENCIDO" | "PROXIMO" | "OK"
        public ItemMantenimiento(String tipo, String descripcion, int porcentaje, int kmRestante, String estado) {
            this.tipo = tipo; this.descripcion = descripcion; this.porcentaje = porcentaje;
            this.kmRestante = kmRestante; this.estado = estado;
        }
    }

    /**
     * UN solo correo con el DETALLE TÉCNICO COMPLETO: cada componente con su % de
     * desgaste y su estado. Lo urgente arriba, el resto como referencia.
     * @param items TODOS los componentes (no solo los que se pasaron).
     */
    public boolean enviarResumenMantenimiento(String correo, String nombre, String placa,
                                              String marca, String modelo, int kmActual,
                                              java.util.List<ItemMantenimiento> items) {
        if (correo == null || correo.isBlank() || correo.endsWith("@gmotors.local")) return false;
        if (items == null || items.isEmpty()) return false;

        // Ordenar por desgaste descendente (lo más crítico primero)
        items = new java.util.ArrayList<>(items);
        items.sort((a, b) -> Integer.compare(b.porcentaje, a.porcentaje));

        long vencidos = items.stream().filter(i -> "VENCIDO".equals(i.estado)).count();
        long proximos = items.stream().filter(i -> "PROXIMO".equals(i.estado)).count();
        String acento = vencidos > 0 ? "#E11428" : (proximos > 0 ? "#F59E0B" : "#10B981");

        StringBuilder filas = new StringBuilder();
        for (ItemMantenimiento it : items) {
            boolean ven = "VENCIDO".equals(it.estado);
            boolean pro = "PROXIMO".equals(it.estado);
            String color = ven ? "#E11428" : pro ? "#F59E0B" : "#10B981";
            String estadoTxt = ven ? "CAMBIAR YA" : pro ? "PRÓXIMO" : "AL DÍA";
            int pct = Math.min(100, Math.max(0, it.porcentaje));
            String detalle = ven
                ? "Excedido"
                : "Faltan " + String.format("%,d", Math.max(0, it.kmRestante)) + " km";
            filas.append("<tr>")
                 // Componente
                 .append("<td style='padding:12px;border-bottom:1px solid #f3f3f3;font-size:13px;color:#111;font-weight:600;vertical-align:top'>")
                 .append(tipoLabel(it.tipo))
                 .append("<br><span style='font-size:10.5px;color:#9CA3AF;font-weight:400'>").append(detalle).append("</span>")
                 .append("</td>")
                 // Barra de % desgaste
                 .append("<td style='padding:12px;border-bottom:1px solid #f3f3f3;vertical-align:middle;width:46%'>")
                 .append("<table cellpadding='0' cellspacing='0' style='width:100%'><tr>")
                 .append("<td style='background:#EEF1F4;border-radius:99px;height:8px;padding:0'>")
                 .append("<table cellpadding='0' cellspacing='0' style='width:").append(pct).append("%;min-width:8px'><tr>")
                 .append("<td style='background:").append(color).append(";border-radius:99px;height:8px;font-size:1px;line-height:1px'>&nbsp;</td>")
                 .append("</tr></table></td>")
                 .append("<td style='padding-left:10px;width:38px;text-align:right;font-size:12px;font-weight:800;color:").append(color).append("'>").append(it.porcentaje).append("%</td>")
                 .append("</tr></table></td>")
                 // Estado
                 .append("<td style='padding:12px;border-bottom:1px solid #f3f3f3;text-align:right'>")
                 .append("<span style='font-size:10px;font-weight:800;color:").append(color)
                 .append(";background:").append(color).append("14;padding:4px 9px;border-radius:6px;white-space:nowrap'>")
                 .append(estadoTxt).append("</span></td></tr>");
        }

        String resumen = vencidos > 0
            ? vencidos + " componente(s) por cambiar" + (proximos > 0 ? " y " + proximos + " por vencer." : ".")
            : (proximos > 0 ? proximos + " componente(s) próximos a vencer." : "Todo en orden.");

        String waMsg = java.net.URLEncoder.encode(
            "¡Hola Gorila Motos! Quiero agendar una cita para mi moto " + marca + " " + modelo +
            " (placa " + placa + "). Kilometraje: " + String.format("%,d", kmActual) + " km.",
            java.nio.charset.StandardCharsets.UTF_8);
        String waLink = "https://wa.me/593980834367?text=" + waMsg;

        String html = "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>" +
               "body{margin:0;padding:0;background:#050505;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#fff}" +
               ".wrap{max-width:600px;margin:0 auto;background:#0A0A0E;border:1px solid #1A1A24;border-radius:12px;overflow:hidden;box-shadow:0 0 30px rgba(225,20,40,0.15)}" +
               ".header{background:linear-gradient(135deg, #0C0C10 0%, #15151D 100%);padding:30px 40px;border-bottom:1px solid rgba(225,20,40,0.2)}" +
               ".content{padding:40px}" +
               ".btn{display:inline-block;background:linear-gradient(90deg, #E11428 0%, #B90D1E 100%);color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:800;font-size:15px;text-transform:uppercase;letter-spacing:1px;box-shadow:0 4px 15px rgba(225,20,40,0.3)}" +
               ".neon-text{color:#E11428;text-shadow:0 0 10px rgba(225,20,40,0.4)}" +
               "</style></head><body>" +
               "<div style='padding:40px 16px'>" +
               "<div class='wrap'>" +
               "<div class='header'>" +
               "<h1 style='margin:0;font-size:28px;font-weight:900;letter-spacing:-1px'>GORILA <span class='neon-text'>MOTOS</span></h1>" +
               "<p style='margin:5px 0 0;color:#8B8FA8;font-size:12px;letter-spacing:3px;text-transform:uppercase'>Alerta de Mantenimiento</p>" +
               "</div>" +
               "<div class='content'>" +
               "<h2 style='margin:0 0 20px;font-size:22px;font-weight:800'>" + (vencidos > 0 ? "⚠️ " : "") + "Tu moto necesita mantenimiento" + "</h2>" +
               "<p style='color:#A1A5B5;font-size:16px;line-height:1.6;margin:0 0 25px'>" +
               "Saludos <strong>" + nombre + "</strong>, el sistema de diagnóstico ha detectado que tu <strong>" + marca + " " + modelo + "</strong> " +
               "(<span style='color:#fff;background:#1A1A24;padding:4px 8px;border-radius:4px;font-family:monospace'>" + placa + "</span>) " +
               "con <strong>" + String.format("%,d", kmActual) + " km</strong> requiere atención técnica.</p>" +
               "<div style='background:#111118;border:1px solid #1F1F2E;border-radius:10px;padding:20px;margin-bottom:30px'>" +
               "<p style='margin:0 0 15px;color:#EAEAEA;font-weight:700'>" + resumen + "</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse'>" +
               "<tr><th style='text-align:left;padding:10px;font-size:11px;color:#6C7086;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #1F1F2E'>Componente</th>" +
               "<th style='text-align:left;padding:10px;font-size:11px;color:#6C7086;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #1F1F2E'>Desgaste</th>" +
               "<th style='text-align:right;padding:10px;font-size:11px;color:#6C7086;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #1F1F2E'>Estado</th></tr>" +
               filas + "</table></div>" +
               "<div style='text-align:center;margin:40px 0 20px'>" +
               "<a href='" + waLink + "' class='btn'>Agendar Cita Ahora</a>" +
               "</div>" +
               "</div>" +
               "<div style='background:#08080C;padding:25px 40px;text-align:center;border-top:1px solid #15151D'>" +
               "<p style='margin:0;color:#666;font-size:12px'>© " + java.time.Year.now().getValue() + " Gorila Motos · Innovación en Movimiento</p>" +
               "<p style='margin:8px 0 0;font-size:11px'><a href='https://pagina-web-gorila-motos.vercel.app/' style='color:#E11428;text-decoration:none'>Visitar Portal</a></p>" +
               "</div></div></div></body></html>";

        String asunto = (vencidos > 0 ? "⚠️ " : "") + "Mantenimiento requerido: " + placa + " — Gorila Motos";
        return enviar(correo, asunto, html);
    }

    private String tipoLabel(String tipo) {
        return switch (tipo) {
            case "ACEITE"           -> "Cambio de aceite";
            case "FILTRO_AIRE"      -> "Cambio de filtro de aire";
            case "BUJIA"            -> "Cambio de bujía";
            case "CADENA"           -> "Revisión de cadena";
            case "LLANTA_TRASERA"   -> "Cambio de llanta trasera";
            case "FRENOS"           -> "Revisión de frenos";
            case "REVISION_GENERAL" -> "Revisión general";
            default                 -> tipo.replace("_", " ").toLowerCase();
        };
    }

    /* ══════════════════════════════════════════════
       EMAIL 7 — Comprobante de venta de inventario
       Diseño elegante estilo recibo profesional
       ══════════════════════════════════════════════ */
    public boolean enviarComprobanteInventario(String correoCliente, String nombreCliente,
                                               String nombreProducto, int cantidad,
                                               double pvp, double total, String fecha) {
        return enviarComprobanteInventario(correoCliente, nombreCliente, nombreProducto,
                null, cantidad, pvp, total, fecha, null);
    }

    public boolean enviarComprobanteInventario(String correoCliente, String nombreCliente,
                                               String nombreProducto, String codigoProducto,
                                               int cantidad, double pvp, double total,
                                               String fecha, String referencia) {
        String ref    = referencia != null ? referencia : ("GRM-" + java.time.LocalDate.now().toString().replace("-","").substring(2) + "-" + java.util.UUID.randomUUID().toString().substring(0,4).toUpperCase());
        String codigo = codigoProducto != null ? codigoProducto : "—";

        String html = htmlComprobante(nombreCliente, nombreProducto, codigo, cantidad, pvp, total, fecha, ref);
        return enviar(correoCliente, "Comprobante de compra #" + ref + " — Gorila Motos", html);
    }

    public boolean enviarComprobanteInventario(String correoCliente, String nombreCliente,
                                               java.util.List<java.util.Map<String, Object>> items,
                                               double total, String fecha, String referencia,
                                               java.util.Map<String, Object> cliente) {
        String ref = referencia != null ? referencia : ("GRM-" + java.time.LocalDate.now().toString().replace("-","").substring(2) + "-" + java.util.UUID.randomUUID().toString().substring(0,4).toUpperCase());
        String html = htmlComprobanteItems(nombreCliente, items, total, fecha, ref, cliente);
        return enviar(correoCliente, "Comprobante de compra #" + ref + " - Gorila Motos", html);
    }

    private String htmlComprobanteItems(String nombre, java.util.List<java.util.Map<String, Object>> items,
                                        double total, String fecha, String referencia,
                                        java.util.Map<String, Object> cliente) {
        StringBuilder filas = new StringBuilder();
        for (java.util.Map<String, Object> item : items) {
            String producto = esc(text(item, "nombreProducto", "nombre", "descripcion"));
            String codigo = esc(text(item, "codigoProducto", "codigo", "codigo_personal"));
            int cantidad = intVal(item.get("cantidad"), 1);
            double pvp = doubleVal(item.get("pvp"), doubleVal(item.get("precioUnitario"), 0));
            double subtotal = doubleVal(item.get("subtotal"), cantidad * pvp);
            filas.append("<tr style='background:#fff'>")
                 .append("<td style='padding:14px;font-family:Arial,sans-serif;font-size:14px;font-weight:600;color:#111'>")
                 .append(producto).append("<br><span style='font-size:11px;color:#9CA3AF;font-weight:400'>Ref: ").append(codigo).append("</span></td>")
                 .append("<td style='padding:14px;text-align:center;font-family:Arial,sans-serif;font-size:15px;font-weight:700;color:#374151'>").append(cantidad).append("</td>")
                 .append("<td style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:14px;color:#374151'>").append(String.format("$%.2f", pvp)).append("</td>")
                 .append("<td style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:15px;font-weight:800;color:#E11428'>").append(String.format("$%.2f", subtotal)).append("</td>")
                 .append("</tr>");
        }

        String datosCliente = "";
        if (cliente != null) {
            datosCliente = "<div style='margin-top:14px;background:#F8FAFC;border:1px solid #E5E7EB;border-radius:8px;padding:12px 14px;font-family:Arial,sans-serif'>" +
                    "<p style='margin:0 0 8px;font-size:10px;font-weight:800;letter-spacing:1.6px;text-transform:uppercase;color:#64748B'>Datos del cliente</p>" +
                    "<p style='margin:0;font-size:12px;color:#111;line-height:1.6'>" +
                    "<strong>Nombre:</strong> " + esc(text(cliente, "nombre", "nombreCliente")) + "<br>" +
                    "<strong>Cedula/RUC:</strong> " + esc(text(cliente, "cedula", "ruc", "identificacion")) + "<br>" +
                    "<strong>Telefono:</strong> " + esc(text(cliente, "telefono", "celular")) + "<br>" +
                    "<strong>Correo:</strong> " + esc(text(cliente, "correo", "email")) + "<br>" +
                    "<strong>Direccion:</strong> " + esc(text(cliente, "direccion")) +
                    "</p></div>";
        }

        String totalStr = String.format("$%.2f", total);
        int yr = java.time.Year.now().getValue();
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>body{margin:0;padding:0;background:#EAEDF2;font-family:Georgia,'Times New Roman',serif}.wrap{max-width:600px;margin:0 auto;background:#fff;border-radius:4px;overflow:hidden}</style></head><body>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px;background:#EAEDF2'><tr><td align='center'>" +
               "<table class='wrap' width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;box-shadow:0 2px 24px rgba(225,20,40,0.10)'>" +
               "<tr><td style='background:#E11428;height:5px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +
               "<tr><td style='background:#1E293B;padding:28px 36px'><table width='100%'><tr><td><p style='margin:0;color:#fff;font-size:22px;font-weight:700'>Gorila <span style='color:#E11428'>Motos</span></p><p style='margin:3px 0 0;color:rgba(255,255,255,0.45);font-size:10px;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>Taller Mecanico - Cuenca, Ecuador</p></td><td align='right'><p style='margin:0;color:#E11428;font-size:9px;font-weight:700;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>COMPROBANTE</p><p style='margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:13px;font-weight:700;font-family:\"Courier New\",monospace'>#" + referencia + "</p></td></tr></table></td></tr>" +
               "<tr><td style='padding:30px 36px 10px;border-bottom:1px solid #F1F1F4'><p style='margin:0 0 4px;font-size:22px;font-weight:700;color:#111'>Gracias por tu confianza</p><p style='margin:0;font-family:Arial,sans-serif;font-size:14px;color:#555;line-height:1.6'>Estimado/a <strong style='color:#111'>" + esc(nombre) + "</strong>, este es el detalle completo de tu compra.</p>" + datosCliente + "</td></tr>" +
               "<tr><td style='padding:24px 36px 0'><p style='margin:0 0 12px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#9CA3AF'>Detalle de la compra</p><table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid rgba(225,20,40,0.15)'><tr style='background:#FFF5F5'><th style='padding:10px 14px;text-align:left;font-family:Arial,sans-serif;font-size:10px;color:#9F1239'>Producto</th><th style='padding:10px 14px;text-align:center;font-family:Arial,sans-serif;font-size:10px;color:#9F1239'>Cant.</th><th style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;color:#9F1239'>P. Unit.</th><th style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;color:#9F1239'>Subtotal</th></tr>" + filas + "</table>" +
               "<table width='100%' style='margin-top:12px'><tr><td align='right' style='font-family:Arial,sans-serif;font-size:13px;font-weight:800;color:#E11428'>TOTAL&nbsp;&nbsp;<span style='font-size:22px;color:#9F1239'>" + totalStr + "</span></td></tr></table></td></tr>" +
               "<tr><td style='padding:20px 36px'><div style='background:#1E293B;border-radius:10px;padding:18px 24px;color:#fff'><table width='100%'><tr><td><p style='margin:0;font-family:Arial,sans-serif;font-size:10px;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.65)'>Total pagado</p><p style='margin:6px 0 0;font-size:30px;font-weight:700'>" + totalStr + "</p></td><td align='right'><p style='margin:0;font-family:Arial,sans-serif;font-size:11px;color:rgba(255,255,255,0.5)'>" + esc(fecha) + "</p><p style='margin:6px 0 0;font-family:\"Courier New\",monospace;font-size:11px;color:rgba(255,255,255,0.35)'>#" + referencia + "</p></td></tr></table></div></td></tr>" +
               "<tr><td style='background:#F8F9FA;border-top:1px solid #E5E7EB;padding:18px 36px'><p style='margin:0;font-family:Arial,sans-serif;font-size:10px;color:#9CA3AF'>© " + yr + " Gorila Motos S.A.S. - Cuenca, Ecuador</p></td></tr>" +
               "<tr><td style='background:#E11428;height:4px;font-size:1px;line-height:1px'>&nbsp;</td></tr></table></td></tr></table></body></html>";
    }

    private String text(java.util.Map<String, Object> data, String... keys) {
        if (data == null) return "—";
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return "—";
    }

    private int intVal(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try { return value == null ? fallback : Integer.parseInt(value.toString()); } catch (Exception e) { return fallback; }
    }

    private double doubleVal(Object value, double fallback) {
        if (value instanceof Number n) return n.doubleValue();
        try { return value == null ? fallback : Double.parseDouble(value.toString()); } catch (Exception e) { return fallback; }
    }

    private String esc(String value) {
        if (value == null) return "—";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String htmlComprobante(String nombre, String producto, String codigo,
                                    int cantidad, double pvp, double total,
                                    String fecha, String referencia) {
        String totalStr = String.format("$%.2f", total);
        String pvpStr   = String.format("$%.2f", pvp);
        int yr = java.time.Year.now().getValue();

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>" +
               "body{margin:0;padding:0;background:#EAEDF2;font-family:Georgia,'Times New Roman',serif}" +
               ".wrap{max-width:600px;margin:0 auto;background:#fff;border-radius:0}" +
               "@media only screen and (max-width:620px){" +
               ".outer{padding:12px 4px!important}" +
               ".hpad{padding:22px 20px!important}" +
               ".bpad{padding:24px 20px!important}" +
               ".tpad{padding:0 20px!important}" +
               ".fpad{padding:16px 20px!important}" +
               ".total-amount{font-size:26px!important}" +
               ".th-hide{display:none!important}" +
               ".td-collapse{display:block!important;text-align:left!important;width:100%!important}" +
               "}" +
               "</style>" +
               "</head><body>" +
               "<table class='outer' width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px;background:#EAEDF2'>" +
               "<tr><td align='center'>" +
               "<table class='wrap' width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 24px rgba(225,20,40,0.10)'>" +

               /* ── Banda superior ── */
               "<tr><td style='background:#E11428;height:5px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

               /* ── Header: logo + info empresa ── */
               "<tr><td class='hpad' style='background:#1E293B;padding:28px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='vertical-align:middle'>" +
               "<img src='https://backend-gorila-motos.onrender.com/images/gorila-logo.png' alt='Gorila Motos' width='54' height='54' style='display:block;border-radius:12px;border:2px solid rgba(225,20,40,0.1)' />" +
               "</td>" +
               "<td style='vertical-align:middle;padding-left:14px'>" +
               "<p style='margin:0;color:#fff;font-size:22px;font-weight:700;font-family:Georgia,serif;letter-spacing:-0.3px'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:3px 0 0;color:rgba(255,255,255,0.45);font-size:10px;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>Taller Mecánico · Cuenca, Ecuador</p>" +
               "</td>" +
               "<td align='right' style='vertical-align:middle'>" +
               "<div style='background:rgba(225,20,40,0.1);border:1px solid rgba(225,20,40,0.25);border-radius:8px;padding:10px 16px;text-align:center'>" +
               "<p style='margin:0;color:#E11428;font-size:9px;font-weight:700;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>COMPROBANTE</p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:13px;font-weight:700;font-family:\"Courier New\",monospace'>#" + referencia + "</p>" +
               "</div></td></tr></table></td></tr>" +

               /* ── Saludo ── */
               "<tr><td class='bpad' style='padding:30px 36px 10px;border-bottom:1px solid #F1F1F4'>" +
               "<p style='margin:0 0 4px;font-family:Georgia,serif;font-size:22px;font-weight:700;color:#111'>¡Gracias por tu confianza!</p>" +
               "<p style='margin:0;font-family:Arial,sans-serif;font-size:14px;color:#555;line-height:1.6'>Estimado/a <strong style='color:#111'>" + nombre + "</strong>, a continuación encontrará el detalle de su comprobante de venta emitido por <strong>Gorila Motos</strong>.</p>" +
               "</td></tr>" +

               /* ── Detalle ── */
               "<tr><td class='tpad' style='padding:24px 36px 0'>" +
               "<p style='margin:0 0 12px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#9CA3AF'>Detalle de la compra</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid rgba(225,20,40,0.15);border-radius:8px;overflow:hidden'>" +
               "<tr style='background:#FFF5F5'>" +
               "<th style='padding:10px 14px;text-align:left;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#9F1239;border-bottom:1px solid rgba(225,20,40,0.15)'>Producto / Descripción</th>" +
               "<th class='th-hide' style='padding:10px 14px;text-align:center;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#9F1239;border-bottom:1px solid rgba(225,20,40,0.15);width:55px'>Cant.</th>" +
               "<th class='th-hide' style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#9F1239;border-bottom:1px solid rgba(225,20,40,0.15);width:80px'>P. Unit.</th>" +
               "<th style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#9F1239;border-bottom:1px solid rgba(225,20,40,0.15);width:80px'>Subtotal</th>" +
               "</tr>" +
               "<tr style='background:#fff'>" +
               "<td style='padding:14px;font-family:Arial,sans-serif;font-size:14px;font-weight:600;color:#111'>" + producto + "<br><span style='font-size:11px;color:#9CA3AF;font-weight:400'>Ref: " + codigo + "</span></td>" +
               "<td class='th-hide' style='padding:14px;text-align:center;font-family:Arial,sans-serif;font-size:15px;font-weight:700;color:#374151'>" + cantidad + "</td>" +
               "<td class='th-hide' style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:14px;color:#374151'>" + pvpStr + "</td>" +
               "<td style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:15px;font-weight:800;color:#E11428'>" + totalStr + "</td>" +
               "</tr></table>" +

               /* ── Línea total ── */
               "<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:8px'>" +
               "<tr><td align='right'>" +
               "<table cellpadding='0' cellspacing='0'>" +
               "<tr><td style='padding:6px 14px;font-family:Arial,sans-serif;font-size:11px;color:#9CA3AF;text-align:right'>Subtotal</td>" +
               "<td style='padding:6px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;text-align:right'>" + totalStr + "</td></tr>" +
               "<tr style='border-top:2px solid #E11428'>" +
               "<td style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#E11428;text-align:right;letter-spacing:0.5px;text-transform:uppercase'>TOTAL</td>" +
               "<td style='padding:8px 14px;font-family:Georgia,serif;font-size:20px;font-weight:700;color:#9F1239;text-align:right'>" + totalStr + "</td></tr>" +
               "</table></td></tr></table>" +
               "</td></tr>" +

               /* ── Banda de total pagado ── */
               "<tr><td style='padding:20px 36px'>" +
               "<div style='background:linear-gradient(135deg, #9F1239 0%, #4C0519 100%);border-radius:10px;padding:18px 24px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.65)'>Total pagado</p>" +
               "<p class='total-amount' style='margin:6px 0 0;font-family:Georgia,serif;font-size:30px;font-weight:700;color:#fff;letter-spacing:-0.5px'>" + totalStr + "</p></td>" +
               "<td align='right'><p style='margin:0;font-family:Arial,sans-serif;font-size:11px;color:rgba(255,255,255,0.5)'>" + fecha + "</p>" +
               "<p style='margin:6px 0 0;font-family:\"Courier New\",monospace;font-size:11px;color:rgba(255,255,255,0.35)'>#" + referencia + "</p></td>" +
               "</tr></table></div></td></tr>" +

               /* ── Nota ── */
               "<tr><td style='padding:0 36px 28px'>" +
               "<div style='background:#FFFBF2;border:1px solid #FDE68A;border-radius:8px;padding:14px 18px'>" +
               "<p style='margin:0;font-family:Arial,sans-serif;font-size:12px;color:#78350F;line-height:1.65'>" +
               "Conserve este correo como comprobante oficial de su compra. Para consultas o garantías comuníquese con nosotros a " +
               "<a href='mailto:gorilamotos2026@gmail.com' style='color:#E11428;font-weight:700'>gorilamotos2026@gmail.com</a> " +
               "o visítenos en nuestro taller en Cuenca, Ecuador.</p></div></td></tr>" +

               /* ── Footer ── */
               "<tr><td class='fpad' style='background:#F8F9FA;border-top:1px solid #E5E7EB;padding:18px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-family:Arial,sans-serif;font-size:10px;color:#9CA3AF'>© " + yr + " Gorila Motos S.A.S. &nbsp;·&nbsp; Cuenca, Ecuador</p>" +
               "<p style='margin:3px 0 0;font-family:Arial,sans-serif;font-size:10px;color:#E11428'><a href='https://pagina-web-gorila-motos.vercel.app/' style='color:#E11428;text-decoration:none'>pagina-web-gorila-motos.vercel.app</a></p></td>" +
               "<td align='right'><img src='https://backend-gorila-motos.onrender.com/images/gorila-logo.png' alt='' width='36' height='36' style='border-radius:8px;border:1px solid rgba(0,0,0,0.1)' /></td>" +
               "</tr></table></td></tr>" +

               /* ── Banda inferior ── */
               "<tr><td style='background:#E11428;height:4px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

               "</table></td></tr></table>" +
               "</body></html>";
    }

    /* ══════════════════════════════════════════════
       EMAIL 8 — Reporte de diagnóstico mecánico
       Se envía automáticamente al guardar un diagnóstico.
       ══════════════════════════════════════════════ */
    public boolean enviarDiagnostico(String correo, String nombre,
                                     String placa, String marca, String modelo,
                                     int kmIngreso, String observaciones,
                                     java.util.List<com.projectBackend.GMotors.model.DetalleDiagnostico> detalles) {
        if (correo == null || correo.isBlank() || correo.endsWith("@gmotors.local")) return false;

        int malos    = 0, regulares = 0;
        StringBuilder filas = new StringBuilder();
        if (detalles != null) {
            for (var d : detalles) {
                int est = d.getEstado() == null ? 1 : d.getEstado();
                if (est >= 3) malos++;
                else if (est == 2) regulares++;
                if (est >= 2) filas.append(filaDiagnostico(d.getParte(), est, d.getObservacion()));
            }
        }

        boolean critico = malos > 0;
        String titulo = critico
            ? "Tu moto necesita atención"
            : (regulares > 0 ? "Revisión con observaciones" : "Tu moto está en buen estado");
        String acentoColor = critico ? "#E11428" : (regulares > 0 ? "#F59E0B" : "#10B981");
        String resumen = critico
            ? malos + " punto(s) en estado CRÍTICO" + (regulares > 0 ? " y " + regulares + " a vigilar." : ".")
            : (regulares > 0 ? regulares + " punto(s) a vigilar — nada crítico." : "Todos los puntos revisados están en buen estado. ¡Listo para rodar!");

        String tabla = filas.length() > 0
            ? "<table style='width:100%;border-collapse:collapse;margin:8px 0 4px'>" +
              "<tr><th style='text-align:left;padding:8px 10px;font-size:11px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #eee'>Componente</th>" +
              "<th style='text-align:left;padding:8px 10px;font-size:11px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #eee'>Estado</th></tr>" +
              filas + "</table>"
            : "<p style='margin:8px 0;color:#10B981;font-size:14px;font-weight:600'>✓ Sin observaciones — todo en orden.</p>";

        String cuerpo =
            "Hola <strong>" + nombre + "</strong>, realizamos el diagnóstico de tu moto <strong>" +
            marca + " " + modelo + "</strong> (placa <strong>" + placa + "</strong>) con " +
            String.format("%,d", kmIngreso) + " km.<br><br>" +
            "<span style='color:" + acentoColor + ";font-weight:800'>" + resumen + "</span>" +
            (observaciones != null && !observaciones.isBlank()
                ? "<br><br><em style='color:#666'>Nota del mecánico: " + observaciones + "</em>" : "") +
            "<br>" + tabla;

        String html = htmlBase(
            (critico ? "⚠️ " : "") + titulo,
            cuerpo,
            "Agendar servicio",
            "https://pagina-web-gorila-motos.vercel.app/mi-moto",
            "Diagnóstico realizado por un técnico de Gorila Motos. Te recomendamos atender los puntos marcados en rojo lo antes posible."
        );
        String asunto = critico
            ? "⚠️ Diagnóstico: tu moto " + placa + " necesita atención — Gorila Motos"
            : "Diagnóstico de tu moto " + placa + " — Gorila Motos";
        return enviar(correo, asunto, html);
    }

    /** Mapa parte → severidad + acción recomendada */
    private String filaDiagnostico(String parte, int estado, String obs) {
        boolean malo = estado >= 3;
        String estLabel = malo ? "MALO — requiere cambio/reparación" : "REGULAR — vigilar";
        String color    = malo ? "#E11428" : "#F59E0B";
        String bg       = malo ? "rgba(225,20,40,0.08)" : "rgba(245,158,11,0.08)";
        String parteLabel = parteDiagnosticoLabel(parte);
        String obsTxt = (obs != null && !obs.isBlank()) ? "<br><span style='font-size:11px;color:#9CA3AF'>" + obs + "</span>" : "";
        return "<tr>" +
               "<td style='padding:10px;border-bottom:1px solid #f3f3f3;font-size:13px;color:#111;font-weight:600'>" + parteLabel + obsTxt + "</td>" +
               "<td style='padding:10px;border-bottom:1px solid #f3f3f3'>" +
               "<span style='font-size:11px;font-weight:700;color:" + color + ";background:" + bg + ";padding:3px 8px;border-radius:6px'>" + estLabel + "</span>" +
               "</td></tr>";
    }

    private String parteDiagnosticoLabel(String parte) {
        if (parte == null) return "Componente";
        return switch (parte.toUpperCase()) {
            case "MOTOR"        -> "Motor";
            case "TRANSMISION"  -> "Transmisión / embrague";
            case "FRENOS"       -> "Frenos";
            case "LLANTAS"      -> "Llantas";
            case "SUSPENSION"   -> "Suspensión";
            case "ELECTRICO"    -> "Sistema eléctrico";
            case "CARROCERIA"   -> "Carrocería";
            case "REFRIGERACION"-> "Refrigeración";
            default             -> parte;
        };
    }

    /* ══════════════════════════════════════════════
       EMAIL 10 — Oferta / campaña de marketing masiva
       Enviada por el ADMIN desde la pantalla de Clientes
       ══════════════════════════════════════════════ */
    public boolean enviarOfertaMarketing(String correo, String asunto, String mensaje) {
        String formattedMensaje = mensaje != null ? mensaje.replace("\n", "<br>") : "";
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
            "margin:0;padding:0;background:#F4F4F5;font-family:Arial,sans-serif'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px'>" +
            "<table width='560' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;" +
            "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)'>" +
            // Banda roja
            "<tr><td style='background:#E11428;height:5px;font-size:1px'>&nbsp;</td></tr>" +
            // Header
            "<tr><td style='background:#0C0C10;padding:24px 36px'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td><p style='margin:0;color:#fff;font-size:22px;font-weight:900;letter-spacing:-0.5px'>" +
            "Gorila <span style='color:#E11428'>Motos</span></p>" +
            "<p style='margin:4px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:2px;text-transform:uppercase'>Taller Mecánico · Cuenca, Ecuador</p>" +
            "</td>" +
            "<td align='right'><span style='background:rgba(225,20,40,0.15);color:#E11428;font-size:9px;font-weight:800;" +
            "letter-spacing:2px;text-transform:uppercase;padding:6px 12px;border-radius:8px;border:1px solid rgba(225,20,40,0.3)'>Oferta exclusiva</span></td>" +
            "</tr></table></td></tr>" +
            // Título
            "<tr><td style='padding:32px 36px 8px'>" +
            "<h2 style='margin:0 0 16px;color:#111;font-size:22px;font-weight:900;letter-spacing:-0.3px'>" + asunto + "</h2>" +
            "</td></tr>" +
            // Contenido del mensaje
            "<tr><td style='padding:0 36px 28px'>" +
            "<div style='color:#444;font-size:15px;line-height:1.7'>" + formattedMensaje + "</div>" +
            "</td></tr>" +
            // CTA
            "<tr><td style='padding:0 36px 32px'>" +
            "<a href='https://pagina-web-gorila-motos.vercel.app' style='display:inline-block;background:#E11428;color:#fff;" +
            "text-decoration:none;padding:13px 28px;border-radius:10px;font-weight:700;font-size:14px'>Ver portal Gorila Motos →</a>" +
            "</td></tr>" +
            // Divider
            "<tr><td style='height:1px;background:#F0F0F2'></td></tr>" +
            // Footer
            "<tr><td style='background:#F9F9FB;padding:20px 36px'>" +
            "<p style='margin:0;color:#aaa;font-size:11px'>© " + java.time.Year.now().getValue() +
            " Gorila Motos · Cuenca, Ecuador · " +
            "<a href='https://pagina-web-gorila-motos.vercel.app' style='color:#E11428;text-decoration:none'>gorila-motos.vercel.app</a></p>" +
            "<p style='margin:6px 0 0;color:#ccc;font-size:10px'>Para dejar de recibir estas comunicaciones, escríbenos a " +
            "<a href='mailto:gorilamotos2026@gmail.com' style='color:#E11428'>gorilamotos2026@gmail.com</a></p>" +
            "</td></tr>" +
            "<tr><td style='background:#E11428;height:4px;font-size:1px'>&nbsp;</td></tr>" +
            "</table></td></tr></table></body></html>";
        return enviar(correo, asunto + " — Gorila Motos", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL TEST — Diagnóstico de SMTP (endpoint público /api/health/email-test)
       ══════════════════════════════════════════════ */
    public boolean enviarEmailPrueba(String destino) {
        String html = htmlBase(
            "Prueba de SMTP — Gorila Motos",
            "Este correo confirma que el servidor de correo <strong>está funcionando correctamente</strong> desde Render.",
            "Ver portal",
            "https://pagina-web-gorila-motos.vercel.app",
            "Si recibiste este mensaje, el SMTP de Gmail está activo y configurado."
        );
        return enviar(destino, "✓ Prueba SMTP — Gorila Motos (" + java.time.LocalDateTime.now() + ")", html);
    }

    /* ══════════════════════════════════════════════
       EMAIL 4 — Bienvenida al registrarse
       ══════════════════════════════════════════════ */
    public boolean enviarBienvenida(String correo, String nombre) {
        String html = htmlBase(
            "¡Bienvenido a Gorila Motos!",
            "Hola <strong>" + nombre + "</strong>, tu cuenta ha sido creada exitosamente. " +
            "Ya puedes registrar tus motos, ver tu historial de servicios y acumular puntos de fidelidad.",
            "Ir al portal",
            "https://pagina-web-gorila-motos.vercel.app",
            "Ante cualquier consulta, escríbenos a gorilamotos2026@gmail.com"
        );
        return enviar(correo, "Bienvenido a Gorila Motos 🏍️", html);
    }

    /* ══════════════════════════════════════════════
       PLANTILLAS HTML
       ══════════════════════════════════════════════ */
    private String htmlBase(String titulo, String cuerpo, String btnLabel, String btnUrl, String nota) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
               "margin:0;padding:0;background:#F4F4F5;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px'>" +
               "<table width='560' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;" +
               "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)'>" +
               // Header
               "<tr><td style='background:#0C0C10;padding:28px 40px'>" +
               "<p style='margin:0;color:#fff;font-size:22px;font-weight:900;letter-spacing:-0.5px'>" +
               "Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.4);font-size:11px;letter-spacing:2px;text-transform:uppercase'>Sistema de gestión · Ecuador</p>" +
               "</td></tr>" +
               // Body
               "<tr><td style='padding:36px 40px'>" +
               "<h2 style='margin:0 0 16px;color:#111;font-size:20px'>" + titulo + "</h2>" +
               "<p style='margin:0 0 24px;color:#555;font-size:15px;line-height:1.6'>" + cuerpo + "</p>" +
               "<a href='" + btnUrl + "' style='display:inline-block;background:#E11428;color:#fff;text-decoration:none;" +
               "padding:12px 28px;border-radius:10px;font-weight:700;font-size:14px'>" + btnLabel + "</a>" +
               (nota != null ? "<p style='margin:24px 0 0;color:#888;font-size:12px;line-height:1.5'>" + nota + "</p>" : "") +
               "</td></tr>" +
               // Footer
               "<tr><td style='background:#F9F9FB;padding:20px 40px;border-top:1px solid #eee'>" +
               "<p style='margin:0;color:#aaa;font-size:11px'>© " + java.time.Year.now().getValue() +
               " Gorila Motos · Cuenca, Ecuador · <a href='https://pagina-web-gorila-motos.vercel.app' style='color:#E11428'>gorila-motos.vercel.app</a></p>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private String htmlServicio(String nombre, String placa, String tipoServicio,
                                double costoTotal, String fecha, Long idRegistro,
                                List<DetalleFactura> detalles) {
        String costoStr = String.format("$%.2f", costoTotal);
        String refNum   = idRegistro != null ? String.format("ORD-%06d", idRegistro) : "ORD";
        int yr = java.time.Year.now().getValue();
        String LOGO = "https://backend-gorila-motos.onrender.com/images/gorila-logo.png";

        StringBuilder filasHtml = new StringBuilder();
        if (detalles != null && !detalles.isEmpty()) {
            List<DetalleFactura> manoList = new java.util.ArrayList<>();
            List<DetalleFactura> repList  = new java.util.ArrayList<>();
            for (DetalleFactura d : detalles) {
                boolean esRepuesto = d.getId_producto() != null;
                if (!esRepuesto && d.getDescripcion() != null) {
                    String desc = d.getDescripcion().toUpperCase();
                    if (desc.startsWith("[REP")) esRepuesto = true;
                }
                if (esRepuesto) repList.add(d); else manoList.add(d);
            }
            BigDecimal subtotalMano = BigDecimal.ZERO;
            BigDecimal subtotalRep  = BigDecimal.ZERO;
            
            filasHtml.append("<tr><td style='padding:16px 14px 6px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#6B7280' colspan='4'>Desglose del servicio</td></tr>");
            filasHtml.append("<tr style='background:#F9FAFB'>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#4B5563;border-bottom:1px solid #E5E7EB'>Concepto</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#4B5563;border-bottom:1px solid #E5E7EB;text-align:center'>Cant.</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#4B5563;border-bottom:1px solid #E5E7EB;text-align:right'>Subtotal</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#4B5563;border-bottom:1px solid #E5E7EB;text-align:center'>Tipo</td>" +
                "</tr>");

            if (!manoList.isEmpty()) {
                filasHtml.append("<tr><td colspan='4' style='padding:8px 14px 4px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#E11428;background:rgba(225,20,40,0.03)'>— Mano de obra</td></tr>");
                for (DetalleFactura d : manoList) {
                    String desc = d.getDescripcion() != null ? d.getDescripcion().replaceAll("^\\[MANO\\]\\s*", "") : "Servicio";
                    String sub  = d.getSubtotal() != null ? String.format("$%.2f", d.getSubtotal()) : "$0.00";
                    subtotalMano = subtotalMano.add(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO);
                    filasHtml.append("<tr style='border-bottom:1px solid #F3F4F6'>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#1F2937'>" + desc + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#374151;text-align:center'>" + (d.getCantidad() != null ? d.getCantidad() : 1) + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:600;color:#111827;text-align:right'>" + sub + "</td>" +
                        "<td style='padding:10px 14px;text-align:center'><span style='font-family:Arial,sans-serif;font-size:10px;font-weight:700;color:#E11428;background:rgba(225,20,40,0.1);border-radius:4px;padding:2px 7px'>MO</span></td>" +
                        "</tr>");
                }
                filasHtml.append("<tr style='background:rgba(225,20,40,0.03)'><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#E11428'>Subtotal mano de obra</td><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:700;color:#E11428;text-align:right'>" + String.format("$%.2f", subtotalMano) + "</td></tr>");
            }
            if (!repList.isEmpty()) {
                filasHtml.append("<tr><td colspan='4' style='padding:8px 14px 4px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#D97706;background:rgba(245,158,11,0.05)'>— Repuestos / Inventario</td></tr>");
                for (DetalleFactura d : repList) {
                    String desc = d.getDescripcion() != null ? d.getDescripcion().replaceAll("^\\[REP\\|[^\\]]+\\]\\s*", "") : "Repuesto";
                    String sub  = d.getSubtotal() != null ? String.format("$%.2f", d.getSubtotal()) : "$0.00";
                    subtotalRep = subtotalRep.add(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO);
                    filasHtml.append("<tr style='border-bottom:1px solid #F3F4F6'>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#1F2937'>" + desc + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#374151;text-align:center'>" + (d.getCantidad() != null ? d.getCantidad() : 1) + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:600;color:#111827;text-align:right'>" + sub + "</td>" +
                        "<td style='padding:10px 14px;text-align:center'><span style='font-family:Arial,sans-serif;font-size:10px;font-weight:700;color:#D97706;background:rgba(245,158,11,0.1);border-radius:4px;padding:2px 7px'>REP</span></td>" +
                        "</tr>");
                }
                filasHtml.append("<tr style='background:rgba(245,158,11,0.05)'><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#D97706'>Subtotal repuestos</td><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:700;color:#D97706;text-align:right'>" + String.format("$%.2f", subtotalRep) + "</td></tr>");
            }
        }
        boolean tieneDetalles = detalles != null && !detalles.isEmpty();

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>" +
               "body{margin:0;padding:0;background:#EAEDF2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif}" +
               "@media only screen and (max-width:620px){" +
               ".outer{padding:12px 4px!important}.hpad{padding:22px 20px!important}" +
               ".bpad{padding:24px 20px!important}.tpad{padding:0 20px!important}" +
               ".fpad{padding:16px 20px!important}.total-amount{font-size:26px!important}" +
               "}" +
               "</style>" +
               "</head><body>" +
               "<table class='outer' width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px;background:#EAEDF2'>" +
               "<tr><td align='center'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #E5E7EB;box-shadow:0 10px 30px rgba(225,20,40,0.08)'>" +
               /* Banda superior */
               "<tr><td style='background:#E11428;height:5px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +
               /* Header con logo */
               "<tr><td class='hpad' style='background:#1E293B;padding:32px 36px 24px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='vertical-align:middle'>" +
               "<img src='" + LOGO + "' alt='Gorila Motos' width='56' height='56' style='display:block;border-radius:12px;border:2px solid rgba(225,20,40,0.1)' />" +
               "</td>" +
               "<td style='vertical-align:middle;padding-left:16px'>" +
               "<p style='margin:0;color:#fff;font-size:24px;font-weight:900;letter-spacing:-0.5px'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.45);font-size:11px;letter-spacing:2px;text-transform:uppercase'>Taller Mecánico</p>" +
               "</td>" +
               "<td align='right' style='vertical-align:middle'>" +
               "<div style='background:rgba(225,20,40,0.1);border:1px solid rgba(225,20,40,0.25);border-radius:8px;padding:8px 12px;text-align:center'>" +
               "<p style='margin:0;color:#E11428;font-size:9px;font-weight:800;letter-spacing:2px;text-transform:uppercase'>COMPROBANTE</p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:14px;font-weight:700;font-family:\"Courier New\",monospace'>" + refNum + "</p>" +
               "</div></td></tr></table></td></tr>" +
               /* Saludo y Detalles */
               "<tr><td class='bpad' style='padding:24px 36px'>" +
               "<p style='margin:0 0 8px;font-size:22px;font-weight:800;color:#111'>Comprobante de Servicio</p>" +
               "<p style='margin:0 0 24px;font-size:14px;color:#4B5563;line-height:1.6'>Estimado/a <strong style='color:#111'>" + nombre + "</strong>, aquí tienes el detalle de los servicios realizados a tu vehículo.</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F8FAFC;border-radius:8px;border:1px solid rgba(225,20,40,0.15)'>" +
               "<tr>" +
               "<td style='padding:16px;border-bottom:1px solid #E2E8F0;width:50%'><p style='margin:0 0 4px;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#64748B'>Placa</p><p style='margin:0;font-size:15px;font-weight:700;color:#0F172A;font-family:\"Courier New\",monospace'>" + placa + "</p></td>" +
               "<td style='padding:16px;border-bottom:1px solid #E2E8F0;border-left:1px solid #E2E8F0;width:50%'><p style='margin:0 0 4px;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#64748B'>Fecha</p><p style='margin:0;font-size:14px;color:#334155'>" + fecha + "</p></td>" +
               "</tr><tr>" +
               "<td colspan='2' style='padding:16px'><p style='margin:0 0 4px;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#64748B'>Servicio Realizado</p><p style='margin:0;font-size:15px;font-weight:600;color:#0F172A'>" + tipoServicio + "</p></td>" +
               "</tr></table></td></tr>" +
               /* Tabla de items */
               (tieneDetalles ? ("<tr><td style='padding:0 36px 24px'><table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid #E5E7EB;border-radius:8px;overflow:hidden'>" + filasHtml.toString() + "</table></td></tr>") : "") +
               /* Total */
               "<tr><td style='padding:0 36px 32px'>" +
               "<div style='background:linear-gradient(135deg, #9F1239 0%, #4C0519 100%);border-radius:12px;padding:24px;border:1px solid rgba(225,20,40,0.2)'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-size:11px;font-weight:800;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.7)'>Total Abonado</p>" +
               "<p class='total-amount' style='margin:4px 0 0;font-size:36px;font-weight:900;color:#fff;letter-spacing:-1px'>" + costoStr + "</p></td>" +
               "<td align='right' style='vertical-align:bottom'><p style='margin:0;font-size:12px;color:rgba(255,255,255,0.8);font-weight:600'>¡Gracias por elegirnos!</p></td>" +
               "</tr></table></div></td></tr>" +
               /* Footer */
               "<tr><td class='fpad' style='background:#F8F9FA;padding:24px 36px;border-top:1px solid #E5E7EB'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-size:11px;color:#6B7280'>© " + yr + " Gorila Motos. Cuenca, Ecuador.</p>" +
               "<p style='margin:4px 0 0;font-size:11px'><a href='https://pagina-web-gorila-motos.vercel.app/' style='color:#E11428;text-decoration:none'>pagina-web-gorila-motos.vercel.app</a></p></td>" +
               "<td align='right'><img src='" + LOGO + "' alt='' width='32' height='32' style='border-radius:8px;opacity:0.5' /></td>" +
               "</tr></table></td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private String fila(String label, String value) {
        return "<tr><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#666;font-size:13px;width:40%'>" +
               label + "</td><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#111;font-size:14px;font-weight:600'>" +
               value + "</td></tr>";
    }
}
