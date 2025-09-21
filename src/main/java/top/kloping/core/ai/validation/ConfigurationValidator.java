package top.kloping.core.ai.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import top.kloping.core.ai.AiModelProperties;
import top.kloping.core.ai.exception.AiModelException;
import top.kloping.core.ai.mcp.McpClientProperties;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 配置验证器
 * 提供统一的配置参数验证逻辑
 *
 * @author github kloping
 * @since 2025/9/21
 */
@Slf4j
public class ConfigurationValidator {
    
    /**
     * 验证AI模型配置
     */
    public static void validateAiModelProperties(AiModelProperties properties) {
        if (properties == null) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "AI模型配置不能为空");
        }
        
        // 验证服务地址
        validateServerUrl(properties.getServer(), "AI模型服务地址");
        
        // 验证令牌
        validateToken(properties.getToken(), "AI模型访问令牌");
        
        // 验证模型名称
        if (!StringUtils.hasText(properties.getModel())) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "AI模型名称不能为空");
        }
        
        // 验证路径
        if (!StringUtils.hasText(properties.getPath())) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "API路径不能为空");
        }
        
        // 验证数值参数
        validateNumericParameters(properties);
        
        log.info("AI model configuration validation passed for model: {}", properties.getModel());
    }
    
    /**
     * 验证MCP客户端配置
     */
    public static void validateMcpClientProperties(McpClientProperties properties) {
        if (properties == null) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "MCP客户端配置不能为空");
        }
        
        // 验证服务地址
        validateServerUrl(properties.getServer(), "MCP服务地址");
        
        // 验证令牌
        validateToken(properties.getToken(), "MCP访问令牌");
        
        // 验证端点
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "MCP端点不能为空");
        }
        
        // 验证心跳间隔
        if (properties.getHeartbeat() <= 0) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "心跳间隔必须大于0秒");
        }
        
        log.info("MCP client configuration validation passed for server: {}", properties.getServer());
    }
    
    /**
     * 验证服务器URL
     */
    private static void validateServerUrl(String serverUrl, String fieldName) {
        if (!StringUtils.hasText(serverUrl)) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    fieldName + "不能为空");
        }
        
        try {
            URL url = new URL(serverUrl);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                        fieldName + "必须使用HTTP或HTTPS协议");
            }
        } catch (MalformedURLException e) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    fieldName + "格式无效: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证令牌格式
     */
    public static void validateToken(String token, String tokenType) {
        if (!StringUtils.hasText(token)) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    tokenType + "不能为空");
        }
        
        // 简单的令牌格式验证
        if (token.length() < 10) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    tokenType + "长度过短，可能无效");
        }
        
        // 检查是否包含明显的占位符
        String upperToken = token.toUpperCase();
        if (upperToken.contains("XXXX") || upperToken.contains("YOUR") || 
            upperToken.contains("REPLACE") || upperToken.contains("PLACEHOLDER")) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    tokenType + "包含占位符，请使用真实的令牌");
        }
    }
    
    /**
     * 验证AI模型的数值参数
     */
    private static void validateNumericParameters(AiModelProperties properties) {
        // 验证最大记忆长度
        if (properties.getMax() != null && properties.getMax() <= 0) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "最大记忆长度必须大于0");
        }
        
        // 验证温度参数
        if (properties.getTemperature() != null) {
            float temp = properties.getTemperature();
            if (temp < 0.0f || temp > 2.0f) {
                throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                        "采样温度必须在0.0-2.0之间，当前值: " + temp);
            }
        }
        
        // 验证top_k参数
        if (properties.getTop_k() != null && properties.getTop_k() <= 0) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "top_k参数必须大于0，当前值: " + properties.getTop_k());
        }
        
        // 验证最大令牌数
        if (properties.getMax_tokens() != null && properties.getMax_tokens() <= 0) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "最大生成令牌数必须大于0，当前值: " + properties.getMax_tokens());
        }
        
        // 验证最大输入令牌数
        if (properties.getMax_input_tokens() != null && properties.getMax_input_tokens() <= 0) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                    "最大输入令牌数必须大于0，当前值: " + properties.getMax_input_tokens());
        }
        
        // 验证种子参数
        if (properties.getSeed() != null) {
            int seed = properties.getSeed();
            if (seed < 0 || seed >= Math.pow(2, 31)) {
                throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION, 
                        "seed参数必须在0到2^31-1之间，当前值: " + seed);
            }
        }
    }
    
    /**
     * 获取配置建议信息
     */
    public static String getConfigurationSuggestions(AiModelProperties properties) {
        StringBuilder suggestions = new StringBuilder();
        
        if (properties.getMax() == null || properties.getMax() > 100) {
            suggestions.append("建议: 将max参数设置为50-100之间以平衡性能和记忆能力\n");
        }
        
        if (properties.getTemperature() == null) {
            suggestions.append("建议: 设置temperature参数(0.0-2.0)来控制生成文本的随机性\n");
        }
        
        if (properties.getMax_tokens() == null) {
            suggestions.append("建议: 设置max_tokens参数来限制单次生成的最大长度\n");
        }
        
        return suggestions.toString();
    }
}