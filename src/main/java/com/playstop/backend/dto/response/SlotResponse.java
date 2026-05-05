package com.playstop.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlotResponse {
    private int hour;
    private String label;
    private boolean available;
}