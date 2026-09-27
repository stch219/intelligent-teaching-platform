package com.ruoyi.web.core.websocket;

import java.util.List;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.teach.domain.TeChatMember;
import com.ruoyi.teach.domain.TeMessage;
import com.ruoyi.teach.mapper.TeChatMemberMapper;
import com.ruoyi.teach.service.ITeMessageService;

/**
 * ============================================================================
 * 【功能】消息中心 WebSocket 服务端点（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】连接地址：ws://host:8080/websocket/message?token=登录JWT
 *   客户端→服务端协议：
 *     {"type":"chat","chatId":1,"content":"你好"}  发送聊天消息
 *     {"type":"ping"}                               心跳保活
 *   服务端→客户端协议：
 *     {"type":"open","userId":107}                  握手成功确认
 *     {"type":"chat","chatId":1,"msgId":9,"senderId":107,
 *      "senderName":"张一","content":"你好",
 *      "sendTime":"2026-09-27 12:00:00"}            推给会话内全部在线成员
 *     {"type":"sent","chatId":1,"msgId":9,
 *      "sendTime":"..."}                            发送者本人回执
 *     {"type":"pong"}                               心跳应答
 *     {"type":"error","msg":"..."}                  业务错误提示
 *     {"type":"notice",...}                         班级公告在线提醒（公告发布时推送）
 * ============================================================================
 */
@Component
@ServerEndpoint("/websocket/message")
public class TeMessageWebSocketServer
{
    private static final Logger log = LoggerFactory.getLogger(TeMessageWebSocketServer.class);

    /** 连接握手成功 */
    @OnOpen
    public void onOpen(Session session)
    {
        // 1. 从握手 URL 取令牌并认证（getFirst 是 JDK21 API，JDK17 需 get(0)）
        java.util.List<String> tokens = session.getRequestParameterMap().get("token");
        String token = (tokens == null || tokens.isEmpty()) ? null : tokens.get(0);
        LoginUser user = SpringUtils.getBean(WsAuth.class).auth(token);
        if (user == null)
        {
            // 未认证直接关闭连接（SecurityConfig 已放行该路径，认证完全由本端点负责）
            try
            {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "unauthorized"));
            }
            catch (Exception ignored)
            {
            }
            return;
        }
        // 2. 用户ID挂到会话属性上，后续收发消息直接取用
        Long userId = user.getUserId();
        session.getUserProperties().put("userId", userId);
        // 3. 登记进在线池（重复连接自动顶替旧连接）
        WsSessionPool.online(userId, session);
        // 4. 回握手确认报文，前端据此标记"连接就绪"
        WsSessionPool.pushToUser(userId, WsSessionPool.buildJson("open", "userId", userId));
    }

    /** 收到客户端报文：心跳或聊天消息 */
    @OnMessage
    public void onMessage(Session session, String text)
    {
        // 会话属性中取回当前用户（onOpen 已认证，此处必然存在）
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null)
        {
            return;
        }
        try
        {
            JSONObject body = JSONObject.parseObject(text);
            String type = body.getString("type");
            // 心跳：立即回 pong，维持连接不被中间件掐断
            if ("ping".equals(type))
            {
                WsSessionPool.pushToUser(userId, WsSessionPool.buildJson("pong"));
                return;
            }
            // 聊天消息：落库并路由推送
            if ("chat".equals(type))
            {
                handleChat(userId, body.getLong("chatId"), body.getString("content"));
            }
        }
        catch (Exception e)
        {
            // 任何业务异常都只回 error 报文，不让连接异常断开
            log.error("[WS] 处理消息出错 user={}: {}", userId, e.getMessage());
            WsSessionPool.pushToUser(userId, WsSessionPool.buildJson("error", "msg",
                    e.getMessage() == null ? "发送失败" : e.getMessage()));
        }
    }

    /**
     * 聊天消息处理：校验成员+落库（Service内完成），随后向会话内
     * 全部在线成员推送 chat 报文，并向发送者回 sent 回执
     */
    private void handleChat(Long userId, Long chatId, String content)
    {
        // 1. 落库（成员权限校验、内容非空/长度校验均在 Service 内）
        ITeMessageService messageService = SpringUtils.getBean(ITeMessageService.class);
        TeMessage msg = messageService.send(userId, chatId, content);
        // 统一格式化发送时间，前端直接展示
        String sendTime = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, msg.getSendTime());
        // 2. 查会话全部成员作为推送目标
        List<TeChatMember> members = SpringUtils.getBean(TeChatMemberMapper.class).selectByChatId(chatId);
        String push = WsSessionPool.buildJson("chat",
                "chatId", msg.getChatId(),
                "msgId", msg.getId(),
                "senderId", msg.getSenderId(),
                "senderName", msg.getSenderName(),
                "content", msg.getContent(),
                "sendTime", sendTime);
        for (TeChatMember m : members)
        {
            // 逐个推送，离线成员自动跳过（其未读数已随落库自然增加）
            WsSessionPool.pushToUser(m.getUserId(), push);
        }
        // 3. 给发送者单独回执（前端凭 msgId 把"发送中"气泡置为已发送）
        WsSessionPool.pushToUser(userId, WsSessionPool.buildJson("sent",
                "chatId", chatId, "msgId", msg.getId(), "sendTime", sendTime));
    }

    /** 连接关闭：从在线池注销 */
    @OnClose
    public void onClose(Session session)
    {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null)
        {
            WsSessionPool.offline(userId, session);
        }
    }

    /** 连接异常：记录日志并安全关闭（onClose 随后会被容器回调） */
    @OnError
    public void onError(Session session, Throwable thr)
    {
        Long userId = (Long) session.getUserProperties().get("userId");
        log.error("[WS] 连接异常 user={}: {}", userId, thr.getMessage());
        try
        {
            if (session.isOpen())
            {
                session.close();
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
