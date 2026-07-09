package com.playstop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterEmployeeRequest {

    @NotBlank(message = "El token de invitación es obligatorio")
    private String token;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe incluir al menos una letra y un número")
    private String password;

    private String phone;
}
