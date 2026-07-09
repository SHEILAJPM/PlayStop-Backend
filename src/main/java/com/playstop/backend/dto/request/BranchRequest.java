package com.playstop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String address;
    private String city;
    private String district;
}
