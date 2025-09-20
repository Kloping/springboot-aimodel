package top.kloping.core.ai;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kloping.core.ai.mcp.McpClient;
import top.kloping.core.ai.mcp.McpClientProperties;
import top.kloping.core.ai.service.AiRequestModel;
import top.kloping.core.ai.service.AiRequestModelImpl;
import top.kloping.core.ai.service.ChatContext;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

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
    public AiRequestModel defaultAiRequestModel(AiModelProperties properties) {
        ChatContext chatContext = new ChatContext();
        chatContext.setModel(properties.getModel());
        chatContext.setMax(properties.getMax());
        chatContext.setMessages(new LinkedList<>());
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
        String server = properties.getServer();
        String path = properties.getPath();
        String url = server + path;
        int i = url.indexOf("//");
        String proot = url.substring(0, i + 2);
        String urlPath = url.substring(i + 2);
        urlPath = urlPath.replaceAll("//", "/");
        final String finalUrl = proot + urlPath;
        AiRequestModelImpl model = new AiRequestModelImpl(finalUrl, properties, chatContext, client);
        model.addMcpServer(mcpClient);
        return model;
    }
}