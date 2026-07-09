package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BranchEmployeeResponse {
    private UUID id;
    private UUID employeeId;
    private String name;
    private String email;
    private String phone;
}
