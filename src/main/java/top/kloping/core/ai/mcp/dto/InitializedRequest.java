package top.kloping.core.ai.mcp.dto;

/**
 *
 * @author github kloping
 * @since 2025/9/20-11:22
 */
public class InitializedRequest extends McpReqPack<McpReqPack.Params>{
    public InitializedRequest(Integer id, Params params) {
        super(id, "notifications/initialized", params);
    }
}
