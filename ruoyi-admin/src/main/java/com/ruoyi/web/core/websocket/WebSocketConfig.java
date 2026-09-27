package com.ruoyi.web.core.websocket;

/**
 * ============================================================================
 * 【功能】WebSocket 配置类（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】ServerEndpointExporter 会扫描容器内所有 @ServerEndpoint 注解的端点，
 *         并将其注册到内嵌 Tomcat 中。注意：使用内嵌容器启动时必须注册该 Bean，
 *         否则 /websocket/message 端点不会生效。
 *         @ServerEndpoint 类本身不是 Spring Bean，其中的依赖一律通过
 *         SpringUtils.getBean() 手动获取。
 * ============================================================================
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig
{
    /**
     * 服务器端点导出器：把 @ServerEndpoint 端点注册进内嵌 Servlet 容器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter()
    {
        return new ServerEndpointExporter();
    }
}
