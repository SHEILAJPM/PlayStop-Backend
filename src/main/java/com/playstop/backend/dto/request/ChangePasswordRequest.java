package com.playstop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "La contrasena actual es obligatoria")
    String contrasenaActual,

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contrasena debe incluir al menos una letra y un numero")
    String nuevaContrasena,

    @NotBlank(message = "La confirmacion es obligatoria")
    String confirmarContrasena
) {}
