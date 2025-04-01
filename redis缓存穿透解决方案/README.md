# Redis缓存穿透解决方案示例

本项目演示了Redis缓存穿透问题的几种常见解决方案。缓存穿透是指查询一个一定不存在的数据，由于缓存是不命中时被动写的，并且出于容错考虑，如果从存储层查不到数据则不写入缓存，这将导致这个不存在的数据每次请求都要到存储层去查询，失去了缓存的意义。

## 项目结构

```
redis缓存穿透解决方案/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           ├── RedisCachePenetrationApplication.java
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

## 缓存穿透解决方案

本项目实现了三种不同的缓存穿透解决方案：

### 1. 基础缓存方案

使用Redis实现基础的缓存功能，但这种方式无法有效防止缓存穿透。

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

### 2. 空值缓存方案

将空结果也进行缓存，但设置较短的过期时间，这样可以有效防止缓存穿透。

```java
public Product getProductByIdWithNullCache(Long id) {
    String cacheKey = CACHE_KEY_PREFIX + id;
    String nullCacheKey = NULL_CACHE_KEY_PREFIX + id;
    
    // 先检查空值缓存
    if (Boolean.TRUE.equals(redisTemplate.hasKey(nullCacheKey))) {
        return null;
    }
    
    // 检查正常缓存
    Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
    if (cachedProduct != null) {
        return (Product) cachedProduct;
    }

    // 缓存未命中，从数据库查询
    Product product = productRepository.findById(id).orElse(null);
    if (product != null) {
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    } else {
        redisTemplate.opsForValue().set(nullCacheKey, "", 5, TimeUnit.MINUTES);
    }
    return product;
}
```

### 3. 布隆过滤器方案

使用Redisson的布隆过滤器预先判断数据是否存在，可以有效防止缓存穿透。

```java
public Product getProductByIdWithBloomFilter(Long id) {
    String cacheKey = CACHE_KEY_PREFIX + id;
    
    // 先从缓存中获取
    Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
    if (cachedProduct != null) {
        return (Product) cachedProduct;
    }
    
    // 使用布隆过滤器判断
    if (!bloomFilter.contains(id)) {
        return null;
    }
    
    // 从数据库查询
    Product product = productRepository.findById(id).orElse(null);
    if (product != null) {
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    return product;
}
```

## 测试接口

项目提供了四个测试接口来演示不同的缓存穿透解决方案：

1. 无缓存方案
   ```
   GET /api/products/{id}
   ```

2. 基础缓存方案
   ```
   GET /api/products/cache/{id}
   ```

3. 空值缓存方案
   ```
   GET /api/products/null-cache/{id}
   ```

4. 布隆过滤器方案
   ```
   GET /api/products/bloom-filter/{id}
   ```

## 测试数据

- ID=1：存在商品数据
- ID=2：不存在商品数据
- ID=-1：无效ID（用于测试布隆过滤器）

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
2. 基础缓存方案：缓存未命中时会查询数据库，但无法防止缓存穿透
3. 空值缓存方案：可以有效防止缓存穿透，但会占用额外的缓存空间
4. 布隆过滤器方案：可以有效防止缓存穿透，且内存占用较小，但存在误判可能

## 注意事项

1. 本项目使用Redisson实现布隆过滤器，提供了更专业的实现
2. 空值缓存方案中的空值过期时间设置为5分钟
3. 正常数据的缓存过期时间设置为30分钟
4. 布隆过滤器的误判率设置为0.01，预期插入量为1000
5. 数据库表结构会自动创建，无需手动创建

## 接口文档

详细的接口文档请参考 `docs/api.md` 文件。

## 面试题目

### Redis的使用场景

根据简历上的业务进行回答，缓存，分布式锁

### 什么是缓存穿透，怎么解决

缓存穿透是查询一个不存在的数据，Mysql查询不到数据也不会写入缓存，导致每次请求都查询数据库

解决方案一：缓存空数据

解决方案二：布隆过滤器
