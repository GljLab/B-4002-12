package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthUserDTO;
import com.label4002.blog.dto.LoginRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthUserDTO login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest httpServletRequest,
                             HttpServletResponse httpServletResponse) {
        return authService.loginSession(request, httpServletRequest, httpServletResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return new MessageResponse("已退出登录");
    }

    @GetMapping("/me")
    public AuthUserDTO me(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.me(principal);
    }
}
