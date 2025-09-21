package top.kloping.core.ai.dto;

import lombok.EqualsAndHashCode;

/**
 *
 *
 * @author github kloping
 * @since 2025/9/19-10:44
 */
@EqualsAndHashCode(callSuper = true)
public class UserMessage extends Message<String> {
    public static final String TYPE = "user";

    public UserMessage(String content) {
        super(TYPE, content);
    }
}
