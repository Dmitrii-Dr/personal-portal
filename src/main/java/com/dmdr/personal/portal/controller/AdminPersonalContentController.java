package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.model.PersonalContent;
import com.dmdr.personal.portal.repository.PersonalContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/content")
public class AdminPersonalContentController {

    private final PersonalContentRepository contentRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("contents", contentRepository.findAll());
        return "admin/content";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("content", new PersonalContent());
        return "admin/content-form";
    }

    @PostMapping
    public String save(@ModelAttribute PersonalContent content) {
        contentRepository.save(content);
        return "redirect:/admin/content";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        contentRepository.deleteById(id);
        return "redirect:/admin/content";
    }
}
