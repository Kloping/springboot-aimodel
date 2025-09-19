package top.kloping.core.ai;

import java.lang.annotation.*;

/**
 *
 * 注解在方法参数注解
 *
 * @author github kloping
 * @date 2025/9/19-13:47
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiToolParm {
    /**
     * 参数描述
     */
    String desc() default "";

    /**
     *
     * @return
     */
    String name() default "";

    /**
     * 必须得 否则可能使用null
     *
     * @return
     */
    boolean required() default true;
}
