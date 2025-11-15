package com.dmdr.personal.portal.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessLevelResponse {

    private String accessLevel;
    private String email;
    private String message;
}

