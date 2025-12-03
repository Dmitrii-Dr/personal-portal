package com.dmdr.personal.portal.content.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateHomePageRequest {
    private String welcomeMessage;
    private UUID welcomeMediaId;
    private List<UUID> welcomeArticleIds;
    private String aboutMessage;
    private UUID aboutMediaId;
    private String educationMessage;
    private UUID educationMediaId;
    private String reviewMessage;
    private List<UUID> reviewMediaIds;
    private List<ContactDto> contact;
}

