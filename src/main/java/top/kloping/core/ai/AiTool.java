package top.kloping.core.ai;

import java.lang.annotation.*;

/**
 *
 * 注解在方法上的AI请求工具
 *
 * @author github kloping
 * @since 2025/9/19-13:47
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiTool {
    /**
     * 方法名
     *
     * @return
     */
    String name() default "";

    /**
     * 工具描述
     */
    String desc() default "";

    /**
     * 工具返回描述
     */
    String ret() default "";
}
