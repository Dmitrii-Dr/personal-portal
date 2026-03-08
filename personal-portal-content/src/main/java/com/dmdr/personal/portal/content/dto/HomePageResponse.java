package com.dmdr.personal.portal.content.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class HomePageResponse {
    private String welcomeMessage;
    private UUID welcomeRightMediaId;
    private UUID welcomeLeftMediaId;
    private UUID welcomeMobileMediaId;
    private List<UUID> welcomeArticleIds;
    private String aboutMessage;
    private UUID aboutMediaId;
    private String educationMessage;
    private UUID educationMediaId;
    private String reviewMessage;
    private List<UUID> reviewMediaIds;
    private Map<String, String> extendedParameters;
    private List<ContactDto> contact;
}
