package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.content.model.MediaEntity;
import com.dmdr.personal.portal.content.service.MediaService;
import com.dmdr.personal.portal.content.service.s3.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/media")
public class MediaEntityController {
    
    private final S3Service s3Service;
    private final MediaService mediaService;

    public MediaEntityController(S3Service s3Service, MediaService mediaService) {
        this.s3Service = s3Service;
        this.mediaService = mediaService;
    }

    @GetMapping("/image/{mediaId}")
    public ResponseEntity<byte[]> getImage(@PathVariable("mediaId") UUID mediaId) {
        MediaEntity mediaEntity = mediaService.findById(mediaId)
                .orElse(null);
        
        if (mediaEntity == null) {
            return ResponseEntity.notFound().build();
        }
        
        String key = mediaEntity.getFileUrl();
        byte[] imageData = s3Service.downloadFile(key);
        
        if (imageData == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(detectContentType(mediaEntity.getFileType(), key));
        headers.setContentLength(imageData.length);
        
        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }

    @GetMapping("/image/{mediaId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable("mediaId") UUID mediaId) {
        MediaEntity mediaEntity = mediaService.findById(mediaId)
                .orElse(null);
        
        if (mediaEntity == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Calculate thumbnail key: "thumbnail/" + original fileUrl
        String thumbnailKey = "thumbnail/" + mediaEntity.getFileUrl();
        byte[] thumbnailData = s3Service.downloadFile(thumbnailKey);
        
        if (thumbnailData == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        // Thumbnails are always JPEG, so set content type accordingly
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(thumbnailData.length);
        
        return new ResponseEntity<>(thumbnailData, headers, HttpStatus.OK);
    }

    private MediaType detectContentType(String fileType, String key) {
        // First try to use the fileType from MediaEntity
        if (fileType != null && !fileType.isEmpty()) {
            try {
                return MediaType.parseMediaType(fileType);
            } catch (Exception e) {
                // Fall through to extension-based detection
            }
        }
        
        // Fallback to extension-based detection
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lowerKey.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (lowerKey.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        } else if (lowerKey.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        } else {
            // Default to JPEG for .jpg, .jpeg, or unknown extensions
            return MediaType.IMAGE_JPEG;
        }
    }
}
