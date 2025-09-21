package top.kloping.core.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 日志工具类
 * 提供统一的日志记录和追踪功能
 *
 * @author github kloping
 * @since 2025/9/21
 */
@Slf4j
public class LoggingUtils {
    
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String USER_ID_KEY = "userId";
    
    /**
     * 设置请求ID用于日志追踪
     */
    public static void setRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }
    
    /**
     * 设置会话ID用于日志追踪
     */
    public static void setSessionId(String sessionId) {
        if (sessionId != null) {
            MDC.put(SESSION_ID_KEY, sessionId);
        }
    }
    
    /**
     * 设置用户ID用于日志追踪
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        }
    }
    
    /**
     * 清除所有MDC上下文
     */
    public static void clearMDC() {
        MDC.clear();
    }
    
    /**
     * 记录性能日志
     */
    public static void logPerformance(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Performance: {} completed in {} ms", operation, duration);
    }
    
    /**
     * 记录性能日志（带详细信息）
     */
    public static void logPerformance(String operation, long startTime, Object... details) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Performance: {} completed in {} ms, details: {}", operation, duration, details);
    }
    
    /**
     * 记录API调用开始
     */
    public static void logApiCallStart(String apiName, Object request) {
        log.debug("API Call Start: {} with request: {}", apiName, request);
    }
    
    /**
     * 记录API调用结束
     */
    public static void logApiCallEnd(String apiName, Object response, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.debug("API Call End: {} completed in {} ms with response: {}", apiName, duration, response);
    }
    
    /**
     * 记录错误信息（带上下文）
     */
    public static void logError(String operation, Throwable throwable, Object... context) {
        log.error("Error in {}: {}, context: {}", operation, throwable.getMessage(), context, throwable);
    }
    
    /**
     * 记录警告信息（带上下文）
     */
    public static void logWarning(String operation, String message, Object... context) {
        log.warn("Warning in {}: {}, context: {}", operation, message, context);
    }
    
    /**
     * 记录配置加载信息
     */
    public static void logConfigurationLoaded(String configName, Object config) {
        log.info("Configuration loaded: {} = {}", configName, config);
    }
    
    /**
     * 记录组件初始化
     */
    public static void logComponentInitialization(String componentName, String status) {
        log.info("Component initialization: {} - {}", componentName, status);
    }
    
    /**
     * 获取当前请求ID
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }
    
    /**
     * 获取当前会话ID
     */
    public static String getCurrentSessionId() {
        return MDC.get(SESSION_ID_KEY);
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        return MDC.get(USER_ID_KEY);
    }
}