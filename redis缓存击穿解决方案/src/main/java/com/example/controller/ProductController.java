package com.example.controller;

import com.example.entity.Product;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/cache/{id}")
    public Product getProductWithCache(@PathVariable Long id) {
        return productService.getProductByIdWithCache(id);
    }

    @GetMapping("/lock/{id}")
    public Product getProductWithLock(@PathVariable Long id) {
        return productService.getProductByIdWithLock(id);
    }

    @GetMapping("/redisson-lock/{id}")
    public Product getProductWithRedissonLock(@PathVariable Long id) {
        return productService.getProductByIdWithRedissonLock(id);
    }
} 