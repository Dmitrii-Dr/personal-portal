package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.MediaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {

    Optional<MediaEntity> findByMediaId(UUID mediaId);

}

