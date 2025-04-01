package com.example.service.impl;

import com.example.entity.Product;
import com.example.repository.ProductRepository;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟
    private static final long LOCK_EXPIRE_TIME = 10; // 10秒

    @Override
    public Product getProductById(Long id) {
        log.info("直接查询数据库，商品ID: {}", id);
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product getProductByIdWithCache(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        log.info("使用缓存查询，商品ID: {}", id);

        // 先从缓存中获取
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("缓存命中，商品ID: {}", id);
            return (Product) cachedProduct;
        }

        // 缓存未命中，从数据库查询
        log.info("缓存未命中，从数据库查询，商品ID: {}", id);
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("将商品数据存入缓存，商品ID: {}", id);
        }
        return product;
    }

    @Override
    public Product getProductByIdWithLock(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        String lockKey = "lock:" + cacheKey;
        log.info("使用Redis分布式锁查询，商品ID: {}", id);

        // 先从缓存中获取
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("缓存命中，商品ID: {}", id);
            return (Product) cachedProduct;
        }

        // 尝试获取锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                cachedProduct = redisTemplate.opsForValue().get(cacheKey);
                if (cachedProduct != null) {
                    log.info("双重检查缓存命中，商品ID: {}", id);
                    return (Product) cachedProduct;
                }

                // 从数据库查询
                log.info("从数据库查询，商品ID: {}", id);
                Product product = productRepository.findById(id).orElse(null);
                if (product != null) {
                    redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
                    log.info("将商品数据存入缓存，商品ID: {}", id);
                }
                return product;
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey);
                log.info("释放分布式锁，商品ID: {}", id);
            }
        } else {
            // 等待一段时间后重试
            try {
                Thread.sleep(100);
                return getProductByIdWithLock(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    @Override
    public Product getProductByIdWithRedissonLock(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        String lockKey = "lock:" + cacheKey;
        log.info("使用Redisson分布式锁查询，商品ID: {}", id);

        // 先从缓存中获取
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("缓存命中，商品ID: {}", id);
            return (Product) cachedProduct;
        }

        // 获取Redisson分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(LOCK_EXPIRE_TIME, TimeUnit.SECONDS)) {
                try {
                    // 双重检查
                    cachedProduct = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedProduct != null) {
                        log.info("双重检查缓存命中，商品ID: {}", id);
                        return (Product) cachedProduct;
                    }

                    // 从数据库查询
                    log.info("从数据库查询，商品ID: {}", id);
                    Product product = productRepository.findById(id).orElse(null);
                    if (product != null) {
                        redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
                        log.info("将商品数据存入缓存，商品ID: {}", id);
                    }
                    return product;
                } finally {
                    // 释放锁
                    lock.unlock();
                    log.info("释放Redisson分布式锁，商品ID: {}", id);
                }
            } else {
                // 等待一段时间后重试
                try {
                    Thread.sleep(100);
                    return getProductByIdWithRedissonLock(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
} 