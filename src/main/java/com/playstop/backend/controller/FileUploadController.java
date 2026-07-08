package com.playstop.backend.controller;

import com.playstop.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !EXTENSION_BY_CONTENT_TYPE.containsKey(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten imágenes JPEG, PNG, WEBP o GIF"));
        }
        if (!matchesImageSignature(file, contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no es una imagen válida"));
        }

        if (cloudinaryService.isEnabled()) {
            String url = cloudinaryService.upload(file);
            return ResponseEntity.ok(Map.of("url", url));
        }

        // Fallback: disco local. El nombre de archivo se genera por completo
        // en el servidor (UUID + extensión fija según el Content-Type real);
        // el nombre original que envía el cliente nunca se usa para construir
        // la ruta, para no permitir path traversal vía ese campo.
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String ext = EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, ".jpg");
        String filename = UUID.randomUUID() + ext;
        Path target = uploadPath.resolve(filename).normalize();
        if (!target.getParent().equals(uploadPath)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre de archivo inválido"));
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.ok(Map.of("url", baseUrl + "/uploads/" + filename));
    }

    /**
     * Valida los primeros bytes del archivo contra la firma real del formato
     * (magic numbers), en vez de confiar solo en el Content-Type que declara
     * el cliente en la solicitud.
     */
    private boolean matchesImageSignature(MultipartFile file, String contentType) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        }
        if (read < 4) return false;

        return switch (contentType) {
            case "image/jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            case "image/gif" -> header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8';
            case "image/webp" -> read >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
