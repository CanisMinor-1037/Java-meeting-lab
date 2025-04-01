package com.example.service.impl;

import com.example.entity.Product;
import com.example.repository.ProductRepository;
import com.example.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final RBloomFilter<Long> bloomFilter;
    private static final String CACHE_KEY_PREFIX = "product:";
    private static final String NULL_CACHE_KEY_PREFIX = "null:product:";
    private static final long CACHE_EXPIRE_TIME = 30; // 缓存过期时间（分钟）

    public ProductServiceImpl(RedisTemplate<String, Object> redisTemplate, 
                            ProductRepository productRepository,
                            RBloomFilter<Long> bloomFilter) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.bloomFilter = bloomFilter;
    }

    @Override
    public Product getProductById(Long id) {
        log.info("查询商品ID: {}", id);
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product getProductByIdWithCache(Long id) {
        log.info("使用基础缓存策略查询商品ID: {}", id);
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 先从缓存中获取
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("从缓存中获取到商品: {}", cachedProduct);
            return (Product) cachedProduct;
        } else {
            log.info("发生缓存穿透,id:{}", id);
        }

        // 缓存未命中，从数据库查询
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            // 将查询结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("将商品存入缓存: {}", product);
        }

        return product;
    }

    @Override
    public Product getProductByIdWithNullCache(Long id) {
        log.info("使用空值缓存策略查询商品ID: {}", id);
        String cacheKey = CACHE_KEY_PREFIX + id;
        String nullCacheKey = NULL_CACHE_KEY_PREFIX + id;
        
        // 先检查空值缓存
        if (Boolean.TRUE.equals(redisTemplate.hasKey(nullCacheKey))) {
            log.info("命中空值缓存，商品不存在");
            return null;
        }
        
        // 检查正常缓存
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("从缓存中获取到商品: {}", cachedProduct);
            return (Product) cachedProduct;
        }

        // 缓存未命中，从数据库查询
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            // 将查询结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("将商品存入缓存: {}", product);
        } else {
            // 将空值标记存入缓存
            redisTemplate.opsForValue().set(nullCacheKey, "", 5, TimeUnit.MINUTES);
            log.info("将空值标记存入缓存");
        }
        return product;
    }

    @Override
    public Product getProductByIdWithBloomFilter(Long id) {
        log.info("使用布隆过滤器策略查询商品ID: {}", id);
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 先从缓存中获取
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("从缓存中获取到商品: {}", cachedProduct);
            return (Product) cachedProduct;
        }
        
        // 使用布隆过滤器判断
        if (!bloomFilter.contains(id)) {
            log.info("布隆过滤器判断商品不存在");
            return null;
        }
        
        // 从数据库查询
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("将商品存入缓存: {}", product);
        }
        return product;
    }

    @Override
    public void initializeBloomFilter() {
        log.info("初始化布隆过滤器");
        // 将所有商品ID加入布隆过滤器
        productRepository.findAll().forEach(product -> 
            bloomFilter.add(product.getId())
        );
        log.info("布隆过滤器初始化完成");
    }
} 