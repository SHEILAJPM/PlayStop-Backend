package com.playstop.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Render bloquea el tráfico SMTP saliente (puertos 25/465/587), por eso se usa
    // la API HTTP de Brevo (puerto 443) en vez de JavaMailSender. El remitente
    // (app.mail.from) debe estar verificado como "Sender" en Brevo.
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    // ─── PLANTILLA BASE ───────────────────────────────────────────────────────

    private String buildEmail(String content) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f0f4f8; font-family:'Segoe UI', Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f4f8; padding:40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0"
                                   style="max-width:600px; width:100%%; background:#ffffff;
                                          border-radius:16px; overflow:hidden;
                                          box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                                <!-- HEADER -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 50%%, #0f3460 100%%);
                                               padding:40px 40px 30px; text-align:center;">
                                        <div style="display:inline-block; background:rgba(255,255,255,0.08);
                                                    border-radius:50%%; width:70px; height:70px;
                                                    line-height:70px; font-size:32px; margin-bottom:16px;">
                                            ⚽
                                        </div>
                                        <h1 style="margin:0; color:#ffffff; font-size:28px;
                                                   font-weight:800; letter-spacing:2px;">
                                            Play<span style="color:#e94560;">Stop</span>
                                        </h1>
                                        <p style="margin:6px 0 0; color:rgba(255,255,255,0.6);
                                                  font-size:13px; letter-spacing:1px;">
                                            RESERVAS DEPORTIVAS
                                        </p>
                                    </td>
                                </tr>

                                <!-- CONTENT -->
                                <tr>
                                    <td style="padding:40px;">
                                        %s
                                    </td>
                                </tr>

                                <!-- FOOTER -->
                                <tr>
                                    <td style="background:#f8fafc; border-top:1px solid #e8ecf0;
                                               padding:24px 40px; text-align:center;">
                                        <p style="margin:0; color:#94a3b8; font-size:12px; line-height:1.6;">
                                            Este correo fue enviado automáticamente por PlayStop.<br>
                                            Si no reconoces esta actividad, ignora este mensaje.
                                        </p>
                                        <p style="margin:12px 0 0; color:#cbd5e1; font-size:11px;">
                                            © 2026 PlayStop · Todos los derechos reservados
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.formatted(content);
    }

    // ─── BADGE DE ESTADO ─────────────────────────────────────────────────────

    private String badge(String color, String text) {
        return """
            <div style="display:inline-block; background:%s;
                        color:#ffffff; font-size:12px; font-weight:700;
                        padding:4px 14px; border-radius:20px;
                        letter-spacing:1px; margin-bottom:20px;">
                %s
            </div>
        """.formatted(color, text);
    }

    // ─── FILA DE DETALLE ─────────────────────────────────────────────────────

    private String detailRow(String icon, String label, String value) {
        return """
            <tr>
                <td style="padding:12px 16px; border-bottom:1px solid #f1f5f9;">
                    <span style="font-size:18px; margin-right:10px;">%s</span>
                    <span style="color:#64748b; font-size:13px; font-weight:600;">%s</span>
                </td>
                <td style="padding:12px 16px; border-bottom:1px solid #f1f5f9;
                           text-align:right; color:#1e293b; font-size:14px; font-weight:600;">
                    %s
                </td>
            </tr>
        """.formatted(icon, label, value);
    }

    // ─── EMAIL: BIENVENIDA ────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Bienvenido, %s! 🎉
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Tu cuenta ha sido creada exitosamente. Ya puedes explorar canchas,
                ver disponibilidad y hacer tus reservas en segundos.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f8fafc; border-radius:12px; margin-bottom:28px;">
                <tr>
                    <td style="padding:20px; text-align:center;">
                        <table cellpadding="0" cellspacing="0" style="margin:0 auto;">
                            <tr>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">🔍</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        BUSCA
                                    </p>
                                </td>
                                <td style="color:#cbd5e1; font-size:20px;">›</td>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">📅</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        RESERVA
                                    </p>
                                </td>
                                <td style="color:#cbd5e1; font-size:20px;">›</td>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">🏃</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        JUEGA
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>

            <p style="margin:0; color:#94a3b8; font-size:13px; text-align:center;">
                ¿Listo para jugar? El equipo de PlayStop te espera. 💪
            </p>
        """.formatted(badge("#22c55e", "✓ CUENTA ACTIVADA"), userName);

        sendHtmlEmail(toEmail, "👋 Bienvenido a PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CONFIRMACIÓN DE RESERVA (con QR adjunto) ─────────────────────

    @Async
    public void sendReservationConfirmationWithQr(String toEmail, String userName,
                                                   String courtName, String date, String slot,
                                                   String reservationId, byte[] qrBytes) {
        log.info("Enviando email de confirmación con QR a: {}", toEmail);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Reserva confirmada! ✅
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu reserva está lista.
                Presenta el código QR al ingresar a la instalación.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
                %s
            </table>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f8fafc; border:1px dashed #cbd5e1;
                          border-radius:16px; margin-bottom:28px;">
                <tr>
                    <td style="padding:28px; text-align:center;">
                        <p style="margin:0 0 16px; color:#475569; font-size:13px;
                                  font-weight:700; text-transform:uppercase; letter-spacing:1px;">
                            Tu código de entrada
                        </p>
                        <img src="data:image/png;base64,%s" alt="Código QR de reserva"
                             style="width:200px; height:200px; border-radius:12px;
                                    display:block; margin:0 auto;" />
                        <p style="margin:14px 0 0; color:#94a3b8; font-size:11px; font-family:monospace;">
                            ID: %s
                        </p>
                    </td>
                </tr>
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda llegar 10 minutos antes — muestra este QR en recepción
                </p>
            </div>
        """.formatted(
            badge("#0ea5e9", "RESERVA CONFIRMADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot),
            detailRow("👤", "CLIENTE", userName),
            qrBase64,
            reservationId
        );

        sendHtmlEmail(toEmail, "✅ Reserva confirmada - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CONFIRMACIÓN DE RESERVA (sin QR — fallback) ──────────────────

    @Async
    public void sendReservationConfirmation(String toEmail, String userName,
                                             String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Reserva confirmada! ✅
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu reserva está lista.
                Te esperamos en la cancha a la hora indicada.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center; margin-bottom:8px;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda llegar 10 minutos antes de tu reserva
                </p>
            </div>
        """.formatted(
            badge("#0ea5e9", "RESERVA CONFIRMADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "✅ Reserva confirmada - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: NUEVA RESERVA (notificación al propietario) ──────────────────

    @Async
    public void sendNewReservationNotificationToOwner(String ownerEmail, String ownerName,
                                                       String clientName, String clientEmail,
                                                       String courtName, String date, String slot,
                                                       String reservationId, double amount) {
        log.info("Enviando notificación de nueva reserva al propietario: {}", ownerEmail);
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Nueva reserva recibida 🎉
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tienes una nueva reserva confirmada
                en tu cancha. Aquí están los detalles:
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda tener la cancha lista para recibir al cliente
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "🏟️ NUEVA RESERVA"),
            ownerName,
            detailRow("👤", "CLIENTE", clientName),
            detailRow("✉️", "EMAIL", clientEmail),
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(ownerEmail, "🏟️ Nueva reserva en " + courtName + " - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CONFIRMACIÓN DE ASISTENCIA (al jugador con reglas) ───────────

    @Async
    public void sendAttendanceConfirmationToPlayer(String toEmail, String userName,
                                                    String courtName, String date, String slot) {
        log.info("Enviando confirmación de asistencia con reglas al jugador: {}", toEmail);
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Bienvenido a la cancha! 🏟️
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu asistencia ha sido confirmada.
                ¡Que disfrutes el partido!
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:#f8fafc; border:1px solid #e2e8f0;
                        border-radius:14px; padding:24px; margin-bottom:16px;">
                <p style="margin:0 0 16px; color:#1e293b; font-size:15px;
                           font-weight:800; letter-spacing:0.3px;">
                    📋 Reglamento de la cancha
                </p>
                <div style="display:flex; flex-direction:column; gap:10px;">
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                </div>
            </div>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    ⚽ ¡El equipo de PlayStop te desea un excelente partido!
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "✓ ACCESO CONFIRMADO"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot),
            ruleRow("👟", "Usa calzado deportivo apropiado para la superficie de la cancha."),
            ruleRow("🚭", "Prohibido fumar o consumir alcohol en las instalaciones."),
            ruleRow("🧹", "Mantén la cancha limpia — deposita los residuos en los tachos."),
            ruleRow("⏱️", "Respeta estrictamente el horario reservado. No excedas tu tiempo."),
            ruleRow("🤝", "Respeta a los demás jugadores, árbitros y al personal del local."),
            ruleRow("💧", "Puedes traer bebidas en envases cerrados. Nada de bebidas alcohólicas."),
            ruleRow("⚽", "No uses pelotas u objetos que puedan dañar la infraestructura.")
        );

        sendHtmlEmail(toEmail, "✅ ¡Acceso confirmado! Reglamento de la cancha - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CONFIRMACIÓN DE ASISTENCIA (al propietario) ──────────────────

    @Async
    public void sendAttendanceConfirmedToOwner(String ownerEmail, String ownerName,
                                                String clientName, String clientEmail,
                                                String courtName, String date, String slot) {
        log.info("Enviando confirmación de asistencia al propietario: {}", ownerEmail);
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Asistencia confirmada ✅
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, el jugador ha ingresado a tu cancha
                y su asistencia quedó registrada en el sistema.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#faf5ff,#ede9fe);
                        border:1px solid #ddd6fe; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#7c3aed; font-size:13px; font-weight:600;">
                    🏟️ Todo en orden — el jugador ya está en cancha
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "🏟️ JUGADOR EN CANCHA"),
            ownerName,
            detailRow("👤", "JUGADOR", clientName),
            detailRow("✉️", "EMAIL", clientEmail),
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(ownerEmail, "🏟️ Asistencia confirmada en " + courtName + " - PlayStop", buildEmail(content));
    }

    // ─── FILA DE REGLA ───────────────────────────────────────────────────────

    private String ruleRow(String icon, String text) {
        return """
            <div style="display:flex; align-items:flex-start; gap:12px; padding:10px 0;
                        border-bottom:1px solid #f1f5f9;">
                <span style="font-size:18px; flex-shrink:0;">%s</span>
                <span style="color:#475569; font-size:14px; line-height:1.5;">%s</span>
            </div>
        """.formatted(icon, text);
    }

    // ─── EMAIL: TÉRMINOS Y CONDICIONES (al jugador al crear la reserva) ──────

    @Async
    public void sendTermsAndConditionsToPlayer(String toEmail, String userName,
                                               String courtName, String date, String slot,
                                               String reservationId) {
        log.info("Enviando términos y condiciones al jugador: {}", toEmail);

        String html = """
            <h2 style="margin:0 0 4px; color:#1e293b; font-size:22px; font-weight:700;
                       letter-spacing:-0.3px;">
                Términos y Condiciones de Reserva
            </h2>
            <p style="margin:0 0 6px; color:#94a3b8; font-size:11px;
                      text-transform:uppercase; letter-spacing:1.5px; font-weight:600;">
                Documento oficial — PlayStop © 2026
            </p>
            <hr style="border:none; border-top:1px solid #e2e8f0; margin:20px 0;" />

            <p style="margin:0 0 20px; color:#475569; font-size:14px; line-height:1.8;">
                Estimado/a <strong style="color:#1e293b;">%s</strong>,<br>
                a continuación encontrará los términos y condiciones correspondientes
                a su reserva en <strong style="color:#1e293b;">%s</strong>.
                La asistencia a las instalaciones implica la aceptación de las presentes disposiciones.
            </p>

            <!-- DATOS DE RESERVA -->
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #cbd5e1; border-radius:8px;
                          overflow:hidden; margin-bottom:24px; font-size:13px;">
                <tr style="background:#f8fafc;">
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#64748b; font-weight:600; width:40%%;">INSTALACIÓN</td>
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#1e293b; font-weight:700;">%s</td>
                </tr>
                <tr>
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#64748b; font-weight:600;">FECHA</td>
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#1e293b;">%s</td>
                </tr>
                <tr style="background:#f8fafc;">
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#64748b; font-weight:600;">HORARIO</td>
                    <td style="padding:10px 16px; border-bottom:1px solid #e2e8f0;
                               color:#1e293b;">%s</td>
                </tr>
                <tr>
                    <td style="padding:10px 16px; color:#64748b; font-weight:600;">N° RESERVA</td>
                    <td style="padding:10px 16px; color:#1e293b; font-family:monospace;">%s</td>
                </tr>
            </table>

            <!-- SECCIÓN 1 -->
            <p style="margin:0 0 8px; color:#1e293b; font-size:13px; font-weight:700;
                      text-transform:uppercase; letter-spacing:1px; border-left:3px solid #e94560;
                      padding-left:10px;">
                1. Política de Cancelación y Reembolso
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:20px;">
                <tr>
                    <td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7;
                               border-bottom:1px solid #f1f5f9; vertical-align:top;">
                        <span style="color:#1e293b; font-weight:700; margin-right:8px;">1.1</span>
                        El titular podrá cancelar su reserva sin cargo alguno con un mínimo de
                        <strong>24 horas</strong> de anticipación a la fecha pactada.
                    </td>
                </tr>
                <tr>
                    <td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7;
                               border-bottom:1px solid #f1f5f9; vertical-align:top;">
                        <span style="color:#1e293b; font-weight:700; margin-right:8px;">1.2</span>
                        Las cancelaciones realizadas con menos de 24 horas de anticipación
                        <strong>no generan derecho a reembolso</strong>.
                    </td>
                </tr>
                <tr>
                    <td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7;
                               vertical-align:top;">
                        <span style="color:#1e293b; font-weight:700; margin-right:8px;">1.3</span>
                        En caso de proceder el reembolso, este se acreditará en un plazo de
                        <strong>3 a 5 días hábiles</strong>.
                    </td>
                </tr>
            </table>

            <!-- SECCIÓN 2 -->
            <p style="margin:0 0 8px; color:#1e293b; font-size:13px; font-weight:700;
                      text-transform:uppercase; letter-spacing:1px; border-left:3px solid #e94560;
                      padding-left:10px;">
                2. Reglamento de Uso de Instalaciones
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:20px;">
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.1</span>Es obligatorio el uso de calzado deportivo adecuado para la superficie de la instalación.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.2</span>Queda estrictamente prohibido fumar o consumir bebidas alcohólicas dentro de las instalaciones.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.3</span>El usuario deberá mantener las instalaciones en condiciones de limpieza y orden durante su uso.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.4</span>El horario reservado deberá respetarse estrictamente. No se permitirán extensiones de tiempo no autorizadas.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.5</span>Se exige un trato respetuoso hacia los demás usuarios, árbitros y personal del establecimiento.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">2.6</span>Se permite el ingreso de bebidas no alcohólicas en envases cerrados.</td></tr>
            </table>

            <!-- SECCIÓN 3 -->
            <p style="margin:0 0 8px; color:#1e293b; font-size:13px; font-weight:700;
                      text-transform:uppercase; letter-spacing:1px; border-left:3px solid #e94560;
                      padding-left:10px;">
                3. Limitación de Responsabilidad
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">3.1</span>PlayStop y el propietario de la instalación no asumen responsabilidad por lesiones derivadas del uso inadecuado de las instalaciones o del incumplimiento del presente reglamento.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; border-bottom:1px solid #f1f5f9; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">3.2</span>El establecimiento no se responsabiliza por la pérdida, robo o daño de objetos personales dentro de las instalaciones.</td></tr>
                <tr><td style="padding:6px 0; color:#475569; font-size:13px; line-height:1.7; vertical-align:top;"><span style="color:#1e293b; font-weight:700; margin-right:8px;">3.3</span>Ante cualquier emergencia, el usuario deberá notificar de inmediato al personal del establecimiento.</td></tr>
            </table>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f8fafc; border:1px solid #cbd5e1;
                          border-radius:8px; margin-bottom:8px;">
                <tr>
                    <td style="padding:16px 20px;">
                        <p style="margin:0 0 4px; color:#1e293b; font-size:12px;
                                  font-weight:700; text-transform:uppercase; letter-spacing:1px;">
                            Declaración de Aceptación
                        </p>
                        <p style="margin:0; color:#475569; font-size:13px; line-height:1.7;">
                            La asistencia a las instalaciones en la fecha y horario indicados constituye
                            la aceptación plena e incondicional de los presentes Términos y Condiciones.
                        </p>
                    </td>
                </tr>
            </table>
        """.formatted(userName, courtName, courtName, date, slot, reservationId);

        sendHtmlEmail(toEmail, "Terminos y Condiciones de su Reserva - PlayStop", buildEmail(html));
    }

    // ─── EMAIL: CANCELACIÓN ───────────────────────────────────────────────────

    @Async
    public void sendReservationCancellation(String toEmail, String userName,
                                             String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Reserva cancelada
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, confirmamos que tu reserva ha sido cancelada
                y el reembolso está en proceso.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:#fff7ed; border:1px solid #fed7aa;
                        border-radius:12px; padding:16px 20px; margin-bottom:8px;">
                <p style="margin:0; color:#c2410c; font-size:13px; font-weight:600;">
                    💳 El reembolso puede tardar entre 3 y 5 días hábiles en reflejarse.
                </p>
            </div>
        """.formatted(
            badge("#ef4444", "RESERVA CANCELADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "❌ Reserva cancelada - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: RECORDATORIO ──────────────────────────────────────────────────

    @Async
    public void sendReservationReminder(String toEmail, String userName,
                                         String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Tu reserva es en 1 hora! ⏰
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, este es tu recordatorio.
                ¡Prepárate, es hora de jugar!
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#faf5ff,#ede9fe);
                        border:1px solid #ddd6fe; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#7c3aed; font-size:13px; font-weight:600;">
                    🏃 ¡Calienta motores, te esperamos en la cancha!
                </p>
            </div>
        """.formatted(
            badge("#f59e0b", "⏰ RECORDATORIO"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "⏰ Recordatorio de reserva - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CÓDIGO DE RECUPERACIÓN ───────────────────────────────────────

    @Async
    public void sendPasswordResetCode(String toEmail, String userName, String code) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Recupera tu contraseña
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, recibimos una solicitud para
                restablecer tu contraseña. Usa el código de abajo.
            </p>

            <div style="background: linear-gradient(135deg,#1a1a2e,#0f3460);
                        border-radius:16px; padding:32px; text-align:center;
                        margin-bottom:28px;">
                <p style="margin:0 0 12px; color:rgba(255,255,255,0.6);
                           font-size:12px; font-weight:700; letter-spacing:2px;">
                    TU CÓDIGO DE VERIFICACIÓN
                </p>
                <div style="display:inline-block; background:rgba(255,255,255,0.08);
                            border:2px solid rgba(233,69,96,0.5);
                            border-radius:12px; padding:16px 40px;">
                    <span style="font-size:42px; font-weight:900; letter-spacing:14px;
                                 color:#ffffff; font-family:monospace;">
                        %s
                    </span>
                </div>
                <p style="margin:16px 0 0; color:rgba(255,255,255,0.5); font-size:12px;">
                    ⏱ Expira en <strong style="color:#e94560;">15 minutos</strong>
                </p>
            </div>

            <div style="background:#fef2f2; border:1px solid #fecaca;
                        border-radius:12px; padding:16px 20px;">
                <p style="margin:0; color:#dc2626; font-size:13px; font-weight:600;">
                    🔒 Si no solicitaste esto, ignora este correo.
                    Tu contraseña no será cambiada.
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "🔐 RECUPERACIÓN DE CUENTA"),
            userName,
            code
        );

        sendHtmlEmail(toEmail, "🔐 Código de verificación - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: NOTIFICACIÓN DE CHAT ─────────────────────────────────────────

    @Async
    public void sendChatNotificationEmail(String toEmail, String toName,
                                          String senderName, String courtName,
                                          String preview) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Tienes un nuevo mensaje 💬
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, <strong>%s</strong> te escribió
                en el chat de tu reserva en <strong>%s</strong>.
            </p>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:16px;
                        padding:20px 24px; margin-bottom:28px;">
                <p style="margin:0 0 6px; color:#15803d; font-size:12px;
                           font-weight:700; letter-spacing:1px;">
                    💬 MENSAJE
                </p>
                <p style="margin:0; color:#1e293b; font-size:15px;
                           line-height:1.6; font-style:italic;">
                    "%s"
                </p>
            </div>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
            </table>

            <div style="text-align:center; margin-bottom:28px;">
                <a href="https://playstop.pe/dashboard"
                   style="display:inline-block; background:linear-gradient(135deg,#00d084,#00b875);
                          color:#0f172a; font-size:15px; font-weight:700;
                          padding:14px 32px; border-radius:12px;
                          text-decoration:none; letter-spacing:0.5px;">
                    Ver mensaje en PlayStop →
                </a>
            </div>
        """.formatted(
            badge("#00d084", "💬 NUEVO MENSAJE"),
            toName,
            senderName,
            courtName,
            preview,
            detailRow("👤", "DE", senderName),
            detailRow("🏟️", "CANCHA", courtName)
        );

        sendHtmlEmail(toEmail, "💬 Nuevo mensaje en PlayStop - " + courtName, buildEmail(content));
    }

    // ─── EMAIL: RETIRO PROCESADO ──────────────────────────────────────────────

    @Async
    public void sendPayoutPaid(String toEmail, String ownerName, BigDecimal amount) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Tu retiro fue procesado! 💸
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, ya transferimos tu dinero según los datos que registraste.
            </p>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:16px;
                        padding:24px; text-align:center;">
                <p style="margin:0 0 4px; color:#15803d; font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:1px;">
                    Monto transferido
                </p>
                <p style="margin:0; color:#15803d; font-size:2rem; font-weight:900;">
                    S/ %.2f
                </p>
            </div>
        """.formatted(
            badge("#22c55e", "✓ RETIRO COMPLETADO"),
            ownerName, amount
        );

        sendHtmlEmail(toEmail, "💸 Tu retiro fue procesado - PlayStop", buildEmail(content));
    }

    @Async
    public void sendPayoutRejected(String toEmail, String ownerName, BigDecimal amount, String reason) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Tu solicitud de retiro fue rechazada
            </h2>
            <p style="margin:0 0 20px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu solicitud de retiro por <strong>S/ %.2f</strong> no pudo procesarse.
            </p>

            <div style="background:#fef2f2; border:1px solid #fecaca;
                        border-radius:12px; padding:16px 20px; margin-bottom:8px;">
                <p style="margin:0 0 4px; color:#dc2626; font-size:12px; font-weight:700; text-transform:uppercase;">Motivo</p>
                <p style="margin:0; color:#991b1b; font-size:14px;">%s</p>
            </div>
            <p style="margin:16px 0 0; color:#94a3b8; font-size:13px;">
                El monto sigue disponible en tu saldo — puedes corregir los datos y solicitarlo de nuevo.
            </p>
        """.formatted(
            badge("#ef4444", "RETIRO RECHAZADO"),
            ownerName, amount, reason
        );

        sendHtmlEmail(toEmail, "Tu retiro fue rechazado - PlayStop", buildEmail(content));
    }

    // ─── MÉTODO INTERNO ───────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("Enviando email '{}' a: {}", subject, to);
        Map<String, Object> payload = Map.of(
            "sender", Map.of("name", "PlayStop", "email", fromEmail),
            "to", List.of(Map.of("email", to)),
            "subject", subject,
            "htmlContent", htmlBody
        );
        sendViaBrevo(to, subject, payload);
    }

    private void sendViaBrevo(String to, String subject, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            restTemplate.postForEntity(BREVO_API_URL, new HttpEntity<>(payload, headers), String.class);
            log.info("Email '{}' enviado exitosamente a: {}", subject, to);
        } catch (Exception e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email: " + e.getMessage());
        }
    }
}