package top.kloping.core.ai.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 简单的内存缓存管理器
 * 提供基本的缓存功能和TTL支持
 *
 * @author github kloping
 * @since 2025/9/21
 */
@Slf4j
@Component
public class SimpleCacheManager {
    
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "cache-cleanup");
        t.setDaemon(true);
        return t;
    });
    
    public SimpleCacheManager() {
        // 定期清理过期缓存
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        private final Object value;
        private final long expireTime;
        
        public CacheEntry(Object value, long ttlMs) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlMs;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
        
        public Object getValue() {
            return value;
        }
    }
    
    /**
     * 添加缓存项
     */
    public void put(String key, Object value, long ttlMs) {
        if (key == null || value == null) {
            return;
        }
        cache.put(key, new CacheEntry(value, ttlMs));
        log.debug("Cached item with key: {}, TTL: {}ms", key, ttlMs);
    }
    
    /**
     * 添加缓存项（默认5分钟TTL）
     */
    public void put(String key, Object value) {
        put(key, value, TimeUnit.MINUTES.toMillis(5));
    }
    
    /**
     * 获取缓存项
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (key == null) {
            return null;
        }
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Removed expired cache entry: {}", key);
            return null;
        }
        
        try {
            return type.cast(entry.getValue());
        } catch (ClassCastException e) {
            log.warn("Cache type mismatch for key {}: expected {}, got {}", 
                    key, type.getSimpleName(), entry.getValue().getClass().getSimpleName());
            return null;
        }
    }
    
    /**
     * 获取缓存项（通用方法）
     */
    public Object get(String key) {
        return get(key, Object.class);
    }
    
    /**
     * 移除缓存项
     */
    public void remove(String key) {
        if (key != null) {
            cache.remove(key);
            log.debug("Removed cache entry: {}", key);
        }
    }
    
    /**
     * 检查缓存项是否存在且未过期
     */
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared {} cache entries", size);
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 清理过期缓存项
     */
    private void cleanupExpiredEntries() {
        int expiredCount = 0;
        for (String key : cache.keySet()) {
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.debug("Cleaned up {} expired cache entries", expiredCount);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("Cache stats - Size: %d entries", cache.size());
    }
    
    /**
     * 销毁缓存管理器
     */
    public void destroy() {
        log.info("Destroying cache manager");
        clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Cache manager destroyed");
    }
}