package com.playstop.backend.service;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;
    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
            ));
            enabled = true;
            log.info("Cloudinary inicializado para cloud: {}", cloudName);
        } else {
            log.info("Cloudinary deshabilitado: variables CLOUDINARY_* no configuradas");
        }
    }

    public boolean isEnabled() { return enabled; }

    public String upload(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "folder",         "playstop/courts",
                "resource_type",  "image",
                "transformation", "q_auto,f_auto,w_800,c_limit"
            )
        );
        return (String) result.get("secure_url");
    }

    public void delete(String publicId) {
        if (!enabled) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("No se pudo eliminar imagen de Cloudinary: {}", e.getMessage());
        }
    }
}
