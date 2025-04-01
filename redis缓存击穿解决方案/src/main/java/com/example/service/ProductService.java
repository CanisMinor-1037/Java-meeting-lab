package com.example.service;

import com.example.entity.Product;

public interface ProductService {
    Product getProductById(Long id);
    Product getProductByIdWithCache(Long id);
    Product getProductByIdWithLock(Long id);
    Product getProductByIdWithRedissonLock(Long id);
} 