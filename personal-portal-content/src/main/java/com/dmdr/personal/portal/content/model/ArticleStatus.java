package com.dmdr.personal.portal.content.model;

public enum ArticleStatus {
    DRAFT,
    PUBLISHED,
    PRIVATE,
    ARCHIVED;

    public static boolean isUnpublished(ArticleStatus status) {
        return status != ArticleStatus.PUBLISHED;
    }
}

