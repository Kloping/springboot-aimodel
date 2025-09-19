package top.kloping.core.ai.service;

import lombok.Data;
import top.kloping.core.ai.dto.Message;
import top.kloping.core.ai.dto.SystemMessage;

import java.util.List;

/**
 *
 *
 * @author github kloping
 * @date 2025/9/19-12:46
 */
@Data
public class ChatContext {
    private String model;
    private Integer max;
    private SystemMessage systemMessage;
    private List<Message<?>> messages;

    public synchronized void addMessage(Message<?> message) {
        if (messages.size() >= max) messages.remove(0);
        messages.add(message);
    }
}
