package com.playstop.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${firebase.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) return;
        try {
            InputStream stream = resolveCredentials();
            if (stream == null) {
                log.warn("FCM disabled — provee firebase-service-account.json o la env var FIREBASE_SERVICE_ACCOUNT_JSON");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }

    private InputStream resolveCredentials() throws IOException {
        // Prioridad 1: env var con JSON directo (producción en Render)
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        // Prioridad 2: archivo local (desarrollo)
        if (Files.exists(Paths.get(serviceAccountPath))) {
            return new FileInputStream(serviceAccountPath);
        }
        return null;
    }

    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty() || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message.Builder builder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());
            if (data != null) data.forEach(builder::putData);
            FirebaseMessaging.getInstance().sendAsync(builder.build());
        } catch (Exception e) {
            log.warn("FCM send failed for token {}: {}", fcmToken.substring(0, 10) + "...", e.getMessage());
        }
    }
}
