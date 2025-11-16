package com.dmdr.personal.portal.content.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminArticleResponse extends ArticleResponse {
    private List<UserResponse> users;
}


