package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.TagDto;
import com.dmdr.personal.portal.content.model.Tag;
import com.dmdr.personal.portal.content.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tag")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagDto>> getAllTags() {
        List<TagDto> tags = tagService.findAll().stream().map(tag -> {
            TagDto dto = new TagDto();
            dto.setTagId(tag.getTagId());
            dto.setName(tag.getName());
            return dto;
        }).toList();
        return ResponseEntity.ok(tags);
    }

    @PostMapping
    public ResponseEntity<TagDto> createTag(@Valid @RequestBody TagDto request) {
        Tag tag = new Tag();
        tag.setName(request.getName());
        Tag created = tagService.createTag(tag);

        TagDto response = new TagDto();
        response.setTagId(created.getTagId());
        response.setName(created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<TagDto> updateTag(@PathVariable("tagId") UUID tagId,
                                            @Valid @RequestBody TagDto request) {
        Tag tag = new Tag();
        tag.setName(request.getName());
        Tag updated = tagService.updateTag(tagId, tag);

        TagDto response = new TagDto();
        response.setTagId(updated.getTagId());
        response.setName(updated.getName());
        return ResponseEntity.ok(response);
    }
}


