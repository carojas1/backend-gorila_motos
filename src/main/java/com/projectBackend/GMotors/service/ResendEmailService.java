package com.projectBackend.GMotors.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResendEmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:noreply@gorilamoto.com}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESEND_URL = "https://api.resend.com/emails";

    /* ── Envío genérico ── */
    private boolean enviar(String to, String subject, String html) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("[EMAIL] API key no configurada — email no enviado a: " + to);
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Gorila Motos <" + fromEmail + ">");
            body.put("to", to);
            body.put("subject", subject);
            body.put("html", html);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = restTemplate.postForEntity(RESEND_URL, req, String.class);
            System.out.println("[EMAIL] Enviado a " + to + " — status: " + res.getStatusCode());
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("[EMAIL] Error al enviar a " + to + ": " + e.getMessage());
            return false;
        }
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

        String enlaceFactura = "https://gorila-motos.vercel.app/invoice/" + idRegistro;
        String costoStr      = String.format("$%.2f", costoTotal);

        String detalles = "<table style='width:100%;border-collapse:collapse;margin:16px 0'>" +
            fila("Vehículo (placa)",  placa)       +
            fila("Servicio",          tipoServicio) +
            fila("Fecha",             fecha)        +
            fila("Total",             "<strong style='color:#E11428;font-size:18px'>" + costoStr + "</strong>") +
            "</table>";

        String html = htmlFactura(nombreCliente, detalles, enlaceFactura);
        return enviar(correoCliente, "Tu factura de servicio — Gorila Motos", html);
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
               "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
               "<body style='margin:0;padding:0;background:#F0F2F5;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px'>" +
               "<table width='580' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.12)'>" +

               /* ── Header oscuro ── */
               "<tr><td style='background:linear-gradient(135deg,#0C0C10 0%,#1A1A22 100%);padding:30px 40px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#ffffff;font-size:24px;font-weight:900;letter-spacing:-0.5px'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.35);font-size:10px;letter-spacing:3px;text-transform:uppercase'>Sistema de gestión · Ecuador</p></td>" +
               "<td align='right'><div style='display:inline-block;background:rgba(225,20,40,0.15);border:1px solid rgba(225,20,40,0.35);border-radius:10px;padding:8px 14px'>" +
               "<p style='margin:0;color:#E11428;font-size:10px;font-weight:900;letter-spacing:2px;text-transform:uppercase'>Comprobante</p>" +
               "<p style='margin:2px 0 0;color:rgba(255,255,255,0.6);font-size:12px;font-weight:700'>#" + referencia + "</p>" +
               "</div></td></tr></table></td></tr>" +

               /* ── Saludo ── */
               "<tr><td style='padding:32px 40px 0'>" +
               "<h2 style='margin:0 0 6px;color:#111827;font-size:22px;font-weight:800'>¡Gracias por tu compra!</h2>" +
               "<p style='margin:0;color:#6B7280;font-size:14px'>Hola <strong style='color:#111827'>" + nombre + "</strong>, aquí está tu comprobante de venta.</p>" +
               "</td></tr>" +

               /* ── Línea divisora puntuada (recibo) ── */
               "<tr><td style='padding:24px 40px'>" +
               "<div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               /* ── Detalle del producto ── */
               "<tr><td style='padding:0 40px'>" +
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
               "<tr><td style='padding:20px 40px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td style='background:linear-gradient(135deg,#E11428,#B91C1C);border-radius:14px;padding:18px 24px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:rgba(255,255,255,0.75);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:1px'>Total pagado</p>" +
               "<p style='margin:4px 0 0;color:#fff;font-size:28px;font-weight:900;letter-spacing:-1px'>" + totalStr + "</p></td>" +
               "<td align='right'><p style='margin:0;color:rgba(255,255,255,0.5);font-size:11px'>" + fecha + "</p></td>" +
               "</tr></table></td></tr></table></td></tr>" +

               /* ── Línea divisora puntuada ── */
               "<tr><td style='padding:0 40px 24px'>" +
               "<div style='border-top:2px dashed #E5E7EB'></div></td></tr>" +

               /* ── Nota informativa ── */
               "<tr><td style='padding:0 40px 32px'>" +
               "<div style='background:#FEF9EC;border-left:3px solid #F59E0B;border-radius:0 10px 10px 0;padding:14px 16px'>" +
               "<p style='margin:0;color:#92400E;font-size:12px;line-height:1.6'>" +
               "<strong>Conserva este correo</strong> como comprobante de tu compra. " +
               "Si tienes preguntas, escríbenos a <a href='mailto:info@gorilamoto.com' style='color:#E11428'>info@gorilamoto.com</a>.</p>" +
               "</div></td></tr>" +

               /* ── Footer ── */
               "<tr><td style='background:#F9FAFB;padding:20px 40px;border-top:1px solid #F3F4F6'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><p style='margin:0;color:#9CA3AF;font-size:11px'>© " + java.time.Year.now().getValue() +
               " Gorila Motos · Quito, Ecuador</p></td>" +
               "<td align='right'><a href='https://gorila-motos.vercel.app' style='color:#E11428;font-size:11px;text-decoration:none'>gorila-motos.vercel.app</a></td>" +
               "</tr></table></td></tr>" +
               "</table></td></tr></table></body></html>";
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
            "Ante cualquier consulta, escríbenos a info@gorilamoto.com"
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
               " Gorila Motos · Quito, Ecuador · <a href='https://gorila-motos.vercel.app' style='color:#E11428'>gorila-motos.vercel.app</a></p>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private String htmlFactura(String nombre, String detalles, String enlace) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
               "margin:0;padding:0;background:#F4F4F5;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px'>" +
               "<table width='560' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;" +
               "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)'>" +
               "<tr><td style='background:#0C0C10;padding:28px 40px'>" +
               "<p style='margin:0;color:#fff;font-size:22px;font-weight:900'>Gorila <span style='color:#E11428'>Motos</span></p>" +
               "<p style='margin:4px 0 0;color:rgba(255,255,255,0.4);font-size:11px;letter-spacing:2px;text-transform:uppercase'>Comprobante de servicio</p>" +
               "</td></tr>" +
               "<tr><td style='padding:36px 40px'>" +
               "<h2 style='margin:0 0 8px;color:#111;font-size:20px'>¡Gracias por tu visita, " + nombre + "!</h2>" +
               "<p style='margin:0 0 24px;color:#666;font-size:14px'>Aquí está el resumen de tu servicio de hoy:</p>" +
               detalles +
               "<a href='" + enlace + "' style='display:inline-block;background:#E11428;color:#fff;text-decoration:none;" +
               "padding:12px 28px;border-radius:10px;font-weight:700;font-size:14px;margin-top:8px'>Ver factura completa</a>" +
               "<p style='margin:20px 0 0;color:#aaa;font-size:12px'>Guarda este correo como comprobante de tu servicio.</p>" +
               "</td></tr>" +
               "<tr><td style='background:#F9F9FB;padding:20px 40px;border-top:1px solid #eee'>" +
               "<p style='margin:0;color:#aaa;font-size:11px'>© " + java.time.Year.now().getValue() +
               " Gorila Motos · <a href='https://gorila-motos.vercel.app' style='color:#E11428'>gorila-motos.vercel.app</a></p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    private String fila(String label, String value) {
        return "<tr><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#666;font-size:13px;width:40%'>" +
               label + "</td><td style='padding:10px 12px;border-bottom:1px solid #f0f0f0;color:#111;font-size:14px;font-weight:600'>" +
               value + "</td></tr>";
    }
}
