package com.dmdr.personal.portal.content.dto;

import com.dmdr.personal.portal.content.model.MediaEntity;

public class MediaEntityMapper {

    public static MediaEntityResponse toResponse(MediaEntity mediaEntity) {
        if (mediaEntity == null) {
            return null;
        }

        MediaEntityResponse response = new MediaEntityResponse();
        response.setMediaId(mediaEntity.getMediaId());
        response.setFileUrl(mediaEntity.getFileUrl());
        response.setFileType(mediaEntity.getFileType());
        response.setAltText(mediaEntity.getAltText());
        response.setUploadedById(mediaEntity.getUploadedById());
        response.setCreatedAt(mediaEntity.getCreatedAt());
        
        // Set imageUrl to the S3 key (fileUrl)
        response.setImageUrl(mediaEntity.getFileUrl());
        
        return response;
    }
}

