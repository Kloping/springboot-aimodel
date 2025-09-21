package top.kloping.core.ai.pool;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * HTTP连接池管理器
 * 提供统一的HTTP客户端管理和资源清理
 *
 * @author github kloping
 * @since 2025/9/21
 */
@Slf4j
@Component
public class HttpClientPoolManager implements DisposableBean {
    
    private volatile OkHttpClient httpClient;
    private volatile ConnectionPool connectionPool;
    
    /**
     * 获取共享的HTTP客户端实例
     */
    public synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            createHttpClient();
        }
        return httpClient;
    }
    
    /**
     * 创建HTTP客户端
     */
    private void createHttpClient() {
        // 创建连接池
        connectionPool = new ConnectionPool(
                10,           // 最大空闲连接数
                5,            // 保持连接时间
                TimeUnit.MINUTES  // 时间单位
        );
        
        // 创建HTTP客户端
        httpClient = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
                
        log.info("HTTP client pool initialized with connection pool: {}", connectionPool);
    }
    
    /**
     * 获取连接池统计信息
     */
    public String getPoolStats() {
        if (connectionPool == null) {
            return "Connection pool not initialized";
        }
        return String.format("Connection pool stats - Idle: %d, Total: %d", 
                connectionPool.idleConnectionCount(), 
                connectionPool.connectionCount());
    }
    
    /**
     * 清理空闲连接
     */
    public void evictIdleConnections() {
        if (connectionPool != null) {
            connectionPool.evictAll();
            log.debug("Evicted all idle connections from pool");
        }
    }
    
    /**
     * 资源清理
     */
    @Override
    public void destroy() throws Exception {
        log.info("Destroying HTTP client pool manager");
        
        if (httpClient != null) {
            // 关闭客户端
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            
            try {
                if (!httpClient.dispatcher().executorService().awaitTermination(30, TimeUnit.SECONDS)) {
                    httpClient.dispatcher().executorService().shutdownNow();
                }
            } catch (InterruptedException e) {
                httpClient.dispatcher().executorService().shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (connectionPool != null) {
            connectionPool.evictAll();
        }
        
        log.info("HTTP client pool manager destroyed");
    }
}