package com.example.controller;

import com.example.entity.Product;
import com.example.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        // 无缓存，直接查询数据库
        return productService.getProductById(id);
    }

    @GetMapping("/cache/{id}")
    public Product getProductWithCache(@PathVariable Long id) {
        // 使用Spring Cache注解实现缓存
        return productService.getProductByIdWithCache(id);
    }

    @GetMapping("/null-cache/{id}")
    public Product getProductWithNullCache(@PathVariable Long id) {
        // 使用空值缓存方案
        return productService.getProductByIdWithNullCache(id);
    }

    @GetMapping("/bloom-filter/{id}")
    public Product getProductWithBloomFilter(@PathVariable Long id) {
        // 使用布隆过滤器方案
        return productService.getProductByIdWithBloomFilter(id);
    }
} 