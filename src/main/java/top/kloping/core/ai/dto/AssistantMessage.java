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
 * @date 2025/9/19-10:50
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

    /**
     * <div class="expandable-content" data-spm-anchor-id="0.0.0.i59.5177707fImAYUX">
     *         <section id="f7cc2f696by6v" class="section"><p id="19ecfbbb87vt8"><b>id</b> <code data-tag="code" code-type="xCode" id="cf00be8918g6d" class="code"><i>string</i></code><i> </i></p><p jc="left" id="4cb0da5ce3mic" style="text-align:left">本次工具响应的<span class="help-letter-space"></span>ID。</p></section><section id="9d93bbddf5o5y" class="section"><p jc="left" id="d052997fa4g23" style="text-align:left" data-spm-anchor-id="0.0.0.i63.5177707fImAYUX"><b>type</b> <code data-tag="code" code-type="xCode" id="a75219fe70vtj" class="code"><i>string</i></code></p><p jc="left" id="83dba1edbdw91" style="text-align:left" data-spm-anchor-id="0.0.0.i62.5177707fImAYUX">工具的类型，当前只支持<code data-tag="code" code-type="xCode" class="code" id="08c6e1248as6v">function</code>。</p></section><section id="43457206b57yi" class="section" data-spm-anchor-id="0.0.0.i61.5177707fImAYUX"><p jc="left" id="565b48d232890" style="text-align:left"><b>function</b> <code data-tag="code" code-type="xCode" id="b02825d008k2o" class="code"><i>object</i></code></p><p jc="left" id="0222125569vj0" style="text-align:left">需要被调用的函数。</p>
     *     <section class="collapse expanded" id="a21e394a66bg0">
     *       <div class="expandable-title-bold">
     *         <span class="title"><p id="6cf1a97d92mee" data-tag="expandable-title" class="expandable-title"><b data-spm-anchor-id="0.0.0.i56.5177707fImAYUX">属性</b></p></span>
     *         <i class="icon help-iconfont help-icon-zhankai1 smallFont"></i>
     *       </div>
     *       <div class="expandable-content" data-spm-anchor-id="0.0.0.i57.5177707fImAYUX">
     *         <section id="57b5aa2073pg3" class="section"><p id="6383982c1c1u0"><b>name</b> <code data-tag="code" code-type="xCode" id="468e34b35797n" class="code"><i>string</i></code></p><p jc="left" id="124c16cd60xly" style="text-align:left" data-spm-anchor-id="0.0.0.i60.5177707fImAYUX">需要被调用的函数名。</p></section><section id="0ad38f97cforc" class="section"><p jc="left" id="b1387474b4nqs" style="text-align:left"><b>arguments</b> <code data-tag="code" code-type="xCode" id="dd5f3f4a53g36" class="code"><i>string</i></code></p><p jc="left" id="f1fb077fa2po6" style="text-align:left" data-spm-anchor-id="0.0.0.i58.5177707fImAYUX">需要输入到工具中的参数，为<span class="help-letter-space"></span>JSON<span class="help-letter-space"></span>字符串。</p></section>
     *       </div>
     *     </section>
     *   </section><section id="fca228b1a69qq" class="section"><p id="d248bca311wpc" data-spm-anchor-id="0.0.0.i64.5177707fImAYUX"><b>index</b> <code data-tag="code" code-type="xCode" id="3cfd30ccccgy8" class="code"><i>integer</i></code></p><p jc="left" id="9d2ac613973p7" style="text-align:left" data-spm-anchor-id="0.0.0.i71.5177707fImAYUX">工具信息在<code data-tag="code" code-type="xCode" class="code" id="bc7e5eea2fuct">tool_calls</code>列表中的索引。</p></section>
     *       </div>
     */
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
