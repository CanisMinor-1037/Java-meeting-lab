# Redis缓存击穿解决方案示例

本项目演示了Redis缓存击穿问题的几种常见解决方案。缓存击穿是指热点数据在缓存过期的一瞬间，有大量请求同时访问这个数据，导致所有请求都打到数据库上。

## 项目结构

```
redis缓存击穿解决方案/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           ├── RedisCacheBreakdownApplication.java
│       │           ├── config/
│       │           │   ├── RedisConfig.java
│       │           │   └── RedissonConfig.java
│       │           ├── controller/
│       │           │   └── ProductController.java
│       │           ├── entity/
│       │           │   └── Product.java
│       │           ├── repository/
│       │           │   └── ProductRepository.java
│       │           └── service/
│       │               ├── ProductService.java
│       │               └── impl/
│       │                   └── ProductServiceImpl.java
│       └── resources/
│           ├── application.yml
│           └── db/
│               └── init.sql
├── docs/
│   └── api.md
└── pom.xml
```

## 技术栈

- Spring Boot 3.4.3
- Redis
- MySQL
- Spring Data JPA
- Redisson
- Lombok
- Java 21

## 环境要求

1. JDK 21
2. Redis服务器（默认配置：localhost:6379）
3. MySQL服务器（默认配置：localhost:3306/javalab）
4. Maven 3.6+

## 数据库配置

MySQL配置在`application.yml`中：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/javalab?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: deesdees
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    show-sql: true
    hibernate:
      ddl-auto: update
```

## Redis配置

Redis配置在`application.yml`中：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 10000
```

## 缓存击穿解决方案

本项目实现了三种不同的缓存击穿解决方案：

### 1. 基础缓存方案

使用Redis实现基础的缓存功能，但这种方式无法有效防止缓存击穿。

```java
public Product getProductByIdWithCache(Long id) {
    String cacheKey = CACHE_KEY_PREFIX + id;
    // 先从缓存中获取
    Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
    if (cachedProduct != null) {
        return (Product) cachedProduct;
    }
    // 缓存未命中，从数据库查询
    Product product = productRepository.findById(id).orElse(null);
    if (product != null) {
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    return product;
}
```

### 2. Redis分布式锁方案

使用Redis的分布式锁来防止缓存击穿。

```java
public Product getProductByIdWithLock(Long id) {
    String cacheKey = CACHE_KEY_PREFIX + id;
    String lockKey = "lock:" + cacheKey;
    
    // 先从缓存中获取
    Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
    if (cachedProduct != null) {
        return (Product) cachedProduct;
    }

    // 尝试获取锁
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
    if (Boolean.TRUE.equals(locked)) {
        try {
            // 双重检查
            cachedProduct = redisTemplate.opsForValue().get(cacheKey);
            if (cachedProduct != null) {
                return (Product) cachedProduct;
            }

            // 从数据库查询
            Product product = productRepository.findById(id).orElse(null);
            if (product != null) {
                redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            }
            return product;
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
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
```

### 3. Redisson分布式锁方案

使用Redisson提供的分布式锁来防止缓存击穿，这种方式更加可靠。

```java
public Product getProductByIdWithRedissonLock(Long id) {
    String cacheKey = CACHE_KEY_PREFIX + id;
    String lockKey = "lock:" + cacheKey;
    
    // 先从缓存中获取
    Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
    if (cachedProduct != null) {
        return (Product) cachedProduct;
    }

    // 获取Redisson分布式锁
    RLock lock = redissonClient.getLock(lockKey);
    if (lock.tryLock(LOCK_EXPIRE_TIME, TimeUnit.SECONDS)) {
        try {
            // 双重检查
            cachedProduct = redisTemplate.opsForValue().get(cacheKey);
            if (cachedProduct != null) {
                return (Product) cachedProduct;
            }

            // 从数据库查询
            Product product = productRepository.findById(id).orElse(null);
            if (product != null) {
                redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            }
            return product;
        } finally {
            // 释放锁
            lock.unlock();
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
}
```

## 测试接口

项目提供了四个测试接口来演示不同的缓存击穿解决方案：

1. 无缓存方案
   ```
   GET /api/products/{id}
   ```

2. 基础缓存方案
   ```
   GET /api/products/cache/{id}
   ```

3. Redis分布式锁方案
   ```
   GET /api/products/lock/{id}
   ```

4. Redisson分布式锁方案
   ```
   GET /api/products/redisson-lock/{id}
   ```

## 测试数据

- ID=1：存在商品数据
- ID=2：不存在商品数据

## 运行项目

1. 确保MySQL服务器已启动，并执行数据库初始化脚本：
   ```bash
   mysql -u root -pdeesdees < src/main/resources/db/init.sql
   ```

2. 确保Redis服务器已启动

3. 在项目根目录执行：
   ```bash
   mvn spring-boot:run
   ```

4. 访问测试接口：
   ```
   http://localhost:8080/api/products/{id}
   ```

## 性能对比

1. 无缓存方案：每次请求都会查询数据库
2. 基础缓存方案：缓存未命中时会查询数据库，但无法防止缓存击穿
3. Redis分布式锁方案：可以有效防止缓存击穿，但实现相对复杂
4. Redisson分布式锁方案：可以有效防止缓存击穿，且实现更加可靠

## 注意事项

1. 本项目使用Redisson实现分布式锁，提供了更专业的实现
2. 正常数据的缓存过期时间设置为30分钟
3. 分布式锁的过期时间设置为10秒
4. 数据库表结构会自动创建，无需手动创建

## 接口文档

详细的接口文档请参考 `docs/api.md` 文件。

## 面试题目

### Redis的使用场景

根据简历上的业务进行回答，缓存，分布式锁

### 什么是缓存击穿，怎么解决

缓存击穿是热点数据在缓存过期的一瞬间，有大量请求同时访问这个数据，导致所有请求都打到数据库上

解决方案一：互斥锁

解决方案二：热点数据永不过期 