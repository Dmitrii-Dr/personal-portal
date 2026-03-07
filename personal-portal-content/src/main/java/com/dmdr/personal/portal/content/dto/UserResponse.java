package com.dmdr.personal.portal.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Short avatarId;
    @JsonProperty("isVerified")
    private boolean isVerified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
