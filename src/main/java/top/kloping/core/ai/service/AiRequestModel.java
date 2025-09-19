package top.kloping.core.ai.service;

/**
 * AI 请求定义接口
 *
 * @author github kloping
 * @date 2025/9/19-12:38
 */
public interface AiRequestModel {
    void setSystemMessage(String message);

    ChatContext getContext();

    ChatResponse doChat(ChatRequest request);

    void clearChatMemory();

    void clearToolMessage();
}
