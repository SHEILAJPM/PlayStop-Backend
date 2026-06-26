package com.playstop.backend.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String fromNumber;

    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isBlank() &&
            authToken  != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            enabled = true;
            log.info("WhatsApp service initialized via Twilio");
        } else {
            log.info("WhatsApp service disabled: TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN not set");
        }
    }

    public void sendReservationConfirmation(String phone, String clientName,
                                            String courtName, String date,
                                            String slot, String totalAmount) {
        if (!enabled || phone == null || phone.isBlank()) return;
        String body = String.format(
            "✅ *Reserva confirmada en PlayStop*\n\n" +
            "Hola %s 👋\n\n" +
            "📍 *Cancha:* %s\n" +
            "📅 *Fecha:* %s\n" +
            "🕐 *Hora:* %s\n" +
            "💰 *Total:* S/ %s\n\n" +
            "Recuerda llegar 10 minutos antes. ¡Que disfrutes el partido! ⚽",
            clientName, courtName, date, slot, totalAmount);
        send(phone, body);
    }

    public void sendReservationReminder(String phone, String clientName,
                                        String courtName, String slot) {
        if (!enabled || phone == null || phone.isBlank()) return;
        String body = String.format(
            "⏰ *Recordatorio PlayStop*\n\n" +
            "Hola %s, tu reserva en *%s* es HOY a las *%s*.\n" +
            "¡Nos vemos en la cancha! ⚽🏃",
            clientName, courtName, slot);
        send(phone, body);
    }

    private void send(String phone, String body) {
        try {
            String to = phone.startsWith("whatsapp:") ? phone : "whatsapp:+" + phone.replaceAll("[^0-9]", "");
            Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber), body).create();
        } catch (Exception e) {
            log.warn("WhatsApp send failed for {}: {}", phone, e.getMessage());
        }
    }
}
