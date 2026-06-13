package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthorDTO;
import com.label4002.blog.dto.CreateAuthorRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.UpdateAuthorRequest;
import com.label4002.blog.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/authors")
public class AdminAuthorController {

    private final AuthorService authorService;

    public AdminAuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public List<AuthorDTO> listAuthors() {
        return authorService.listAuthors();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO createAuthor(@Valid @RequestBody CreateAuthorRequest request) {
        return authorService.createAuthor(request);
    }

    @GetMapping("/{id}")
    public AuthorDTO getAuthor(@PathVariable Long id) {
        return authorService.getAuthor(id);
    }

    @PutMapping("/{id}")
    public AuthorDTO updateAuthor(@PathVariable Long id,
                                  @Valid @RequestBody UpdateAuthorRequest request) {
        return authorService.updateAuthor(id, request);
    }

    @PutMapping("/{id}/reset-password")
    public MessageResponse resetPassword(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        authorService.resetPassword(id, body.get("newPassword"));
        return new MessageResponse("密码已重置");
    }

    @PutMapping("/{id}/disable")
    public MessageResponse disableAuthor(@PathVariable Long id) {
        authorService.disableAuthor(id);
        return new MessageResponse("作者已禁用");
    }

    @PutMapping("/{id}/enable")
    public MessageResponse enableAuthor(@PathVariable Long id) {
        authorService.enableAuthor(id);
        return new MessageResponse("作者已启用");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Long id,
                             @RequestParam(required = false) Long transferTo) {
        authorService.deleteAuthor(id, transferTo);
    }
}
