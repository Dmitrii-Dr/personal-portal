package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.MediaEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MediaService {

    MediaEntity createMedia(MediaEntity media);

    /**
     * Creates media with file uploads to S3.
     * This method ensures atomicity: if S3 upload fails, the database transaction is rolled back.
     * Generates S3 keys internally using the original filename and a UUID hash.
     * 
     * @param mediaEntity The media entity to save (without fileUrl set)
     * @param originalFilename The original filename to use for key generation
     * @param originalFileBytes The original image bytes
     * @param thumbnailBytes The thumbnail bytes (must not be null - thumbnails are required)
     * @return The saved MediaEntity
     * @throws RuntimeException if S3 upload fails (will trigger transaction rollback)
     */
    MediaEntity createMediaWithS3Upload(
            MediaEntity mediaEntity,
            String originalFilename,
            byte[] originalFileBytes,
            byte[] thumbnailBytes);

    Optional<MediaEntity> findById(UUID mediaId);

    List<MediaEntity> findByIds(Set<UUID> mediaIds);

    List<MediaEntity> findAll();

    Page<MediaEntity> findAll(Pageable pageable);

    MediaEntity updateMedia(UUID mediaId, MediaEntity media);

    void deleteMedia(UUID mediaId);

    /**
     * Deletes media with S3 cleanup.
     * This method validates that the media is not used by articles before deleting from S3 and database.
     * 
     * @param mediaId The media ID to delete
     * @throws IllegalArgumentException if media is not found or is being used by articles
     * @throws RuntimeException if S3 deletion fails
     */
    void deleteMediaWithS3Cleanup(UUID mediaId);

}

