## springboot-aimodel

> 适用于springboot中的AI模型及工具调用

## 🚀 最新优化 (2025/9/21)

本次代码优化包含以下重大改进：

### 🏗️ 架构优化
- **异常处理系统**: 新增统一的`AiModelException`异常体系，提供详细的错误码和错误信息
- **配置验证**: 添加`ConfigurationValidator`，自动验证所有配置参数的有效性
- **线程安全**: 重构核心类，使用`ConcurrentHashMap`和原子操作确保线程安全
- **资源管理**: 新增`HttpClientPoolManager`和`SimpleCacheManager`，改善资源利用和内存管理

### 📊 性能提升
- **连接池**: HTTP客户端使用连接池，减少连接开销
- **缓存机制**: 内置TTL缓存，支持自动过期清理
- **内存监控**: `MemoryMonitor`组件监控内存使用，防止内存泄漏
- **并发优化**: 改善并发处理能力，提升多线程环境下的性能

### 🛡️ 稳定性增强
- **健壮的错误处理**: 全面的异常捕获和恢复机制
- **配置验证**: 启动时自动验证配置，防止运行时错误
- **日志优化**: 统一的日志记录，支持MDC上下文追踪
- **资源清理**: 自动资源释放，防止内存泄漏

### 🧪 测试完善
- **单元测试**: 为核心组件添加完整的测试用例
- **验证测试**: 配置验证器的完整测试覆盖
- **工具测试**: 请求工具注册和调用的测试

---

## 📝 使用说明

```xml

<!--使用快照版本-->
<repositories>
    <repository>
        <name>Central Portal Snapshots</name>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<!-- 引入依赖 -->
<dependencies>
    <dependency>
        <groupId>top.kloping.core</groupId>
        <artifactId>springboot-aimodel</artifactId>
        <version>1.1.2-SNAPSHOT</version>
    </dependency>
</dependencies>

```

- 开发对应阿里AI平台 **理论**适合各AI模型调用

<hr>
> 国内AI平台

- [阿里云百炼](https://bailian.console.aliyun.com/)
- [硅基流动](https://www.siliconflow.cn/)
- ~~- [openrouter](https://openrouter.ai/)~~

<hr>

## 如何使用

> application.yml 配置文件

```yaml
top:
  kloping:
    ai:
      # OPENAI 的 服务地址
      server: https://dashscope.aliyuncs.com/compatible-mode
      # 对话路径 最后将路径拼接的
      path: /v1/chat/completions
      token: sk-xxxxxx
      # 对话模型
      model: qwen-plus-2025-01-25
      # 模型参数 --以下可以不填--
      max: 50 #对话最长记忆
      temperature: 0.7 # 模型温度
      top_k: 50 #取值越大，生成的随机性越高
      max_tokens: 2048 # 生成的最大长度
      max_input_tokens: 2048 # 输入的最大长度
      enable_thinking: false # 是否启用思考模式
      seed: 0 # 0到231
    ## MCP SERVER 配置 可以不配置
    mcp:
      server: https://dashscope.aliyuncs.com
      endpoint: /api/v1/mcps/WebSearch/sse
      token: sk-xxxxxx
      heartbeat: 7

logging:
  level:
    top.kloping.core.ai: debug
```

> AiRequestTestDemo.java 测试启动类

```java


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import top.kloping.core.ai.AiTool;
import top.kloping.core.ai.AiToolParm;
import top.kloping.core.ai.service.AiRequestModel;
import top.kloping.core.ai.service.ChatRequest;
import top.kloping.core.ai.service.ChatResponse;

import java.util.Date;

@SpringBootApplication
public class AiRequestTestDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiRequestTestDemo.class);

    public static void main(String[] args) {
        SpringApplication.run(AiRequestTestDemo.class, args);
    }

    @Autowired
    AiRequestModel requestModel;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ChatResponse response = requestModel.doChat(
                ChatRequest.builder()
                        .setContent("你好AI大模型,请告诉我现在时间!")
                        .addTool(new AiRequestTestDemo())
                        .clearToolMessage(false)
                        .build());
        log.info("调用结果: {}", response.response());
        log.info("--测试通过--");
    }

    @AiTool(desc = "获得当前时间")
    public String time(@AiToolParm(desc = "参数留空") String p) {
        log.info("{} - tool 调用了! ", p);
        return new Date().toString();
    }

}
```

#### 初步测试版本 如有问题 请开issue