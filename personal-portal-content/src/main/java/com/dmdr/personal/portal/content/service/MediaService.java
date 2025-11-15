package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.MediaEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MediaService {

    MediaEntity createMedia(MediaEntity media);

    Optional<MediaEntity> findById(UUID mediaId);

    List<MediaEntity> findByIds(Set<UUID> mediaIds);

    List<MediaEntity> findAll();

    MediaEntity updateMedia(UUID mediaId, MediaEntity media);

    void deleteMedia(UUID mediaId);

}

