package com.datn.quizai.quiz.controller;

import com.datn.quizai.quiz.service.CategoryService;
import com.datn.quizai.quiz.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh mục quiz — công khai để Guest lọc được danh sách quiz. */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Danh mục", description = "Danh mục quiz")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Danh sách danh mục (công khai)")
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }
}
