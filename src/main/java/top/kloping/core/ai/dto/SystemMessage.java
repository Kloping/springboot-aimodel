package top.kloping.core.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author github kloping
 * @since 2025/9/19-10:44
 */
@EqualsAndHashCode(callSuper = true)
public class SystemMessage extends Message<String> {
    public static final String TYPE = "system";

    public SystemMessage(String content) {
        super(TYPE, content);
    }
}
