package top.kloping.core.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author github kloping
 * @date 2025/9/19-10:56
 */
@EqualsAndHashCode(callSuper = true)
public class ToolMessage extends Message<String> {
    public static final String TYPE = "tool";
    private String tool_call_id;

    public ToolMessage(String content, String tool_call_id) {
        super(TYPE, content);
        this.tool_call_id = tool_call_id;
    }
}
