package com.dmdr.personal.portal.content.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaEntityResponse {

    private UUID mediaId;
    private String fileUrl;
    private String fileType;
    private String altText;
    private UUID uploadedById;
    private OffsetDateTime createdAt;
    private String imageUrl;
}

