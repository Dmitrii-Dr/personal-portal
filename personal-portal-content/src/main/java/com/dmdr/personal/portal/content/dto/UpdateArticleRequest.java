package com.dmdr.personal.portal.content.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateArticleRequest extends AbstractArticleRequest {
    
    private Set<UUID> mediaIds;
}

