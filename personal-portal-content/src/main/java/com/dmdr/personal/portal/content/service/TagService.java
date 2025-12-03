package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.Tag;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TagService {

    Tag createTag(Tag tag);

    Optional<Tag> findById(UUID tagId);

    List<Tag> findByIds(Set<UUID> tagIds);

    Optional<Tag> findByName(String name);

    List<Tag> findAll();

    Tag updateTag(UUID tagId, Tag tag);

    void deleteTag(UUID tagId);

}

