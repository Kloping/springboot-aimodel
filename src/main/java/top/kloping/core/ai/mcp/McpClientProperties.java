package top.kloping.core.ai.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class McpClientProperties {
    private String server;
    private String endpoint = "/sse";
    private String token;

    private Integer heartbeat = 30;
    private String clientName = "mcp-client";
    private String clientVersion = "0.1.0";
    private String protocolVersion = "2025-05-05";

    public McpClientProperties() {
        log.info("mcp client properties initialize.");
    }
}
