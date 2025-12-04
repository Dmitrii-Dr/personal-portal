package com.dmdr.personal.portal.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import com.dmdr.personal.portal.content.dto.MediaEntityMapper;
import com.dmdr.personal.portal.content.dto.MediaEntityResponse;
import com.dmdr.personal.portal.content.dto.PaginatedResponse;
import com.dmdr.personal.portal.content.model.MediaEntity;
import java.util.stream.Collectors;
import com.dmdr.personal.portal.content.service.MediaService;
import com.dmdr.personal.portal.content.service.s3.S3Service;
import com.dmdr.personal.portal.service.CurrentUserService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMediaEntityController {
    private final S3Service s3Service;
    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    public AdminMediaEntityController(
            S3Service s3Service,
            MediaService mediaService,
            CurrentUserService currentUserService) {
        this.s3Service = s3Service;
        this.mediaService = mediaService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/media/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String key = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            s3Service.uploadFile(key, file.getBytes());
            
            // Create MediaEntity in database
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setFileUrl(key);
            mediaEntity.setFileType(determineFileType(file));
            mediaEntity.setAltText(null);
            mediaEntity.setUploadedById(currentUserService.getCurrentUser().getId());
            
            MediaEntity savedMedia = mediaService.createMedia(mediaEntity);

            return ResponseEntity.ok(Map.of("mediaId", savedMedia.getMediaId().toString()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file."));
        }
    }

    private String determineFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            return contentType;
        }
        // Fallback to file extension
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            return switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                case "svg" -> "image/svg+xml";
                default -> "application/octet-stream";
            };
        }
        return "application/octet-stream";
    }

    @GetMapping("/media")
    public ResponseEntity<PaginatedResponse<MediaEntityResponse>> getMediaGallery(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MediaEntity> mediaPage = mediaService.findAll(pageable);
        
        // Map entities to DTOs manually to ensure all items are included
        java.util.List<MediaEntityResponse> content = mediaPage.getContent().stream()
                .map(MediaEntityMapper::toResponse)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        
        // Create PaginatedResponse with the mapped content
        PaginatedResponse<MediaEntityResponse> paginatedResponse = new PaginatedResponse<>(
                content,
                mediaPage.getNumber(),
                mediaPage.getSize(),
                mediaPage.getTotalElements(),
                mediaPage.getTotalPages(),
                mediaPage.isFirst(),
                mediaPage.isLast(),
                content.size()
        );
        
        return ResponseEntity.ok(paginatedResponse);
    }

    @DeleteMapping("/media/image/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable("mediaId") UUID mediaId) {
        try {
            MediaEntity mediaEntity = mediaService.findById(mediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Media with id " + mediaId + " not found"));
            
            String key = mediaEntity.getFileUrl();
            
            // Delete from S3
            s3Service.deleteFile(key);
            
            // Delete from database
            mediaService.deleteMedia(mediaId);
            
            return ResponseEntity.ok(Map.of("message", "File deleted successfully", "mediaId", mediaId.toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
    
}
