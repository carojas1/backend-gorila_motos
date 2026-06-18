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

import jakarta.mail.internet.MimeMessage;
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
        String html = htmlServicio(nombreCliente, placa, tipoServicio, costoTotal, fecha, idRegistro);
        return enviar(correoCliente, "Comprobante de servicio #" + (idRegistro != null ? idRegistro : "") + " — Gorila Motos", html);
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

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>@media only screen and (max-width:600px){.email-wrap{width:100%!important;border-radius:12px!important}.email-pad{padding:20px 16px!important}.email-h1{font-size:18px!important}.email-total{font-size:22px!important}}</style>" +
               "</head>" +
               "<body style='margin:0;padding:0;background:#F0F2F5;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:24px 8px'>" +
               "<table class='email-wrap' width='580' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.12);max-width:580px;width:100%'>" +

               /* ── Header oscuro ── */
               "<tr><td class='email-pad' style='background:linear-gradient(135deg,#0C0C10 0%,#1A1A22 100%);padding:24px 28px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#ffffff;font-size:24px;font-weight:900;letter-spacing:-0.5px'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:3px;text-transform:uppercase'>Sistema de gestión · Ecuador</p></td>" +
               "<td align='right'><div style='display:inline-block;background:rgba(225,20,40,0.15);border:1px solid rgba(225,20,40,0.35);border-radius:10px;padding:8px 14px'>" +
               "<p style='margin:0;color:#E11428;font-size:10px;font-weight:900;letter-spacing:2px;text-transform:uppercase'>Comprobante</p>" +
               "<p style='margin:2px 0 0;color:rgba(255,255,255,0.6);font-size:12px;font-weight:700'>#" + referencia + "</p>" +
               "</div></td></tr></table></td></tr>" +

               /* ── Saludo ── */
               "<tr><td class='email-pad' style='padding:24px 28px 0'>" +
               "<h2 class='email-h1' style='margin:0 0 6px;color:#111827;font-size:22px;font-weight:800'>¡Gracias por tu compra!</h2>" +
               "<p style='margin:0;color:#6B7280;font-size:14px'>Hola <strong style='color:#111827'>" + nombre + "</strong>, aquí está tu comprobante de venta.</p>" +
               "</td></tr>" +

               /* ── Línea divisora puntuada (recibo) ── */
               "<tr><td class='email-pad' style='padding:20px 28px'>" +
               "<div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               /* ── Detalle del producto ── */
               "<tr><td class='email-pad' style='padding:0 28px'>" +
               "<p style='margin:0 0 14px;color:#9CA3AF;font-size:10px;font-weight:800;letter-spacing:3px;text-transform:uppercase'>Detalle de la compra</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:separate;border-spacing:0;border-radius:12px;overflow:hidden;border:1px solid #F3F4F6'>" +
               "<thead><tr style='background:#F9FAFB'>" +
               "<th style='padding:10px 16px;text-align:left;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6'>Producto</th>" +
               "<th style='padding:10px 16px;text-align:center;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6'>Cant.</th>" +
               "<th style='padding:10px 16px;text-align:right;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6'>P. Unit.</th>" +
               "<th style='padding:10px 16px;text-align:right;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6'>Subtotal</th>" +
               "</tr></thead><tbody>" +
               "<tr><td style='padding:14px 16px;color:#111827;font-size:14px;font-weight:600'>" + producto +
               "<br><span style='font-size:11px;color:#9CA3AF;font-weight:400'>Código: " + codigo + "</span></td>" +
               "<td style='padding:14px 16px;text-align:center;color:#374151;font-size:14px;font-weight:700'>" + cantidad + "</td>" +
               "<td style='padding:14px 16px;text-align:right;color:#374151;font-size:14px;font-weight:600'>" + pvpStr + "</td>" +
               "<td style='padding:14px 16px;text-align:right;color:#111827;font-size:14px;font-weight:700'>" + totalStr + "</td>" +
               "</tr></tbody></table></td></tr>" +

               /* ── Total destacado ── */
               "<tr><td class='email-pad' style='padding:16px 28px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='background:linear-gradient(135deg,#E11428,#B91C1C);border-radius:14px;padding:16px 20px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:rgba(255,255,255,0.75);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:1px'>Total pagado</p>" +
               "<p class='email-total' style='margin:4px 0 0;color:#fff;font-size:28px;font-weight:900;letter-spacing:-1px'>" + totalStr + "</p></td>" +
               "<td align='right' style='white-space:nowrap'><p style='margin:0;color:rgba(255,255,255,0.5);font-size:11px'>" + fecha + "</p></td>" +
               "</tr></table></td></tr></table></td></tr>" +

               /* ── Línea divisora puntuada ── */
               "<tr><td class='email-pad' style='padding:0 28px 20px'>" +
               "<div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               /* ── Nota informativa ── */
               "<tr><td class='email-pad' style='padding:0 28px 28px'>" +
               "<div style='background:#FEF9EC;border-left:3px solid #F59E0B;border-radius:0 10px 10px 0;padding:14px 16px'>" +
               "<p style='margin:0;color:#92400E;font-size:12px;line-height:1.6'>" +
               "<strong>Conserva este correo</strong> como comprobante de tu compra. " +
               "Si tienes preguntas, escríbenos a <a href='mailto:gorilamotos2026@gmail.com' style='color:#E11428'>gorilamotos2026@gmail.com</a>.</p>" +
               "</div></td></tr>" +

               /* ── Footer ── */
               "<tr><td class='email-pad' style='background:#F9FAFB;padding:16px 28px;border-top:1px solid #F3F4F6'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#9CA3AF;font-size:11px'>© " + java.time.Year.now().getValue() +
               " Gorila Motos · Cuenca, Ecuador</p></td>" +
               "<td align='right'><a href='https://gorila-motos.vercel.app' style='color:#E11428;font-size:11px;text-decoration:none'>gorila-motos.vercel.app</a></td>" +
               "</tr></table></td></tr>" +
               "</table></td></tr></table></body></html>";
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
                                double costoTotal, String fecha, Long idRegistro) {
        String costoStr = String.format("$%.2f", costoTotal);
        String refNum   = idRegistro != null ? String.format("SRV-%05d", idRegistro) : "SRV";
        String enlace   = "https://gorila-motos.vercel.app" + (idRegistro != null ? "/invoice/" + idRegistro : "");

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>@media only screen and (max-width:600px){.email-wrap{width:100%!important;border-radius:12px!important}.email-pad{padding:16px!important}.email-h1{font-size:18px!important}.email-total{font-size:22px!important}}</style>" +
               "</head>" +
               "<body style='margin:0;padding:0;background:#F0F2F5;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:24px 8px'>" +
               "<table class='email-wrap' width='580' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.12);max-width:580px;width:100%'>" +

               // ── Header oscuro con logo + badge referencia ──
               "<tr><td class='email-pad' style='background:linear-gradient(135deg,#0C0C10 0%,#1A1A22 100%);padding:24px 28px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#ffffff;font-size:24px;font-weight:900;letter-spacing:-0.5px'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:3px;text-transform:uppercase'>Sistema de gestión · Ecuador</p></td>" +
               "<td align='right'><div style='display:inline-block;background:rgba(225,20,40,0.15);border:1px solid rgba(225,20,40,0.35);border-radius:10px;padding:8px 14px'>" +
               "<p style='margin:0;color:#E11428;font-size:10px;font-weight:900;letter-spacing:2px;text-transform:uppercase'>Servicio</p>" +
               "<p style='margin:2px 0 0;color:rgba(255,255,255,0.6);font-size:12px;font-weight:700'>#" + refNum + "</p>" +
               "</div></td></tr></table></td></tr>" +

               // ── Saludo ──
               "<tr><td class='email-pad' style='padding:24px 28px 0'>" +
               "<h2 class='email-h1' style='margin:0 0 6px;color:#111827;font-size:22px;font-weight:800'>¡Gracias por tu visita!</h2>" +
               "<p style='margin:0;color:#6B7280;font-size:14px'>Hola <strong style='color:#111827'>" + nombre + "</strong>, aquí está el resumen de tu servicio en Gorila Motos.</p>" +
               "</td></tr>" +

               // ── Línea divisora puntuada ──
               "<tr><td class='email-pad' style='padding:20px 28px'><div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               // ── Tabla de detalles del servicio ──
               "<tr><td class='email-pad' style='padding:0 28px'>" +
               "<p style='margin:0 0 14px;color:#9CA3AF;font-size:10px;font-weight:800;letter-spacing:3px;text-transform:uppercase'>Detalle del servicio</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:1px solid #F3F4F6;border-radius:12px;overflow:hidden'>" +
               "<tr style='background:#F9FAFB'>" +
               "<th style='padding:10px 16px;text-align:left;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6;width:40%'>Campo</th>" +
               "<th style='padding:10px 16px;text-align:left;color:#6B7280;font-size:11px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;border-bottom:1px solid #F3F4F6'>Detalle</th>" +
               "</tr>" +
               "<tr><td style='padding:14px 16px;color:#6B7280;font-size:13px;border-bottom:1px solid #F9FAFB'>Vehículo (placa)</td>" +
               "<td style='padding:14px 16px;color:#111827;font-size:14px;font-weight:700;border-bottom:1px solid #F9FAFB'>" + placa + "</td></tr>" +
               "<tr style='background:#FAFAFA'><td style='padding:14px 16px;color:#6B7280;font-size:13px;border-bottom:1px solid #F9FAFB'>Servicio realizado</td>" +
               "<td style='padding:14px 16px;color:#111827;font-size:14px;font-weight:700;border-bottom:1px solid #F9FAFB'>" + tipoServicio + "</td></tr>" +
               "<tr><td style='padding:14px 16px;color:#6B7280;font-size:13px'>Fecha</td>" +
               "<td style='padding:14px 16px;color:#111827;font-size:14px;font-weight:700'>" + fecha + "</td></tr>" +
               "</table></td></tr>" +

               // ── Total destacado en rojo ──
               "<tr><td class='email-pad' style='padding:16px 28px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='background:linear-gradient(135deg,#E11428,#B91C1C);border-radius:14px;padding:16px 20px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:rgba(255,255,255,0.75);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:1px'>Total del servicio</p>" +
               "<p class='email-total' style='margin:4px 0 0;color:#fff;font-size:28px;font-weight:900;letter-spacing:-1px'>" + costoStr + "</p></td>" +
               "<td align='right' style='white-space:nowrap'><p style='margin:0;color:rgba(255,255,255,0.5);font-size:11px'>" + fecha + "</p></td>" +
               "</tr></table></td></tr></table></td></tr>" +

               // ── Botón ver orden ──
               "<tr><td class='email-pad' style='padding:0 28px 24px'>" +
               "<a href='" + enlace + "' style='display:inline-block;background:linear-gradient(135deg,#E11428,#B91C1C);color:#fff;text-decoration:none;padding:13px 28px;border-radius:12px;font-weight:700;font-size:14px'>Ver orden de servicio →</a>" +
               "</td></tr>" +

               // ── Línea divisora puntuada ──
               "<tr><td class='email-pad' style='padding:0 28px 20px'><div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               // ── Nota informativa ──
               "<tr><td class='email-pad' style='padding:0 28px 28px'>" +
               "<div style='background:#FEF9EC;border-left:3px solid #F59E0B;border-radius:0 10px 10px 0;padding:14px 16px'>" +
               "<p style='margin:0;color:#92400E;font-size:12px;line-height:1.6'>" +
               "<strong>Conserva este correo</strong> como comprobante de tu servicio. " +
               "Si tienes preguntas, escríbenos a <a href='mailto:gorilamotos2026@gmail.com' style='color:#E11428'>gorilamotos2026@gmail.com</a>.</p>" +
               "</div></td></tr>" +

               // ── Footer ──
               "<tr><td class='email-pad' style='background:#F9FAFB;padding:16px 28px;border-top:1px solid #F3F4F6'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#9CA3AF;font-size:11px'>© " + java.time.Year.now().getValue() +
               " Gorila Motos · Cuenca, Ecuador</p></td>" +
               "<td align='right'><a href='https://gorila-motos.vercel.app' style='color:#E11428;font-size:11px;text-decoration:none'>gorila-motos.vercel.app</a></td>" +
               "</tr></table></td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private String fila(String label, String value) {
        return "<tr><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#666;font-size:13px;width:40%'>" +
               label + "</td><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#111;font-size:14px;font-weight:600'>" +
               value + "</td></tr>";
    }
}
