package com.label4002.blog.controller;

import com.label4002.blog.dto.ImageDTO;
import com.label4002.blog.dto.ImageRenameRequest;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.ImageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/author/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public List<ImageDTO> listMyImages(@AuthenticationPrincipal AppUserPrincipal principal) {
        return imageService.listByAuthor(principal.getId());
    }

    @GetMapping("/{id}")
    public ImageDTO getImage(@PathVariable Long id) {
        return imageService.getImage(id);
    }

    @PutMapping("/{id}/rename")
    public ImageDTO renameImage(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id,
                                @RequestBody ImageRenameRequest request) {
        return imageService.renameImage(principal.getId(), id, request.originalName());
    }

    @DeleteMapping("/{id}")
    public void deleteImage(@AuthenticationPrincipal AppUserPrincipal principal,
                            @PathVariable Long id) {
        imageService.deleteImage(principal.getId(), id);
    }

    @GetMapping("/{id}/references")
    public List<PostSummaryDTO> getImageReferences(@PathVariable Long id) {
        return imageService.getReferencingPosts(id);
    }
}
