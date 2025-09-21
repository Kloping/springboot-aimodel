package top.kloping.core.ai.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 *
 *
 * @author github kloping
 * @since 2025/9/19-10:56
 */
@EqualsAndHashCode(callSuper = true)
public class ToolMessage extends Message<String> {
    public static final String TYPE = "tool";
    private final String tool_call_id;

    public ToolMessage(String content, String tool_call_id) {
        super(TYPE, content);
        this.tool_call_id = tool_call_id;
    }

    public ToolMessage(String content, String tool_call_id, boolean isFinished) {
        super(TYPE, content);
        this.tool_call_id = tool_call_id;
        this.isFinished = isFinished;
    }

    @JSONField(serialize = false, deserialize = false)
    @Getter
    private boolean isFinished = false;


}
