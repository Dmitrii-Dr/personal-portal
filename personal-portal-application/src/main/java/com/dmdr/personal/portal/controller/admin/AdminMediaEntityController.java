package com.dmdr.personal.portal.controller.admin;

import lombok.extern.slf4j.Slf4j;
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
import com.dmdr.personal.portal.content.service.ThumbnailService;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.controller.util.ImageFileValidator;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminMediaEntityController {
    private final MediaService mediaService;
    private final CurrentUserService currentUserService;
    private final ThumbnailService thumbnailService;

    public AdminMediaEntityController(
            MediaService mediaService,
            CurrentUserService currentUserService,
            ThumbnailService thumbnailService) {
        this.mediaService = mediaService;
        this.currentUserService = currentUserService;
        this.thumbnailService = thumbnailService;
    }

    @PostMapping("/media/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String validationError = ImageFileValidator.validateImageFile(file);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "File name is required."));
            }
            
            byte[] fileBytes = file.getBytes();
            String fileType = determineFileType(file);
            
            // Step 1: Generate thumbnail first (validates it works before any DB/S3 operations)
           
            
            if (thumbnailService.isSvg(fileType, originalFilename)) {
                return ResponseEntity.badRequest()
                .body(Map.of("error", "SVG files are not supported for thumbnails."));
            }
                
            byte[] thumbnailBytes = thumbnailService.generateThumbnail(fileBytes);
            if (thumbnailBytes == null) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to generate thumbnail."));
            }
            
            // Step 2: Create MediaEntity (without fileUrl - will be set in service)
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setFileType(fileType);
            mediaEntity.setAltText(null);
            mediaEntity.setUploadedById(currentUserService.getCurrentUser().getId());
            
            // Step 3: Save to DB and upload to S3 atomically
            // If S3 upload fails, DB transaction will rollback automatically
            MediaEntity savedMedia = mediaService.createMediaWithS3Upload(
                    mediaEntity,
                    originalFilename,
                    fileBytes,
                    thumbnailBytes);

            return ResponseEntity.ok(Map.of("mediaId", savedMedia.getMediaId().toString()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to read file."));
        } catch (RuntimeException e) {
            // S3 upload failure - transaction already rolled back
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
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
            // Validation and deletion (including S3 cleanup) are handled in the service layer
            mediaService.deleteMediaWithS3Cleanup(mediaId);
            
            return ResponseEntity.ok(Map.of("message", "File deleted successfully", "mediaId", mediaId.toString()));
        } catch (IllegalArgumentException e) {
            // Return 400 Bad Request if media is used by articles or not found
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
    
}
