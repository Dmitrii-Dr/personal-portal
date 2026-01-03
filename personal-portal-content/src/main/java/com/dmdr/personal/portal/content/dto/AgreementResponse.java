package com.dmdr.personal.portal.content.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AgreementResponse {

    private UUID id;
    private String name;
    private String content;
    private String slug;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long version;
}
