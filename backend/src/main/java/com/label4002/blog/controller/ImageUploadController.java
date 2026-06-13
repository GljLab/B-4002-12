package com.label4002.blog.controller;

import com.label4002.blog.dto.ImageDTO;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.ImageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
public class ImageUploadController {

    private final ImageService imageService;

    public ImageUploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/image")
    public ImageDTO uploadImage(@AuthenticationPrincipal AppUserPrincipal principal,
                                @RequestParam("file") MultipartFile file) {
        return imageService.uploadImage(principal.getId(), file);
    }
}
