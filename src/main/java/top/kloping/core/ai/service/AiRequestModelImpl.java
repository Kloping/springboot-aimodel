package top.kloping.core.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import top.kloping.core.ai.AiModelProperties;
import top.kloping.core.ai.dto.*;
import top.kloping.core.ai.exception.AiModelException;
import top.kloping.core.ai.mcp.McpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI请求模型实现类
 * 负责处理AI模型的请求和响应处理
 *
 * @author github kloping
 * @since 2025/9/19
 */
@Slf4j
public class AiRequestModelImpl implements AiRequestModel {
    private final String finalUrl;
    private final ChatContext chatContext;
    private final AiModelProperties properties;
    private final OkHttpClient client;

    @Getter
    private final List<McpClient> mcpClients = new LinkedList<>();

    // 线程池用于异步处理
    private final ExecutorService executorService;

    public AiRequestModelImpl(String finalUrl,
                              AiModelProperties properties, ChatContext chatContext, OkHttpClient client) {
        this.chatContext = Objects.requireNonNull(chatContext, "ChatContext cannot be null");
        this.properties = Objects.requireNonNull(properties, "AiModelProperties cannot be null");
        this.finalUrl = buildAndValidateUrl(finalUrl);
        this.client = Objects.requireNonNull(client, "OkHttpClient cannot be null");

        // 创建线程池
        this.executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ai-request-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * 构建和验证URL
     */
    private String buildAndValidateUrl(String finalUrl) {
        if (finalUrl == null || finalUrl.trim().isEmpty()) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION,
                    "Final URL cannot be null or empty");
        }
        try {
            return normalizeUrl(finalUrl);
        } catch (Exception e) {
            throw new AiModelException(AiModelException.ErrorCode.INVALID_CONFIGURATION,
                    "Invalid URL format: " + finalUrl, e);
        }
    }

    /**
     * 规范化URL，处理重复斜杠
     */
    private String normalizeUrl(String url) {
        int protocolIndex = url.indexOf("//");
        if (protocolIndex == -1) {
            throw new IllegalArgumentException("Invalid URL format, missing protocol");
        }
        String protocol = url.substring(0, protocolIndex + 2);
        String path = url.substring(protocolIndex + 2);
        path = path.replaceAll("//+", "/");
        return protocol + path;
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
//        chatContext.addMessage(new UserMessage(chatRequest.getContent()));
//        Request request = getRequestByChatRequest(chatRequest, true);
//        return getChatResponse(chatRequest, request);
        return null;
    }

    private ChatResponse getChatResponse(ChatRequest chatRequest, Request request) {
        log.debug("request start {} url {}", request.hashCode(), request.url());
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseString = getString(response);
                JSONObject jsonObject;
                try {
                    jsonObject = JSON.parseObject(responseString);
                } catch (Exception e) {
                    throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                            "JSON解析失败: " + responseString, e);
                }

                ChatResponse chatResponse = handleChatResponse(jsonObject, chatRequest);
                if (chatResponse != null) {
                    chatResponse.setIsCompleted(true);
                }
                return chatResponse;
            } else {
                String errorBody = "";
                try {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        errorBody = responseBody.string();
                    }
                } catch (IOException e) {
                    log.warn("读取错误响应体失败", e);
                }
                throw new AiModelException(AiModelException.ErrorCode.REQUEST_FAILED,
                        String.format("HTTP请求失败: %d %s, 响应: %s",
                                response.code(), response.message(), errorBody));
            }
        } catch (AiModelException e) {
            throw e;
        } catch (IOException e) {
            throw new AiModelException(AiModelException.ErrorCode.REQUEST_FAILED,
                    "网络请求失败", e);
        } catch (Exception e) {
            throw new AiModelException(AiModelException.ErrorCode.REQUEST_FAILED,
                    "请求处理失败", e);
        }
    }

    @NotNull
    private static String getString(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                    "响应体为空");
        }
        String responseString = responseBody.string();
        if (responseString.isEmpty()) {
            throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                    "响应内容为空");
        }
        return responseString;
    }


    private Request getRequestByChatRequest(ChatRequest chatRequest, Boolean isStream) {
        JSONObject reqBody = new JSONObject();

        // 设置模型
        String model = (chatRequest.getModel() == null || chatRequest.getModel().trim().isEmpty())
                ? properties.getModel() : chatRequest.getModel();
        reqBody.put("model", model);

        // 设置可选参数
        addIfNotNull(reqBody, "enable_thinking", properties.getEnable_thinking());
        addIfNotNull(reqBody, "seed", properties.getSeed());
        addIfNotNull(reqBody, "max_input_tokens", properties.getMax_input_tokens());
        addIfNotNull(reqBody, "max_tokens", properties.getMax_tokens());
        addIfNotNull(reqBody, "temperature", properties.getTemperature());
        addIfNotNull(reqBody, "top_k", properties.getTop_k());
        addIfNotNull(reqBody, "stop", chatRequest.getStop());

        reqBody.put("stream", isStream);

        // 构建消息列表
        List<Message<?>> messages = buildMessageList();
        reqBody.put("messages", messages);

        // 构建工具列表
        List<RequestTool> reqTools = buildToolList(chatRequest);
        if (!reqTools.isEmpty()) {
            reqBody.put("tools", reqTools);
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqBody.toString());
        log.debug("set reqbody {}", reqBody);

        return new Request.Builder()
                .header("Authorization", "Bearer " + properties.getToken())
                .url(finalUrl)
                .method("POST", body)
                .build();
    }

    /**
     * 添加非空值到JSON对象
     */
    private void addIfNotNull(JSONObject jsonObject, String key, Object value) {
        if (value != null) {
            jsonObject.put(key, value);
        }
    }

    /**
     * 构建消息列表
     */
    private List<Message<?>> buildMessageList() {
        List<Message<?>> messages = new LinkedList<>();
        SystemMessage systemMessage = chatContext.getSystemMessage();
        if (systemMessage != null) {
            messages.add(systemMessage);
        }
        messages.addAll(chatContext.getMessagesReadOnly());
        return messages;
    }

    /**
     * 构建工具列表
     */
    private List<RequestTool> buildToolList(ChatRequest chatRequest) {
        List<RequestTool> reqTools = chatRequest.getReqTools();
        if (chatRequest.getTools() != null) {
            reqTools.addAll(chatRequest.getReqTools());
        }

        // 添加MCP工具
        for (McpClient mcpClient : mcpClients) {
            if (mcpClient != null) {
                reqTools.addAll(mcpClient.getRequestTools());
            }
        }

        return reqTools;
    }

    private ChatResponse handleChatToolRequest(ChatRequest chatRequest, AssistantMessage assistantMessage) {
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getTool_calls();
        List<ToolMessage> toolMessages = RequestTool.toolCall(toolCalls);
        if (toolMessages.isEmpty() || toolMessages.size() < toolCalls.size()) {
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
        toolMessages.forEach(chatContext::addMessage);
        Request request = getRequestByChatRequest(chatRequest, false);
        return getChatResponse(chatRequest, request);
    }

    private ChatResponse handleChatResponse(JSONObject jsonObject, ChatRequest chatRequest) {
        try {
            JSONArray array = jsonObject.getJSONArray("choices");
            if (array == null || array.isEmpty()) {
                throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                        "Response choices array is null or empty");
            }

            JSONObject choice = array.getJSONObject(0);
            JSONObject choiceMessage = choice.getJSONObject("message");
            if (choiceMessage == null) {
                throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                        "Choice message is null");
            }

            ChatResponse chatResponse = jsonObject.toJavaObject(ChatResponse.class);
            if (chatResponse.getUsage() != null) {
                log.debug("round of conversation completed. " +
                                "consumed {} completion tokens, {} prompt tokens, total {} tokens",
                        chatResponse.getUsage().getCompletion_tokens(),
                        chatResponse.getUsage().getPrompt_tokens(),
                        chatResponse.getUsage().getTotal_tokens());
            }

            chatResponse.setChatContext(chatContext);
            String role = choiceMessage.getString("role");

            if (AssistantMessage.TYPE.equalsIgnoreCase(role)) {
                AssistantMessage assistantMessage = choiceMessage.toJavaObject(AssistantMessage.class);
                chatContext.addMessage(assistantMessage);

                if (assistantMessage.getTool_calls() != null && !assistantMessage.getTool_calls().isEmpty()) {
                    try {
                        return handleChatToolRequest(chatRequest, assistantMessage);
                    } finally {
                        if (chatRequest.getClearToolMessage()) {
                            clearToolMessage();
                        }
                    }
                }
            }

            return chatResponse;
        } catch (AiModelException e) {
            throw e;
        } catch (Exception e) {
            throw new AiModelException(AiModelException.ErrorCode.RESPONSE_PARSE_ERROR,
                    "处理响应失败", e);
        }
    }

    @Override
    public synchronized void clearChatMemory() {
        chatContext.clearMessages();
    }

    @Override
    public synchronized void clearToolMessage() {
        List<Message<?>> messagesToRemove = new ArrayList<>();
        List<Message<?>> allMessages = chatContext.getMessagesReadOnly();

        for (Message<?> message : allMessages) {
            if (message instanceof ToolMessage) {
                messagesToRemove.add(message);
            } else if (message instanceof AssistantMessage) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                if (assistantMessage.getTool_calls() != null && !assistantMessage.getTool_calls().isEmpty()) {
                    messagesToRemove.add(message);
                }
            }
        }

        // 从原始列表中移除
        chatContext.getMessages().removeAll(messagesToRemove);
    }

    /**
     * 资源清理方法
     */
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
