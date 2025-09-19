package top.kloping.core.ai.service;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import top.kloping.core.ai.dto.Message;

import java.util.List;

/**
 *
 * @author github kloping
 * @since 2025/9/19-12:44
 */
@Data
public class ChatResponse {
    @JSONField(serialize = false, deserialize = false)
    private ChatContext chatContext;

    private String id;
    private List<Choices> choices;
    private String object;
    private Usage usage;
    private Long created;
    private String system_fingerprint;
    private String model;

    @Data
    public static class Choices {
        @JSONField(deserialize = false)
        private Message message;
        private String finish_reason;
        private Integer index;
        private Object logprobs;
    }

    @Data
    public static class Usage {
        private Integer completion_tokens;
        private Integer prompt_tokens;
        private Integer total_tokens;
    }

    /**
     * 获取响应
     *
     * @return
     */
    public String response() {
        Message<?> message = getChatContext().getMessages().get(getChatContext().getMessages().size() - 1);
        return message.getContent().toString();
    }
}
