package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    Optional<Tag> findByTagId(UUID tagId);

}

