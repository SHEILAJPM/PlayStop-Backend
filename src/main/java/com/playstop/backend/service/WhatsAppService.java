package com.playstop.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppService {

    @Value("${whatsapp.cloud.token:}")
    private String accessToken;

    @Value("${whatsapp.cloud.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud.api-version:v23.0}")
    private String apiVersion;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (accessToken != null && !accessToken.isBlank() &&
            phoneNumberId != null && !phoneNumberId.isBlank()) {
            enabled = true;
            log.info("WhatsApp service initialized via Meta Cloud API");
        } else {
            log.info("WhatsApp service disabled: WHATSAPP_CLOUD_TOKEN / WHATSAPP_PHONE_NUMBER_ID not set");
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
            String to = phone.replaceAll("[^0-9]", "");
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", body)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("WhatsApp send failed for {}: HTTP {} - {}", phone, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("WhatsApp send failed for {}: {}", phone, e.getMessage());
        }
    }
}
