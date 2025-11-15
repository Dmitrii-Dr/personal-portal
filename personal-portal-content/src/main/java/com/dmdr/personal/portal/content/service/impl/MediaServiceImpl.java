package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.model.MediaEntity;
import com.dmdr.personal.portal.content.repository.MediaRepository;
import com.dmdr.personal.portal.content.service.MediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;

    public MediaServiceImpl(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    @Transactional
    public MediaEntity createMedia(MediaEntity media) {
        return mediaRepository.save(media);
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
        if (!mediaRepository.existsById(mediaId)) {
            throw new IllegalArgumentException("Media with id " + mediaId + " not found");
        }
        mediaRepository.deleteById(mediaId);
    }
}

