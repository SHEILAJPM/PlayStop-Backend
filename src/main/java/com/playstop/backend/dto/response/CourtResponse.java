package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CourtResponse {
    private UUID id;
    private String name;
    private String sportType;
    private BigDecimal pricePerHour;
    private String address;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private boolean active;
    private String ownerName;
    private String ownerEmail;
    private String city;
    private String district;
    private Double averageRating;
    private long reviewCount;
    private String slug;
    private UUID branchId;
    private String branchName;
}
