package top.kloping.core.ai.mcp.dto;

import lombok.Data;

/**
 *
 * @author github kloping
 * @since 2025/9/20-11:24
 */
@Data
public class McpReqPack<T> {
    private final String jsonrpc = "2.0";
    private Integer id;
    private String method;
    private T params;

    public McpReqPack(Integer id, String method, T params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }


    @Data
    public static class Params {
        private String protocolVersion;
        private Object capabilities;
        private ClientInfo clientInfo;
    }

    @Data
    public static class ClientInfo {
        private String name = "mcp-client";
        private String version = "0.1.0";
    }
}
