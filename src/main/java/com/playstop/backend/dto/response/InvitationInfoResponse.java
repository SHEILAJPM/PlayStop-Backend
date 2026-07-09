package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitationInfoResponse {
    private String email;
    private String branchName;
    private String ownerName;
}
