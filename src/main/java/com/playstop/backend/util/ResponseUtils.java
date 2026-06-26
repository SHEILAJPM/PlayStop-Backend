package com.playstop.backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {

    private ResponseUtils() {}

    public static ResponseEntity<Map<String, Object>> success(String message) {
        return ResponseEntity.ok(buildBody(true, message, null));
    }

    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return ResponseEntity.ok(buildBody(true, message, data));
    }

    public static ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(buildBody(false, message, null));
    }

    private static Map<String, Object> buildBody(boolean success, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", success);
        body.put("message", message);
        if (data != null) body.put("data", data);
        return body;
    }
}
