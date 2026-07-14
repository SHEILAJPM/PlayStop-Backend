package com.playstop.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        whatsAppService = new WhatsAppService();
        ReflectionTestUtils.setField(whatsAppService, "http", httpClient);
    }

    private void enable() {
        ReflectionTestUtils.setField(whatsAppService, "accessToken", "token123");
        ReflectionTestUtils.setField(whatsAppService, "phoneNumberId", "phone123");
        ReflectionTestUtils.setField(whatsAppService, "apiVersion", "v23.0");
        ReflectionTestUtils.invokeMethod(whatsAppService, "init");
    }

    @Test
    void sendReservationConfirmation_credentialsNotConfigured_doesNotCallHttpClient() {
        ReflectionTestUtils.invokeMethod(whatsAppService, "init");

        whatsAppService.sendReservationConfirmation(
                "999999999", "Juan", "Cancha 1", "2026-07-20", "10:00 - 11:00", "50");

        verifyNoInteractions(httpClient);
    }

    @Test
    void sendReservationConfirmation_blankPhone_doesNotCallHttpClient() {
        enable();

        whatsAppService.sendReservationConfirmation(
                "", "Juan", "Cancha 1", "2026-07-20", "10:00 - 11:00", "50");

        verifyNoInteractions(httpClient);
    }

    @Test
    void sendReservationConfirmation_enabled_postsToMetaGraphApi() throws Exception {
        enable();
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.<String>send(any(), any())).thenReturn(httpResponse);

        whatsAppService.sendReservationConfirmation(
                "999999999", "Juan", "Cancha 1", "2026-07-20", "10:00 - 11:00", "50");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("https://graph.facebook.com/v23.0/phone123/messages", captor.getValue().uri().toString());
        assertEquals("Bearer token123", captor.getValue().headers().firstValue("Authorization").orElse(null));
    }

    @Test
    void sendReservationReminder_credentialsNotConfigured_doesNotCallHttpClient() {
        ReflectionTestUtils.invokeMethod(whatsAppService, "init");

        whatsAppService.sendReservationReminder("999999999", "Juan", "Cancha 1", "10:00 - 11:00");

        verifyNoInteractions(httpClient);
    }

    @Test
    void sendReservationReminder_enabled_postsToMetaGraphApi() throws Exception {
        enable();
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.<String>send(any(), any())).thenReturn(httpResponse);

        whatsAppService.sendReservationReminder("999999999", "Juan", "Cancha 1", "10:00 - 11:00");

        verify(httpClient).send(any(), any());
    }

    @Test
    void send_apiReturnsError_doesNotThrow() throws Exception {
        enable();
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("Unauthorized");
        when(httpClient.<String>send(any(), any())).thenReturn(httpResponse);

        assertDoesNotThrow(() -> whatsAppService.sendReservationConfirmation(
                "999999999", "Juan", "Cancha 1", "2026-07-20", "10:00 - 11:00", "50"));
    }

    @Test
    void send_httpClientThrows_doesNotPropagateException() throws Exception {
        enable();
        when(httpClient.send(any(), any())).thenThrow(new IOException("network down"));

        assertDoesNotThrow(() -> whatsAppService.sendReservationConfirmation(
                "999999999", "Juan", "Cancha 1", "2026-07-20", "10:00 - 11:00", "50"));
    }
}
