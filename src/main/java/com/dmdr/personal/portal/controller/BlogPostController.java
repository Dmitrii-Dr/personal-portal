package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.BlogPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/blog")
public class BlogPostController {

    private final BlogPostService blogPostService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", blogPostService.listPublished());
        return "blog/list";
    }

    @GetMapping("/{slug}")
    public String view(@PathVariable String slug, Model model) {
        model.addAttribute("post",
            blogPostService.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Post not found")));
        return "blog/post";
    }
}
