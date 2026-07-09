package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BranchResponse {
    private UUID id;
    private String name;
    private String address;
    private String city;
    private String district;
    private boolean active;
    private long courtCount;
    private long employeeCount;
}
