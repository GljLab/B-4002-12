package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthUserDTO;
import com.label4002.blog.dto.LoginRequest;
import com.label4002.blog.dto.TokenRefreshRequest;
import com.label4002.blog.dto.TokenResponse;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/token")
public class TokenController {

    private final AuthService authService;

    public TokenController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loginToken(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refreshToken(request);
    }

    @GetMapping("/secure/me")
    public AuthUserDTO me(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.me(principal);
    }
}
