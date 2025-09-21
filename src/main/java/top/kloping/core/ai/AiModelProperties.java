package top.kloping.core.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.kloping.core.ai.validation.ConfigurationValidator;

/**
 * AI模型配置属性类
 * 包含所有AI模型相关的配置项
 *
 * @author github kloping
 * @since 2025/9/19
 */
@Slf4j
@Data
public class AiModelProperties {
    
    public AiModelProperties() {
        log.info("AI model properties initialize.");
    }
    
    /**
     * 配置验证方法
     */
    public void validateConfiguration() {
        ConfigurationValidator.validateAiModelProperties(this);
        log.info("AI model configuration validation passed.");
    }

    /**
     * 模型API地址
     */
    private String server;
    /**
     * 请求凭证
     */
    private String token;
    /**
     * 请求接口
     */
    private String path = "/v1/chat/completions";
    /**
     * 最大记忆长度
     */
    private Integer max = 50;
    /**
     * 模型名称
     */
    private String model;
    /**
     * 采样温度，控制模型生成文本的多样性。 0-2
     * temperature越高，生成的文本更多样，反之，生成的文本更确定。
     */
    private Float temperature = 1.0f;
    /**
     * 生成过程中采样候选集的大小。例如，取值为50时，仅将单次生成中得分最高的50个Token组成随机采样的候选集。取值越大，生成的随机性越高；取值越小，生成的确定性越高。取值为None或当top_k大于100时，表示不启用top_k策略，此时仅有top_p策略生效。
     */
    private Integer top_k;
    /**
     * 允许输入的最大 Token 长度
     */
    private Integer max_input_tokens;
    /**
     * 允许生成的最大 Token 长度
     */
    private Integer max_tokens;
    /**
     * 是否思考模式
     */
    private Boolean enable_thinking;
    /**
     * 设置seed参数会使文本生成过程更具有确定性，通常用于使模型每次运行的结果一致。
     * <p>
     * 在每次模型调用时传入相同的seed值（由您指定），并保持其他参数不变，模型将尽可能返回相同的结果。
     * <p>
     * 取值范围：0到231−1。
     */
    private Integer seed;
}