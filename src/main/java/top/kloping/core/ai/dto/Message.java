package top.kloping.core.ai.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 *
 * @author github kloping
 * @date 2025/9/19-10:43
 */
@Getter
@EqualsAndHashCode
public abstract class Message<T> {
    public final String role;
    @Setter
    protected T content;

    protected Message(String role) {
        this.role = role;
    }

    protected Message(String role, T content) {
        this.role = role;
        this.content = content;
    }
}
