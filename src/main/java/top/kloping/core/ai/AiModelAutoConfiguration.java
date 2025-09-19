package top.kloping.core.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kloping.core.ai.dto.*;
import top.kloping.core.ai.service.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnClass(AiModelProperties.class)
@ConditionalOnProperty(prefix = "top.kloping.ai", name = "server", matchIfMissing = false)
@EnableConfigurationProperties(AiModelProperties.class)
public class AiModelAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AiRequestModel defaultAiRequestModel(AiModelProperties properties) {
        ChatContext chatContext = new ChatContext();
        chatContext.setModel(properties.getModel());
        chatContext.setMax(properties.getMax());
        chatContext.setMessages(new LinkedList<>());
        final OkHttpClient client = new OkHttpClient();
        String server = properties.getServer();
        String path = properties.getPath();
        String url = server + path;
        int i = url.indexOf("//");
        String proot = url.substring(0, i + 2);
        String urlPath = url.substring(i + 2);
        urlPath = urlPath.replaceAll("//", "/");
        final String finalUrl = proot + urlPath;
        return new AiRequestModel() {

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
                Request request = getRequestByChatRequest(chatRequest);
                ChatResponse response = getChatResponse(chatRequest, request);
                if (response != null) return response;
                return null;
            }

            @Nullable
            private ChatResponse getChatResponse(ChatRequest chatRequest, Request request) {
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = JSON.parseObject(responseBody);
                        return handleChatResponse(jsonObject, chatRequest);
                    } else {
                        log.error(response.body().string());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            }


            @NotNull
            private Request getRequestByChatRequest(ChatRequest chatRequest) {
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
                List<Message<?>> messages = new LinkedList<>();
                SystemMessage systemMessage = chatContext.getSystemMessage();
                if (systemMessage != null) messages.add(systemMessage);
                messages.addAll(chatContext.getMessages());
                reqBody.put("messages", messages);
                if (chatRequest.getTools() != null && !chatRequest.getTools().isEmpty()) {
                    reqBody.put("tools", chatRequest.getReqTools());
                }
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqBody.toString());
                return new Request.Builder().header("Authorization", "Bearer " + properties.getToken())
                        .url(finalUrl).method("POST", body).build();
            }

            private ChatResponse handleChatToolRequest(ChatRequest chatRequest, AssistantMessage assistantMessage) {
                ToolMessage toolMessage = RequestTool.toolCall(assistantMessage.getTool_calls().get(0));
                chatContext.addMessage(toolMessage);
                Request request = getRequestByChatRequest(chatRequest);
                return getChatResponse(chatRequest, request);
            }

            @NotNull
            private ChatResponse handleChatResponse(JSONObject jsonObject, ChatRequest chatRequest) {
                JSONArray array = jsonObject.getJSONArray("choices");
                JSONObject choice = array.getJSONObject(0);
                JSONObject choiceMessage = choice.getJSONObject("message");
                ChatResponse chatResponse = jsonObject.toJavaObject(ChatResponse.class);
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
        };
    }
}