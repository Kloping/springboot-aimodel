package top.kloping.core.ai.exception;

/**
 * AI模型通用异常类
 *
 * @author github kloping
 * @since 2025/9/21
 */
public class AiModelException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public AiModelException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public AiModelException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AiModelException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public AiModelException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public enum ErrorCode {
        INVALID_CONFIGURATION("AI模型配置无效"),
        REQUEST_FAILED("AI请求失败"),
        RESPONSE_PARSE_ERROR("响应解析错误"),
        TOOL_INVOCATION_ERROR("工具调用错误"),
        MCP_CONNECTION_ERROR("MCP连接错误"),
        THREAD_INTERRUPTED("线程被中断"),
        TIMEOUT_ERROR("请求超时"),
        AUTHENTICATION_ERROR("认证失败");
        
        private final String message;
        
        ErrorCode(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}