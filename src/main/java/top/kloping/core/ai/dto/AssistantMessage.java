package top.kloping.core.ai.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *
 *
 * @author github kloping
 * @since 2025/9/19-10:50
 */
@EqualsAndHashCode(callSuper = true)
@Setter
public class AssistantMessage extends Message<String> {
    public static final String TYPE = "assistant";

    public AssistantMessage() {
        super(TYPE);
    }

    @Getter
    @Setter
    private List<ToolCall> tool_calls;

    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private ToolCallFunction function;
        private Integer index;
    }

    @Data
    public static class ToolCallFunction {
        private String name;
        private String arguments;
    }
}
