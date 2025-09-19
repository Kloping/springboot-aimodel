package top.kloping.core.ai.service;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 *
 *
 * @author github kloping
 * @date 2025/9/19-12:44
 */
@Getter
public class ChatRequest {
    private String model;
    private String content;
    private String stop;
    private Boolean clearToolMessage = true;
    private final List<Object> tools = new LinkedList<>();

    public List<RequestTool> getReqTools() {
        RequestTool.analyseObjectTools(tools);
        List<RequestTool> tools = new LinkedList<>();
        for (Object tool : this.tools) {
            List<RequestTool> rts0 = RequestTool.TOOLS_MAP.get(tool);
            tools.addAll(rts0);
        }
        return tools;
    }

    public static class ChatRequestBuilder {
        private final ChatRequest request = new ChatRequest();

        /**
         * 生成指定字符时停止
         * 非必选
         *
         * @param stop
         * @return
         */
        public ChatRequestBuilder setStop(String stop) {
            request.stop = stop;
            return this;
        }

        /**
         * 不设置使用 默认配置
         *
         * @param model
         * @return
         */
        public ChatRequestBuilder setModel(String model) {
            request.model = model;
            return this;
        }

        public ChatRequestBuilder setContent(String content) {
            request.content = content;
            return this;
        }

        public ChatRequestBuilder addTool(Object... tool) {
            request.tools.addAll(Arrays.asList(tool));
            return this;
        }

        public ChatRequestBuilder clearToolMessage(Boolean clearToolMessage) {
            request.clearToolMessage = clearToolMessage;
            return this;
        }

        public ChatRequest build() {
            return request;
        }
    }

    public static ChatRequestBuilder builder() {
        return new ChatRequestBuilder();
    }
}
