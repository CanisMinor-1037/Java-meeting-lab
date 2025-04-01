package com.example.service;

import com.example.entity.Product;

public interface ProductService {
    Product getProductById(Long id);
    Product getProductByIdWithCache(Long id);
    Product getProductByIdWithNullCache(Long id);
    Product getProductByIdWithBloomFilter(Long id);
    void initializeBloomFilter();
} 