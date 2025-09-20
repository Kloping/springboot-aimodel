package top.kloping.core.ai.service;

import top.kloping.core.ai.mcp.McpClient;

/**
 * AI 请求定义接口
 *
 * @author github kloping
 * @since 2025/9/19-12:38
 */
public interface AiRequestModel {
    void setSystemMessage(String message);

    ChatContext getContext();

    ChatResponse doChat(ChatRequest request);

    ChatResponse doStreamChat(ChatRequest request);

    void clearChatMemory();

    void clearToolMessage();

    void addMcpServer(McpClient mcpClient);
}
