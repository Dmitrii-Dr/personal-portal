package com.dmdr.personal.portal.content.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class HomePageResponse {
    private String welcomeMessage;
    private UUID welcomeMediaId;
    private String aboutMessage;
    private UUID aboutMediaId;
    private String educationMessage;
    private UUID educationMediaId;
}

