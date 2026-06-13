package com.label4002.blog.controller;

import com.label4002.blog.dto.CategoryDTO;
import com.label4002.blog.dto.CreateCategoryRequest;
import com.label4002.blog.dto.UpdateCategoryRequest;
import com.label4002.blog.service.CategoryService;
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

@RestController
@RequestMapping("/api/v1")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<CategoryDTO> publicTree() {
        return categoryService.listTree(true);
    }

    @GetMapping("/categories/flat")
    public List<CategoryDTO> publicFlat() {
        return categoryService.listAllFlat(true);
    }

    @GetMapping("/categories/{id}")
    public CategoryDTO publicGet(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @GetMapping("/admin/categories")
    public List<CategoryDTO> adminTree() {
        return categoryService.listTree(false);
    }

    @GetMapping("/admin/categories/flat")
    public List<CategoryDTO> adminFlat() {
        return categoryService.listAllFlat(false);
    }

    @GetMapping("/admin/categories/{id}")
    public CategoryDTO adminGet(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/admin/categories/{id}")
    public CategoryDTO update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }

    @PutMapping("/admin/categories/{id}/toggle-enabled")
    public CategoryDTO toggleEnabled(@PathVariable Long id) {
        categoryService.toggleEnabled(id);
        return categoryService.getById(id);
    }
}
