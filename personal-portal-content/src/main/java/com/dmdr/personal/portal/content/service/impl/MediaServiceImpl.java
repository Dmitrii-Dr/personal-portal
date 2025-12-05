package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.model.MediaEntity;
import com.dmdr.personal.portal.content.repository.HomePageRepository;
import com.dmdr.personal.portal.content.repository.MediaRepository;
import com.dmdr.personal.portal.content.service.MediaService;
import com.dmdr.personal.portal.content.service.s3.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final S3Service s3Service;
    private final HomePageRepository homePageRepository;

    public MediaServiceImpl(MediaRepository mediaRepository, S3Service s3Service, 
                           HomePageRepository homePageRepository) {
        this.mediaRepository = mediaRepository;
        this.s3Service = s3Service;
        this.homePageRepository = homePageRepository;
    }

    @Override
    @Transactional
    public MediaEntity createMedia(MediaEntity media) {
        return mediaRepository.save(media);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaEntity createMediaWithS3Upload(
            MediaEntity mediaEntity,
            String originalFilename,
            byte[] originalFileBytes,
            byte[] thumbnailBytes) {
        
        // Generate S3 keys: originalFilename-UUID8chars for original, thumbnail/prefix for thumbnail
        String uuidHash = UUID.randomUUID().toString().substring(24); // Last 8 characters
        String s3Key = originalFilename + "-" + uuidHash;
        String thumbnailS3Key = "thumbnail/" + s3Key;
        
        // Set the fileUrl before saving
        mediaEntity.setFileUrl(s3Key);
        
        // Step 1: Save to database first (within transaction)
        MediaEntity savedMedia = mediaRepository.save(mediaEntity);
        log.debug("Media entity saved to database with ID: {}", savedMedia.getMediaId());
        
        boolean originalUploaded = false;
        boolean thumbnailUploaded = false;
        
        try {
            // Step 2: Upload original file to S3
            s3Service.uploadFile(s3Key, originalFileBytes);
            originalUploaded = true;
            log.debug("Original file uploaded to S3 with key: {}", s3Key);
            
            // Step 3: Upload thumbnail to S3 (thumbnails are required)
            s3Service.uploadFile(thumbnailS3Key, thumbnailBytes);
            thumbnailUploaded = true;
            log.debug("Thumbnail uploaded to S3 with key: {}", thumbnailS3Key);
            
            return savedMedia;
        } catch (RuntimeException e) {
            // Cleanup: Delete any uploaded S3 files
            try {
                if (thumbnailUploaded) {
                    s3Service.deleteFile(thumbnailS3Key);
                    log.debug("Cleaned up thumbnail from S3: {}", thumbnailS3Key);
                }
                if (originalUploaded) {
                    s3Service.deleteFile(s3Key);
                    log.debug("Cleaned up original file from S3: {}", s3Key);
                }
            } catch (RuntimeException cleanupException) {
                log.error("Failed to cleanup S3 files after upload failure", cleanupException);
                // Continue to throw original exception
            }
            
            // Throw exception to trigger transaction rollback
            log.error("S3 upload failed for key: {}. Transaction will rollback.", s3Key, e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<MediaEntity> findById(UUID mediaId) {
        if (mediaId == null) {
            return Optional.empty();
        }
        return mediaRepository.findByMediaId(mediaId);
    }

    @Override
    public List<MediaEntity> findByIds(Set<UUID> mediaIds) {
        if (CollectionUtils.isEmpty(mediaIds)) {
            return List.of();
        }
        return mediaRepository.findAllById(mediaIds);
    }

    @Override
    public List<MediaEntity> findAll() {
        return mediaRepository.findAll();
    }

    @Override
    public Page<MediaEntity> findAll(Pageable pageable) {
        // If no sort is specified, add default sort by createdAt descending (newest first)
        Pageable pageableWithSort = pageable;
        if (!pageable.getSort().isSorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
            pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                defaultSort
            );
        }
        return mediaRepository.findAll(pageableWithSort);
    }

    @Override
    @Transactional
    public MediaEntity updateMedia(UUID mediaId, MediaEntity media) {
        MediaEntity existingMedia = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media with id " + mediaId + " not found"));

        // Update fields
        if (media.getFileUrl() != null) {
            existingMedia.setFileUrl(media.getFileUrl());
        }
        if (media.getFileType() != null) {
            existingMedia.setFileType(media.getFileType());
        }
        if (media.getAltText() != null) {
            existingMedia.setAltText(media.getAltText());
        }
        if (media.getUploadedById() != null) {
            existingMedia.setUploadedById(media.getUploadedById());
        }

        return mediaRepository.save(existingMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("Media id cannot be null");
        }
        
        MediaEntity mediaEntity = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media with id " + mediaId + " not found"));
        
        // Check if media is used by any articles
        if (mediaEntity.getArticles() != null && !mediaEntity.getArticles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete media with id " + mediaId + " because it is being used by " 
                    + mediaEntity.getArticles().size() + " article(s)");
        }
        
        // Check if media is used by HomePage
        if (homePageRepository.existsByMediaId(mediaId)) {
            throw new IllegalArgumentException(
                    "Cannot delete media with id " + mediaId + " because it is being used by the home page");
        }
        
        mediaRepository.deleteById(mediaId);
    }

    @Override
    @Transactional
    public void deleteMediaWithS3Cleanup(UUID mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("Media id cannot be null");
        }
        
        // Step 1: Fetch and validate media entity
        MediaEntity mediaEntity = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media with id " + mediaId + " not found"));
        
        // Step 2: Validate that media is not used by articles (before any deletions)
        if (mediaEntity.getArticles() != null && !mediaEntity.getArticles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete media with id " + mediaId + " because it is being used by " 
                    + mediaEntity.getArticles().size() + " article(s)");
        }
        
        // Check if media is used by HomePage
        if (homePageRepository.existsByMediaId(mediaId)) {
            throw new IllegalArgumentException(
                    "Cannot delete media with id " + mediaId + " because it is being used by the home page");
        }
        
        // Step 3: Delete from S3 (after validation passes)
        String key = mediaEntity.getFileUrl();
        String thumbnailKey = "thumbnail/" + key;
        
        // Delete thumbnail from S3
        s3Service.deleteFile(thumbnailKey);
        log.debug("Thumbnail deleted from S3 with key: {}", thumbnailKey);
        
        // Delete original image from S3
        s3Service.deleteFile(key);
        log.debug("Original file deleted from S3 with key: {}", key);
        
        // Step 4: Delete from database
        mediaRepository.deleteById(mediaId);
        log.debug("Media entity deleted from database with ID: {}", mediaId);
    }
}

