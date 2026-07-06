package com.playstop.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayoutRequestDto {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto mínimo de retiro es S/ 1.00")
    private BigDecimal amount;

    @NotBlank(message = "El método de pago es obligatorio")
    @Pattern(regexp = "YAPE_PLIN|BANK", message = "Método de pago inválido")
    private String method;

    @NotBlank(message = "El nombre del titular es obligatorio")
    private String holderName;

    // Requeridos si method = YAPE_PLIN
    private String phoneNumber;

    // Requeridos si method = BANK
    private String bankName;
    private String accountType;
    private String accountNumber;
}
