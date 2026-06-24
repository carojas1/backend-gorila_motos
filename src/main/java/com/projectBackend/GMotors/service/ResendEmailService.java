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
    private boolean enviar(String to, String subject, String html) {
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
            ? "https://gorila-motos.vercel.app/restablecer?token=" + token
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
            "https://gorila-motos.vercel.app/mi-moto",
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
            "https://gorila-motos.vercel.app/mi-moto",
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
            "https://gorila-motos.vercel.app/mi-moto",
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
    private static final String WHATSAPP_CITAS = "593989443292";

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

        String cuerpo =
            "Hola <strong>" + nombre + "</strong>, este es el estado técnico de tu moto <strong>" + marca + " " + modelo +
            "</strong> (placa <strong>" + placa + "</strong>) con <strong>" + String.format("%,d", kmActual) +
            " km</strong>:<br><br>" +
            "<span style='color:" + acento + ";font-weight:800;font-size:15px'>" + resumen + "</span>" +
            "<table style='width:100%;border-collapse:collapse;margin:12px 0 4px'>" +
            "<tr><th style='text-align:left;padding:8px 12px;font-size:10px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #eee'>Componente</th>" +
            "<th style='text-align:left;padding:8px 12px;font-size:10px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #eee'>Desgaste</th>" +
            "<th style='text-align:right;padding:8px 12px;font-size:10px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #eee'>Estado</th></tr>" +
            filas + "</table>";

        // Botón "Agendar cita" → WhatsApp con mensaje prellenado
        String waMsg = java.net.URLEncoder.encode(
            "¡Hola Gorila Motos! Quiero agendar una cita para mi moto " + marca + " " + modelo +
            " (placa " + placa + "). Kilometraje: " + String.format("%,d", kmActual) + " km.",
            java.nio.charset.StandardCharsets.UTF_8);
        String waLink = "https://wa.me/" + WHATSAPP_CITAS + "?text=" + waMsg;

        String html = htmlBase(
            (vencidos > 0 ? "⚠️ " : "") + "Tu moto necesita mantenimiento",
            cuerpo,
            "Agendar cita por WhatsApp",
            waLink,
            "Agenda tu cita pronto para evitar daños mayores y costos más altos. — Gorila Motos"
        );
        String asunto = (vencidos > 0 ? "⚠️ " : "") + "Mantenimiento de tu moto " + placa + " — Gorila Motos";
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
        String ref    = referencia != null ? referencia : ("GRM-" + java.time.LocalDate.now().toString().replace("-","").substring(2));
        String codigo = codigoProducto != null ? codigoProducto : "—";

        String html = htmlComprobante(nombreCliente, nombreProducto, codigo, cantidad, pvp, total, fecha, ref);
        return enviar(correoCliente, "Comprobante de compra #" + ref + " — Gorila Motos", html);
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
               "<table class='wrap' width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.10)'>" +

               /* ── Banda roja superior ── */
               "<tr><td style='background:#C8001A;height:5px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

               /* ── Header: logo + info empresa ── */
               "<tr><td class='hpad' style='background:#0D0D12;padding:28px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='vertical-align:middle'>" +
               "<img src='https://gorila-motos.vercel.app/brand/gorila-logo.png' alt='Gorila Motos' width='54' height='54' style='display:block;border-radius:12px;border:2px solid rgba(200,0,26,0.5)' />" +
               "</td>" +
               "<td style='vertical-align:middle;padding-left:14px'>" +
               "<p style='margin:0;color:#fff;font-size:22px;font-weight:700;font-family:Georgia,serif;letter-spacing:-0.3px'>Gorila <span style='color:#E8192C'>Motos</span></p>" +
               "<p style='margin:3px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>Taller Mecánico · Cuenca, Ecuador</p>" +
               "</td>" +
               "<td align='right' style='vertical-align:middle'>" +
               "<div style='background:rgba(200,0,26,0.12);border:1px solid rgba(200,0,26,0.4);border-radius:8px;padding:10px 16px;text-align:center'>" +
               "<p style='margin:0;color:#E8192C;font-size:9px;font-weight:700;letter-spacing:2.5px;text-transform:uppercase;font-family:Arial,sans-serif'>COMPROBANTE</p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;font-weight:700;font-family:\"Courier New\",monospace'>#" + referencia + "</p>" +
               "</div></td></tr></table></td></tr>" +

               /* ── Saludo ── */
               "<tr><td class='bpad' style='padding:30px 36px 10px;border-bottom:1px solid #F1F1F4'>" +
               "<p style='margin:0 0 4px;font-family:Georgia,serif;font-size:22px;font-weight:700;color:#111'>¡Gracias por tu confianza!</p>" +
               "<p style='margin:0;font-family:Arial,sans-serif;font-size:14px;color:#555;line-height:1.6'>Estimado/a <strong style='color:#111'>" + nombre + "</strong>, a continuación encontrará el detalle de su comprobante de venta emitido por <strong>Gorila Motos</strong>.</p>" +
               "</td></tr>" +

               /* ── Detalle ── */
               "<tr><td class='tpad' style='padding:24px 36px 0'>" +
               "<p style='margin:0 0 12px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#9CA3AF'>Detalle de la compra</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid #E5E7EB;border-radius:8px;overflow:hidden'>" +
               "<tr style='background:#F8F9FA'>" +
               "<th style='padding:10px 14px;text-align:left;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#6B7280;border-bottom:1px solid #E5E7EB'>Producto / Descripción</th>" +
               "<th class='th-hide' style='padding:10px 14px;text-align:center;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#6B7280;border-bottom:1px solid #E5E7EB;width:55px'>Cant.</th>" +
               "<th class='th-hide' style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#6B7280;border-bottom:1px solid #E5E7EB;width:80px'>P. Unit.</th>" +
               "<th style='padding:10px 14px;text-align:right;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#6B7280;border-bottom:1px solid #E5E7EB;width:80px'>Subtotal</th>" +
               "</tr>" +
               "<tr style='background:#fff'>" +
               "<td style='padding:14px;font-family:Arial,sans-serif;font-size:14px;font-weight:600;color:#111'>" + producto + "<br><span style='font-size:11px;color:#9CA3AF;font-weight:400'>Ref: " + codigo + "</span></td>" +
               "<td class='th-hide' style='padding:14px;text-align:center;font-family:Arial,sans-serif;font-size:15px;font-weight:700;color:#374151'>" + cantidad + "</td>" +
               "<td class='th-hide' style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:14px;color:#374151'>" + pvpStr + "</td>" +
               "<td style='padding:14px;text-align:right;font-family:Arial,sans-serif;font-size:15px;font-weight:800;color:#111'>" + totalStr + "</td>" +
               "</tr></table>" +

               /* ── Línea total ── */
               "<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:8px'>" +
               "<tr><td align='right'>" +
               "<table cellpadding='0' cellspacing='0'>" +
               "<tr><td style='padding:6px 14px;font-family:Arial,sans-serif;font-size:11px;color:#9CA3AF;text-align:right'>Subtotal</td>" +
               "<td style='padding:6px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;text-align:right'>" + totalStr + "</td></tr>" +
               "<tr style='border-top:2px solid #C8001A'>" +
               "<td style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#111;text-align:right;letter-spacing:0.5px;text-transform:uppercase'>TOTAL</td>" +
               "<td style='padding:8px 14px;font-family:Georgia,serif;font-size:20px;font-weight:700;color:#C8001A;text-align:right'>" + totalStr + "</td></tr>" +
               "</table></td></tr></table>" +
               "</td></tr>" +

               /* ── Banda de total pagado ── */
               "<tr><td style='padding:20px 36px'>" +
               "<div style='background:linear-gradient(135deg,#C8001A 0%,#8B0000 100%);border-radius:10px;padding:18px 24px'>" +
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
               "<a href='mailto:gorilamotos2026@gmail.com' style='color:#C8001A;font-weight:700'>gorilamotos2026@gmail.com</a> " +
               "o visítenos en nuestro taller en Cuenca, Ecuador.</p></div></td></tr>" +

               /* ── Footer ── */
               "<tr><td class='fpad' style='background:#F8F9FA;border-top:1px solid #E5E7EB;padding:18px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-family:Arial,sans-serif;font-size:10px;color:#9CA3AF'>© " + yr + " Gorila Motos S.A.S. &nbsp;·&nbsp; Cuenca, Ecuador</p>" +
               "<p style='margin:3px 0 0;font-family:Arial,sans-serif;font-size:10px;color:#C8001A'><a href='https://gorila-motos.vercel.app' style='color:#C8001A;text-decoration:none'>gorila-motos.vercel.app</a></p></td>" +
               "<td align='right'><img src='https://gorila-motos.vercel.app/brand/gorila-logo.png' alt='' width='36' height='36' style='border-radius:8px;border:1px solid rgba(0,0,0,0.1)' /></td>" +
               "</tr></table></td></tr>" +

               /* ── Banda roja inferior ── */
               "<tr><td style='background:#C8001A;height:4px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

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
            "https://gorila-motos.vercel.app/mi-moto",
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
    public boolean enviarOfertaMarketing(String correo, String asunto, String mensajeHtml) {
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
            "<div style='color:#444;font-size:15px;line-height:1.7'>" + mensajeHtml + "</div>" +
            "</td></tr>" +
            // CTA
            "<tr><td style='padding:0 36px 32px'>" +
            "<a href='https://gorila-motos.vercel.app' style='display:inline-block;background:#E11428;color:#fff;" +
            "text-decoration:none;padding:13px 28px;border-radius:10px;font-weight:700;font-size:14px'>Ver portal Gorila Motos →</a>" +
            "</td></tr>" +
            // Divider
            "<tr><td style='height:1px;background:#F0F0F2'></td></tr>" +
            // Footer
            "<tr><td style='background:#F9F9FB;padding:20px 36px'>" +
            "<p style='margin:0;color:#aaa;font-size:11px'>© " + java.time.Year.now().getValue() +
            " Gorila Motos · Cuenca, Ecuador · " +
            "<a href='https://gorila-motos.vercel.app' style='color:#E11428;text-decoration:none'>gorila-motos.vercel.app</a></p>" +
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
            "https://gorila-motos.vercel.app",
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
            "https://gorila-motos.vercel.app",
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
               " Gorila Motos · Cuenca, Ecuador · <a href='https://gorila-motos.vercel.app' style='color:#E11428'>gorila-motos.vercel.app</a></p>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private String htmlServicio(String nombre, String placa, String tipoServicio,
                                double costoTotal, String fecha, Long idRegistro,
                                List<DetalleFactura> detalles) {
        String costoStr = String.format("$%.2f", costoTotal);
        // Número de orden ÚNICO: mismo formato que la web/impresión/APK (ordenNumero → ORD-000NNN)
        String refNum   = idRegistro != null ? String.format("ORD-%06d", idRegistro) : "ORD";
        int yr = java.time.Year.now().getValue();
        String LOGO = "https://gmotors-frontend.vercel.app/brand/gorila-logo.png";

        // ── Generar filas de detalles (mano de obra + repuestos) ──
        StringBuilder filasHtml = new StringBuilder();
        if (detalles != null && !detalles.isEmpty()) {
            // separar por tipo
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
            // header tabla
            filasHtml.append("<tr><td style='padding:16px 14px 6px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#9CA3AF' colspan='4'>Desglose del servicio</td></tr>");
            filasHtml.append("<tr style='background:#F3F4F6'>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;border-bottom:1px solid #E5E7EB'>Concepto</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;border-bottom:1px solid #E5E7EB;text-align:center'>Cant.</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;border-bottom:1px solid #E5E7EB;text-align:right'>Subtotal</td>" +
                "<td style='padding:9px 14px;font-family:Arial,sans-serif;font-size:11px;font-weight:700;color:#374151;border-bottom:1px solid #E5E7EB;text-align:center'>Tipo</td>" +
                "</tr>");

            if (!manoList.isEmpty()) {
                filasHtml.append("<tr><td colspan='4' style='padding:8px 14px 4px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#3B82F6;background:#EFF6FF'>— Mano de obra</td></tr>");
                for (DetalleFactura d : manoList) {
                    String desc = d.getDescripcion() != null ? d.getDescripcion().replaceAll("^\\[MANO\\]\\s*", "") : "Servicio";
                    String sub  = d.getSubtotal() != null ? String.format("$%.2f", d.getSubtotal()) : "$0.00";
                    subtotalMano = subtotalMano.add(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO);
                    filasHtml.append("<tr style='border-bottom:1px solid #F3F4F6'>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#111'>" + desc + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#374151;text-align:center'>" + (d.getCantidad() != null ? d.getCantidad() : 1) + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:600;color:#111;text-align:right'>" + sub + "</td>" +
                        "<td style='padding:10px 14px;text-align:center'><span style='font-family:Arial,sans-serif;font-size:10px;font-weight:700;color:#3B82F6;background:#DBEAFE;border-radius:4px;padding:2px 7px'>MO</span></td>" +
                        "</tr>");
                }
                filasHtml.append("<tr style='background:#EFF6FF'><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#3B82F6'>Subtotal mano de obra</td><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:700;color:#3B82F6;text-align:right'>" + String.format("$%.2f", subtotalMano) + "</td></tr>");
            }
            if (!repList.isEmpty()) {
                filasHtml.append("<tr><td colspan='4' style='padding:8px 14px 4px;font-family:Arial,sans-serif;font-size:10px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#D97706;background:#FFFBEB'>— Repuestos / Inventario</td></tr>");
                for (DetalleFactura d : repList) {
                    String desc = d.getDescripcion() != null ? d.getDescripcion().replaceAll("^\\[REP\\|[^\\]]+\\]\\s*", "") : "Repuesto";
                    String sub  = d.getSubtotal() != null ? String.format("$%.2f", d.getSubtotal()) : "$0.00";
                    subtotalRep = subtotalRep.add(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO);
                    filasHtml.append("<tr style='border-bottom:1px solid #F3F4F6'>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#111'>" + desc + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;color:#374151;text-align:center'>" + (d.getCantidad() != null ? d.getCantidad() : 1) + "</td>" +
                        "<td style='padding:10px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:600;color:#111;text-align:right'>" + sub + "</td>" +
                        "<td style='padding:10px 14px;text-align:center'><span style='font-family:Arial,sans-serif;font-size:10px;font-weight:700;color:#D97706;background:#FEF3C7;border-radius:4px;padding:2px 7px'>REP</span></td>" +
                        "</tr>");
                }
                filasHtml.append("<tr style='background:#FFFBEB'><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:12px;font-weight:700;color:#D97706'>Subtotal repuestos</td><td colspan='2' style='padding:8px 14px;font-family:Arial,sans-serif;font-size:13px;font-weight:700;color:#D97706;text-align:right'>" + String.format("$%.2f", subtotalRep) + "</td></tr>");
            }
        }
        boolean tieneDetalles = detalles != null && !detalles.isEmpty();

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>" +
               "body{margin:0;padding:0;background:#EAEDF2;font-family:Arial,sans-serif}" +
               "@media only screen and (max-width:620px){" +
               ".outer{padding:12px 4px!important}.hpad{padding:22px 20px!important}" +
               ".bpad{padding:24px 20px!important}.tpad{padding:0 20px!important}" +
               ".fpad{padding:16px 20px!important}.total-amount{font-size:26px!important}" +
               "}" +
               "</style>" +
               "</head><body>" +
               "<table class='outer' width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px;background:#EAEDF2'>" +
               "<tr><td align='center'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.10)'>" +

               /* Banda roja superior */
               "<tr><td style='background:#C8001A;height:5px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

               /* Header con logo */
               "<tr><td class='hpad' style='background:#0D0D12;padding:24px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='vertical-align:middle'>" +
               "<img src='" + LOGO + "' alt='Gorila Motos' width='50' height='50' style='display:block;border-radius:10px;border:2px solid rgba(200,0,26,0.5)' />" +
               "</td>" +
               "<td style='vertical-align:middle;padding-left:13px'>" +
               "<p style='margin:0;color:#fff;font-size:21px;font-weight:900;font-family:Georgia,serif;letter-spacing:-0.3px'>Gorila <span style='color:#E8192C'>Motos</span></p>" +
               "<p style='margin:3px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:2.5px;text-transform:uppercase'>Taller Mecánico · Cuenca, Ecuador</p>" +
               "</td>" +
               "<td align='right' style='vertical-align:middle'>" +
               "<div style='background:rgba(200,0,26,0.12);border:1px solid rgba(200,0,26,0.4);border-radius:8px;padding:10px 16px;text-align:center'>" +
               "<p style='margin:0;color:#E8192C;font-size:9px;font-weight:700;letter-spacing:2.5px;text-transform:uppercase'>NOTA DE VENTA</p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;font-weight:700;font-family:\"Courier New\",monospace'>" + refNum + "</p>" +
               "</div></td></tr></table></td></tr>" +

               /* Saludo */
               "<tr><td style='padding:26px 36px 12px;border-bottom:1px solid #F1F1F4'>" +
               "<p style='margin:0 0 6px;font-size:20px;font-weight:900;color:#111'>¡Gracias por su confianza!</p>" +
               "<p style='margin:0;font-size:14px;color:#555;line-height:1.65'>Estimado/a <strong style='color:#111'>" + nombre + "</strong>, a continuación su comprobante de servicio realizado en Gorila Motos.</p>" +
               "</td></tr>" +

               /* Info vehículo */
               "<tr><td style='padding:20px 36px 0'>" +
               "<p style='margin:0 0 10px;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#9CA3AF'>Datos del servicio</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid #E5E7EB'>" +
               "<tr style='background:#F8F9FA'>" +
               "<td style='padding:10px 14px;font-size:11px;font-weight:700;color:#6B7280;letter-spacing:1px;text-transform:uppercase;border-bottom:1px solid #E5E7EB;width:38%'>Placa / Vehículo</td>" +
               "<td style='padding:10px 14px;font-size:15px;font-weight:700;color:#111;border-bottom:1px solid #E5E7EB;font-family:\"Courier New\",monospace'>" + placa + "</td>" +
               "</tr><tr>" +
               "<td style='padding:10px 14px;font-size:11px;font-weight:700;color:#6B7280;letter-spacing:1px;text-transform:uppercase;border-bottom:1px solid #E5E7EB;background:#FAFAFA'>Tipo de servicio</td>" +
               "<td style='padding:10px 14px;font-size:14px;font-weight:700;color:#111;border-bottom:1px solid #E5E7EB'>" + tipoServicio + "</td>" +
               "</tr><tr style='background:#F8F9FA'>" +
               "<td style='padding:10px 14px;font-size:11px;font-weight:700;color:#6B7280;letter-spacing:1px;text-transform:uppercase'>Fecha</td>" +
               "<td style='padding:10px 14px;font-size:14px;color:#374151'>" + fecha + "</td>" +
               "</tr></table></td></tr>" +

               /* Tabla de detalles (cuando hay ítems) */
               (tieneDetalles ? (
               "<tr><td style='padding:4px 36px 0'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid #E5E7EB;border-top:none'>" +
               filasHtml.toString() +
               "</table></td></tr>"
               ) : "") +

               /* Total */
               "<tr><td style='padding:20px 36px'>" +
               "<div style='background:linear-gradient(135deg,#C8001A 0%,#8B0000 100%);border-radius:10px;padding:18px 24px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-size:10px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.65)'>Total del servicio</p>" +
               "<p class='total-amount' style='margin:6px 0 0;font-family:Georgia,serif;font-size:30px;font-weight:700;color:#fff;letter-spacing:-0.5px'>" + costoStr + "</p></td>" +
               "<td align='right'><p style='margin:0;font-size:11px;color:rgba(255,255,255,0.5)'>" + fecha + "</p>" +
               "<p style='margin:8px 0 0;font-family:\"Courier New\",monospace;font-size:11px;color:rgba(255,255,255,0.35)'>" + refNum + "</p></td>" +
               "</tr></table></div></td></tr>" +

               /* Nota */
               "<tr><td style='padding:0 36px 28px'>" +
               "<div style='background:#FFFBF2;border:1px solid #FDE68A;border-radius:8px;padding:14px 18px'>" +
               "<p style='margin:0;font-size:12px;color:#78350F;line-height:1.65'>" +
               "Conserve este correo como comprobante oficial. Para consultas comuníquese a " +
               "<a href='mailto:gorilamotos2026@gmail.com' style='color:#C8001A;font-weight:700'>gorilamotos2026@gmail.com</a> " +
               "o visítenos en nuestro taller en Cuenca, Ecuador.</p></div></td></tr>" +

               /* Footer */
               "<tr><td class='fpad' style='background:#F8F9FA;border-top:1px solid #E5E7EB;padding:18px 36px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;font-size:10px;color:#9CA3AF'>© " + yr + " Gorila Motos S.A.S. &nbsp;·&nbsp; Cuenca, Ecuador</p>" +
               "<p style='margin:3px 0 0;font-size:10px'><a href='https://gmotors-frontend.vercel.app' style='color:#C8001A;text-decoration:none'>gmotors-frontend.vercel.app</a></p></td>" +
               "<td align='right'><img src='" + LOGO + "' alt='' width='36' height='36' style='border-radius:8px;border:1px solid rgba(0,0,0,0.1)' /></td>" +
               "</tr></table></td></tr>" +

               /* Banda roja inferior */
               "<tr><td style='background:#C8001A;height:4px;font-size:1px;line-height:1px'>&nbsp;</td></tr>" +

               "</table></td></tr></table>" +
               "</body></html>";
    }

    private String fila(String label, String value) {
        return "<tr><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#666;font-size:13px;width:40%'>" +
               label + "</td><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#111;font-size:14px;font-weight:600'>" +
               value + "</td></tr>";
    }
}
