package com.playstop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "La contrasena actual es obligatoria")
    String contrasenaActual,

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    String nuevaContrasena,

    @NotBlank(message = "La confirmacion es obligatoria")
    String confirmarContrasena
) {}
