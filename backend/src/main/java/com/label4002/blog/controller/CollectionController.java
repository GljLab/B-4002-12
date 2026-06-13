package com.label4002.blog.controller;

import com.label4002.blog.dto.AddToAlbumRequest;
import com.label4002.blog.dto.BatchCollectionActionRequest;
import com.label4002.blog.dto.CollectionAlbumDTO;
import com.label4002.blog.dto.CollectionItemDTO;
import com.label4002.blog.dto.CreateAlbumRequest;
import com.label4002.blog.dto.PostFavoriteCountDTO;
import com.label4002.blog.dto.ReorderItemsRequest;
import com.label4002.blog.dto.UpdateAlbumRequest;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/albums")
    public List<CollectionAlbumDTO> listAlbums(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return collectionService.listAlbums(principal.getId());
    }

    @PostMapping("/albums")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionAlbumDTO createAlbum(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateAlbumRequest request) {
        return collectionService.createAlbum(principal.getId(), request);
    }

    @PutMapping("/albums/{albumId}")
    public CollectionAlbumDTO updateAlbum(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody UpdateAlbumRequest request) {
        return collectionService.updateAlbum(principal.getId(), albumId, request);
    }

    @DeleteMapping("/albums/{albumId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAlbum(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId) {
        collectionService.deleteAlbum(principal.getId(), albumId);
    }

    @GetMapping("/albums/{albumId}/items")
    public List<CollectionItemDTO> listAlbumItems(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId) {
        return collectionService.listAlbumItems(principal.getId(), albumId);
    }

    @PostMapping("/albums/{albumId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionItemDTO addItem(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody AddToAlbumRequest request) {
        return collectionService.addItem(principal.getId(), albumId, request);
    }

    @DeleteMapping("/albums/{albumId}/items/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public void removeItem(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long postId) {
        collectionService.removeItem(principal.getId(), albumId, postId);
    }

    @PutMapping("/albums/{albumId}/reorder")
    @ResponseStatus(HttpStatus.OK)
    public void reorderItems(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody ReorderItemsRequest request) {
        collectionService.reorderItems(principal.getId(), albumId, request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public void batchAction(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody BatchCollectionActionRequest request) {
        collectionService.batchAction(principal.getId(), request);
    }

    @GetMapping("/albums/{albumId}/export")
    public ResponseEntity<byte[]> exportAlbum(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "markdown") String format) {
        String content = collectionService.exportAlbumMarkdown(principal.getId(), albumId);
        String filename;
        String contentType;
        byte[] body;

        if ("pdf".equalsIgnoreCase(format)) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
            filename = "album-" + albumId + ".pdf";
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
            htmlBuilder.append("<style>body{font-family:sans-serif;padding:40px;line-height:1.8}");
            htmlBuilder.append("h1{border-bottom:2px solid #2d6df6;padding-bottom:8px}");
            htmlBuilder.append("h2{margin-top:32px;color:#334a68}blockquote{border-left:4px solid #2d6df6;padding:8px 16px;background:#e7f0ff;margin:12px 0}");
            htmlBuilder.append("hr{border:none;border-top:1px solid #d7e1ee;margin:24px 0}</style>");
            htmlBuilder.append("</head><body>");
            String md = content;
            String[] lines = md.split("\n");
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    htmlBuilder.append("<h1>").append(escapeHtml(line.substring(2))).append("</h1>");
                } else if (line.startsWith("## ")) {
                    htmlBuilder.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
                } else if (line.startsWith("> ")) {
                    htmlBuilder.append("<blockquote>").append(escapeHtml(line.substring(2))).append("</blockquote>");
                } else if (line.equals("---")) {
                    htmlBuilder.append("<hr>");
                } else if (!line.isBlank()) {
                    htmlBuilder.append("<p>").append(escapeHtml(line)).append("</p>");
                }
            }
            htmlBuilder.append("</body></html>");
            body = htmlBuilder.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            contentType = "text/markdown;charset=utf-8";
            filename = "album-" + albumId + ".md";
            body = content.getBytes(StandardCharsets.UTF_8);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    @GetMapping("/public/{shareToken}")
    public CollectionAlbumDTO getPublicAlbum(@PathVariable String shareToken) {
        return collectionService.getPublicAlbum(shareToken);
    }

    @GetMapping("/public/{shareToken}/items")
    public List<CollectionItemDTO> getPublicAlbumItems(@PathVariable String shareToken) {
        return collectionService.getPublicAlbumItems(shareToken);
    }

    @GetMapping("/ranking")
    public List<PostFavoriteCountDTO> getFavoriteRanking(
            @RequestParam(defaultValue = "10") int top) {
        return collectionService.getFavoriteRanking(Math.min(Math.max(top, 1), 50));
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
