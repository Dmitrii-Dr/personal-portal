package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.dto.RegistrationDto;
import com.dmdr.personal.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    // Authentication handled by Spring Security's UsernamePasswordAuthenticationFilter

    @GetMapping("/login")
    public String login() {
        return "login";
    }


    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registration", new RegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registration") RegistrationDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.registration", "Passwords do not match");
            return "register";
        }
        userService.register(dto);
        return "redirect:/login?registered";
    }
}
