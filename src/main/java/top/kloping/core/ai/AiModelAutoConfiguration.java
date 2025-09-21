package top.kloping.core.ai;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import top.kloping.core.ai.mcp.McpClient;
import top.kloping.core.ai.mcp.McpClientProperties;
import top.kloping.core.ai.pool.HttpClientPoolManager;
import top.kloping.core.ai.service.AiRequestModel;
import top.kloping.core.ai.service.AiRequestModelImpl;
import top.kloping.core.ai.service.ChatContext;
import top.kloping.core.ai.validation.ConfigurationValidator;

import java.util.concurrent.TimeUnit;

/**
 * AI模型自动配置类
 * 负责创建和配置AI模型相关的Bean
 *
 * @author github kloping
 * @since 2025/9/19
 */
@Slf4j
@Configuration
@ConditionalOnClass(AiModelProperties.class)
public class AiModelAutoConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "top.kloping.ai")
    @ConditionalOnProperty(prefix = "top.kloping.ai", name = "server", matchIfMissing = false)
    public AiModelProperties aiModelProperties() {
        return new AiModelProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "top.kloping.mcp")
    @ConditionalOnProperty(prefix = "top.kloping.mcp", name = "server", matchIfMissing = false)
    public McpClientProperties mcpClientProperties() {
        return new McpClientProperties();
    }


    private McpClient mcpClient;
    
    @Autowired(required = false)
    private HttpClientPoolManager httpClientPoolManager;

    @Bean
    @ConditionalOnBean(McpClientProperties.class)
    public McpClient defaultMcpClient(McpClientProperties properties) {
        mcpClient = new McpClient(properties);
        new Thread(() -> {
            try {
                mcpClient.initialize();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }).start();
        try {
            mcpClient.getCdl().await(properties.getHeartbeat(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return mcpClient;
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn(value = "defaultMcpClient")
    public AiRequestModel defaultAiRequestModel(AiModelProperties properties) {
        // 验证配置
        ConfigurationValidator.validateAiModelProperties(properties);
        
        ChatContext chatContext = new ChatContext();
        chatContext.setModel(properties.getModel());
        chatContext.setMax(properties.getMax());
        
        // 使用连接池管理器或创建默认客户端
        final OkHttpClient client = httpClientPoolManager != null 
                ? httpClientPoolManager.getHttpClient()
                : createDefaultHttpClient();
                
        String finalUrl = buildFinalUrl(properties);
        
        AiRequestModelImpl model = new AiRequestModelImpl(finalUrl, properties, chatContext, client);
        if (mcpClient != null) {
            model.addMcpServer(mcpClient);
        }
        return model;
    }
    
    /**
     * 创建默认HTTP客户端（在没有连接池管理器时使用）
     */
    private OkHttpClient createDefaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 构建最终URL，处理重复斜杠问题
     */
    private String buildFinalUrl(AiModelProperties properties) {
        String server = properties.getServer();
        String path = properties.getPath();
        
        if (server == null || server.trim().isEmpty()) {
            throw new IllegalArgumentException("AI模型服务地址不能为空");
        }
        
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("请求路径不能为空");
        }
        
        // 处理server末尾的斜杠
        if (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        
        // 处理path开头的斜杠
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        String url = server + path;
        
        // 规范化URL，处理重复斜杠
        int protocolIndex = url.indexOf("//");
        if (protocolIndex != -1) {
            String protocol = url.substring(0, protocolIndex + 2);
            String urlPath = url.substring(protocolIndex + 2);
            urlPath = urlPath.replaceAll("//+", "/");
            url = protocol + urlPath;
        }
        
        log.info("Built final URL: {}", url);
        return url;
    }
}