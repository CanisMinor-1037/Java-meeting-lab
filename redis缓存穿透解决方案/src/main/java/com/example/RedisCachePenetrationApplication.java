package com.example;

import com.example.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisCachePenetrationApplication implements CommandLineRunner {
    private final ProductService productService;

    public RedisCachePenetrationApplication(ProductService productService) {
        this.productService = productService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisCachePenetrationApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 初始化布隆过滤器
        productService.initializeBloomFilter();
    }
} 