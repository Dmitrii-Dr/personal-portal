package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.model.Tag;
import com.dmdr.personal.portal.content.repository.TagRepository;
import com.dmdr.personal.portal.content.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public Tag createTag(Tag tag) {
        // Check if tag with slug already exists
        if (tag.getSlug() != null && tagRepository.findBySlug(tag.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Tag with slug " + tag.getSlug() + " already exists");
        }
        // Check if tag with name already exists
        if (tag.getName() != null && tagRepository.findByName(tag.getName()).isPresent()) {
            throw new IllegalArgumentException("Tag with name " + tag.getName() + " already exists");
        }
        return tagRepository.save(tag);
    }

    @Override
    public Optional<Tag> findById(UUID tagId) {
        if (tagId == null) {
            return Optional.empty();
        }
        return tagRepository.findByTagId(tagId);
    }

    @Override
    public List<Tag> findByIds(Set<UUID> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return List.of();
        }
        return tagRepository.findAllById(tagIds);
    }

    @Override
    public Optional<Tag> findBySlug(String slug) {
        return tagRepository.findBySlug(slug);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name);
    }

    @Override
    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    @Override
    @Transactional
    public Tag updateTag(UUID tagId, Tag tag) {
        Tag existingTag = tagRepository.findByTagId(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag with id " + tagId + " not found"));

        // Check if slug is being changed and if the new slug already exists
        if (tag.getSlug() != null && !tag.getSlug().equals(existingTag.getSlug())) {
            if (tagRepository.findBySlug(tag.getSlug()).isPresent()) {
                throw new IllegalArgumentException("Tag with slug " + tag.getSlug() + " already exists");
            }
        }

        // Check if name is being changed and if the new name already exists
        if (tag.getName() != null && !tag.getName().equals(existingTag.getName())) {
            if (tagRepository.findByName(tag.getName()).isPresent()) {
                throw new IllegalArgumentException("Tag with name " + tag.getName() + " already exists");
            }
        }

        // Update fields
        if (tag.getName() != null) {
            existingTag.setName(tag.getName());
        }
        if (tag.getSlug() != null) {
            existingTag.setSlug(tag.getSlug());
        }

        return tagRepository.save(existingTag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagId) {
        if (tagId == null) {
            throw new IllegalArgumentException("Tag id cannot be null");
        }
        if (!tagRepository.existsById(tagId)) {
            throw new IllegalArgumentException("Tag with id " + tagId + " not found");
        }
        tagRepository.deleteById(tagId);
    }
}

