package com.playstop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 500, message = "Máximo 500 caracteres")
    private String content;
}
