package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.dto.BlogPostDto;
import com.dmdr.personal.portal.model.BlogPost;
import com.dmdr.personal.portal.service.BlogPostService;
import com.dmdr.personal.portal.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/blog")
public class AdminBlogController {

    private final BlogPostService blogPostService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", blogPostService.listPublished());
        return "admin/blog-form"; // Placeholder view reuse
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("post", new BlogPostDto());
        return "admin/blog-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("post") BlogPostDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "admin/blog-form";
        }
        BlogPost post = BlogPost.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .slug(dto.getSlug() == null || dto.getSlug().isBlank() ? SlugUtil.toSlug(dto.getTitle()) : dto.getSlug())
                .content(dto.getContent())
                .build();
        if (dto.isPublish()) {
            post.setPublishedAt(Instant.now());
        }
        blogPostService.save(post);
        return "redirect:/admin/blog";
    }
}
