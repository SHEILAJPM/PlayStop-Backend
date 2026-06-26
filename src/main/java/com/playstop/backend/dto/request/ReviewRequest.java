package com.playstop.backend.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record ReviewRequest(
    @NotNull(message = "El ID de la cancha es obligatorio")
    UUID courtId,

    @NotNull(message = "La calificacion es obligatoria")
    @Min(value = 1, message = "La calificacion minima es 1")
    @Max(value = 5, message = "La calificacion maxima es 5")
    Integer rating,

    @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
    String comment,

    @Size(max = 5, message = "Máximo 5 fotos por reseña")
    List<String> photoUrls
) {}
