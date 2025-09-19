package top.kloping.core.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;
import top.kloping.core.ai.AiTool;
import top.kloping.core.ai.AiToolParm;
import top.kloping.core.ai.dto.AssistantMessage;
import top.kloping.core.ai.dto.ToolMessage;

import java.lang.reflect.Method;
import java.util.*;

/**
 *
 * @author github kloping
 * @since 2025/9/19-14:21
 */
@Data
@Accessors(chain = true)
public class RequestTool {
    private final String type = "function";
    private Function function;

    @Data
    @Accessors(chain = true)
    public static class Function {
        private String name;
        private String description;
        private Parameter parameters;
    }

    @Data
    @Accessors(chain = true)
    public static class Parameter {
        private String type = "object";
        private Map<String, ParameterDesc> properties = new HashMap<>();
        private List<String> required = new ArrayList<>();
    }


    @Data
    @Accessors(chain = true)
    public static class ParameterDesc {
        private String type;
        private String description;
    }


    public static final Map<Object, List<RequestTool>> TOOLS_MAP = new HashMap<>();
    public static final Map<String, Map.Entry<Object, Method>> NAME_2_METHOD = new HashMap<>();

    public static synchronized void analyseObjectTools(List<Object> tools) {
        for (Object tool : tools) {
            if (tool != null) {
                if (TOOLS_MAP.containsKey(tool)) return;
                for (Method declaredMethod : tool.getClass().getDeclaredMethods()) {
                    AiTool annotation = declaredMethod.getAnnotation(AiTool.class);
                    if (annotation != null) {
                        declaredMethod.setAccessible(true);
                        analyseOneTool(tool, annotation, declaredMethod);
                    }
                }
            }
        }
    }

    public static void analyseOneTool(Object tool, AiTool annotation, Method declaredMethod) {
        String desc = annotation.desc();
        String ret = annotation.ret();
        if (ret != null && !ret.isEmpty()) desc += ".return:" + ret;
        RequestTool requestTool = new RequestTool();
        RequestTool.Function function = new RequestTool.Function();
        String funName = annotation.name().isEmpty() ? declaredMethod.getName() : annotation.name();
        //  保证 方法名不重复 但浪费token
        if (NAME_2_METHOD.containsKey(funName))
            funName = funName + "_" + (UUID.randomUUID().toString().substring(0, 5));
        NAME_2_METHOD.put(funName, new AbstractMap.SimpleEntry<>(tool, declaredMethod));
        function.setName(funName);
        function.setDescription(desc);
        int i = 1;
        RequestTool.Parameter toolParameter = new RequestTool.Parameter();
        toolParameter.setType("object");
        List<String> required = new ArrayList<>();
        toolParameter.setRequired(required);
        for (java.lang.reflect.Parameter parameter : declaredMethod.getParameters()) {
            AiToolParm parm = parameter.getAnnotation(AiToolParm.class);
            String pd0 = null;
            String name = null;
            if (parm != null) {
                pd0 = parm.desc();
                name = parm.name();
            }
            RequestTool.ParameterDesc parameterDesc = new RequestTool.ParameterDesc();
            parameterDesc.setDescription(pd0);
            parameterDesc.setType(getTypeByJava(parameter.getType()));
            if (name == null || name.trim().isEmpty()) name = "p" + i++;
            if (parm == null || parm.required()) required.add(name);
            toolParameter.getProperties().put(name, parameterDesc);
        }
        function.setParameters(toolParameter);
        requestTool.setFunction(function);
        // 添加tool
        List<RequestTool> tools = TOOLS_MAP.get(tool);
        if (tools == null) tools = new ArrayList<>();
        tools.add(requestTool);
        TOOLS_MAP.put(tool, tools);
    }

    public static String getTypeByJava(Class<?> type) {
        if (type.isAssignableFrom(String.class)) {
            return "string";
        } else if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) {
            return "integer";
        } else if (type.isAssignableFrom(Number.class) || type.isAssignableFrom(float.class) || type.isAssignableFrom(double.class)) {
            return "number";
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
            return "boolean";
        } else if (type.isAssignableFrom(Collection.class) || type.isArray()) {
            return "array";
        } else {
            return "object";
        }
    }

    public static Object[] getParams(Method method, JSONObject jsonObject) {
        Object[] params = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getParameters().length; i++) {
            java.lang.reflect.Parameter parameter = method.getParameters()[i];
            AiToolParm annotation = parameter.getAnnotation(AiToolParm.class);
            String name = null;
            if (annotation != null) name = annotation.name();
            if (name == null || name.trim().isEmpty()) name = "p" + (i + 1);
            Class<?> type = parameter.getType();
            if (type.isAssignableFrom(String.class)) {
                params[i] = jsonObject.getString(name);
            } else if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) {
                params[i] = jsonObject.getInteger(name);
            } else if (type.isAssignableFrom(Long.class) || type.isAssignableFrom(long.class)) {
                params[i] = jsonObject.getLong(name);
            } else if (type.isAssignableFrom(Double.class) || type.isAssignableFrom(double.class)) {
                params[i] = jsonObject.getDouble(name);
            } else if (type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) {
                params[i] = jsonObject.getDouble(name);
            } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
                params[i] = jsonObject.getBoolean(name);
            } else if (type.isAssignableFrom(Collection.class) || type.isArray()) {
                params[i] = jsonObject.getJSONArray(name).toArray();
            } else {
                params[i] = jsonObject.getObject(name, type);
            }
        }
        return params;
    }


    public static ToolMessage toolCall(AssistantMessage.ToolCall toolCall) {
        String name = toolCall.getFunction().getName();
        JSONObject jsonObject = JSON.parseObject(toolCall.getFunction().getArguments());
        java.util.Map.Entry<Object, Method> entry = RequestTool.NAME_2_METHOD.get(name);
        Object tool = entry.getKey();
        Method method = entry.getValue();
        String result = null;
        try {
            Object[] args = RequestTool.getParams(method, jsonObject);
            Object oo = method.invoke(tool, args);
            oo = oo == null ? "" : oo;
            result = oo.toString();
        } catch (Exception e) {
            result = "call failed! msg:" + e.getMessage();
        }
        return new ToolMessage(result, toolCall.getId());
    }
}
