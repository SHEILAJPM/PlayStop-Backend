package com.playstop.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteEmployeeRequest {

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    private String email;
}
