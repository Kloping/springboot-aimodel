package top.kloping.core.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.kloping.core.ai.AiModelProperties;
import top.kloping.core.ai.dto.*;
import top.kloping.core.ai.mcp.McpClient;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author github kloping
 */
@Slf4j
public class AiRequestModelImpl implements AiRequestModel {
    private final String finalUrl;
    private final ChatContext chatContext;
    private final AiModelProperties properties;
    private final OkHttpClient client;
    @Getter
    private final List<McpClient> mcpClients = new LinkedList<>();

    public AiRequestModelImpl(String finalUrl,
                              AiModelProperties properties, ChatContext chatContext, OkHttpClient client) {
        this.chatContext = chatContext;
        this.properties = properties;
        this.finalUrl = finalUrl;
        this.client = client;
    }

    @Override
    public void addMcpServer(McpClient mcpClient) {
        mcpClients.add(mcpClient);
    }

    @Override
    public void setSystemMessage(String message) {
        getContext().setSystemMessage(new SystemMessage(message));
    }

    @Override
    public ChatContext getContext() {
        return chatContext;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        chatContext.addMessage(new UserMessage(chatRequest.getContent()));
        Request request = getRequestByChatRequest(chatRequest, false);
        return getChatResponse(chatRequest, request);
    }

    @Override
    public ChatResponse doStreamChat(ChatRequest chatRequest) {
        chatContext.addMessage(new UserMessage(chatRequest.getContent()));
        Request request = getRequestByChatRequest(chatRequest, true);
        return getChatResponse(chatRequest, request);
    }

    private ChatResponse getChatResponse(ChatRequest chatRequest, Request request) {
        log.debug("request start {} url {} ", request.hashCode(),request.url());
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonObject = JSON.parseObject(responseBody);
                ChatResponse chatResponse = handleChatResponse(jsonObject, chatRequest);
                if (chatResponse != null) chatResponse.setIsCompleted(true);
                return chatResponse;
            } else {
                log.error(response.body().string());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    private Request getRequestByChatRequest(ChatRequest chatRequest, Boolean isStream) {
        JSONObject reqBody = new JSONObject();
        reqBody.put("model", (chatRequest.getModel() == null || chatRequest.getModel().trim().isEmpty()) ? properties.getModel() : chatRequest.getModel());
        if (properties.getEnable_thinking() != null)
            reqBody.put("enable_thinking", properties.getEnable_thinking());
        if (properties.getSeed() != null)
            reqBody.put("seed", properties.getSeed());
        if (properties.getMax_input_tokens() != null)
            reqBody.put("max_input_tokens", properties.getMax_input_tokens());
        if (properties.getMax_tokens() != null)
            reqBody.put("max_tokens", properties.getMax_tokens());
        if (properties.getTemperature() != null)
            reqBody.put("temperature", properties.getTemperature());
        if (properties.getTop_k() != null)
            reqBody.put("top_k", properties.getTop_k());
        if (properties.getSeed() != null)
            reqBody.put("seed", properties.getSeed());
        if (chatRequest.getStop() != null)
            reqBody.put("stop", chatRequest.getStop());
        reqBody.put("stream", isStream);
        List<Message<?>> messages = new LinkedList<>();
        SystemMessage systemMessage = chatContext.getSystemMessage();
        if (systemMessage != null) messages.add(systemMessage);
        messages.addAll(chatContext.getMessages());
        reqBody.put("messages", messages);
        List<RequestTool> reqTools = chatRequest.getReqTools();
        if (chatRequest.getTools() != null) reqTools.addAll(chatRequest.getReqTools());
        for (McpClient mcpClient : mcpClients) {
            if (mcpClient != null) reqTools.addAll(mcpClient.getRequestTools());
        }
        if (!reqTools.isEmpty()) reqBody.put("tools", reqTools);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqBody.toString());
        log.debug("set reqbody {}", reqBody);
        return new Request.Builder().header("Authorization", "Bearer " + properties.getToken())
                .url(finalUrl).method("POST", body).build();
    }

    private ChatResponse handleChatToolRequest(ChatRequest chatRequest, AssistantMessage assistantMessage) {
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getTool_calls();
        List<ToolMessage> toolMessages = RequestTool.toolCall(toolCalls);
        if (toolMessages == null || toolMessages.isEmpty()) {
            toolCalls.forEach(e -> {
                for (McpClient mcpClient : mcpClients) {
                    if (mcpClient.getTool().containsKey(e.getFunction().getName())) {
                        ToolMessage toolMessage = mcpClient.toolCall(e);
                        chatContext.addMessage(toolMessage);
                        break;
                    }
                }
            });
        }
        if (toolMessages != null) toolMessages.forEach(chatContext::addMessage);
        Request request = getRequestByChatRequest(chatRequest, false);
        return getChatResponse(chatRequest, request);
    }

    private ChatResponse handleChatResponse(JSONObject jsonObject, ChatRequest chatRequest) {
        JSONArray array = jsonObject.getJSONArray("choices");
        JSONObject choice = array.getJSONObject(0);
        JSONObject choiceMessage = choice.getJSONObject("message");
        ChatResponse chatResponse = jsonObject.toJavaObject(ChatResponse.class);
        log.debug("round of conversation completed. " +
                        "consumed {} completion tokens, {} prompt tokens,total {} tokens",
                chatResponse.getUsage().getCompletion_tokens(),
                chatResponse.getUsage().getPrompt_tokens(),
                chatResponse.getUsage().getTotal_tokens());
        chatResponse.setChatContext(chatContext);
        String role = choiceMessage.getString("role");
        if (AssistantMessage.TYPE.equalsIgnoreCase(role)) {
            AssistantMessage assistantMessage = choiceMessage.toJavaObject(AssistantMessage.class);
            chatContext.addMessage(assistantMessage);
            if (assistantMessage.getTool_calls() != null && !assistantMessage.getTool_calls().isEmpty()) {
                try {
                    return handleChatToolRequest(chatRequest, assistantMessage);
                } finally {
                    if (chatRequest.getClearToolMessage()) clearToolMessage();
                }
            }
        }
        return chatResponse;
    }

    @Override
    public synchronized void clearChatMemory() {
        chatContext.getMessages().clear();
    }

    @Override
    public synchronized void clearToolMessage() {
        Iterator<Message<?>> iterator = chatContext.getMessages().iterator();
        while (iterator.hasNext()) {
            Message<?> message = iterator.next();
            if (message instanceof ToolMessage) {
                iterator.remove();
            } else if (message instanceof AssistantMessage) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                if (assistantMessage.getTool_calls() != null && !assistantMessage.getTool_calls().isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

}
