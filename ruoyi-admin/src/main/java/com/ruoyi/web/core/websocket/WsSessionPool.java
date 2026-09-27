package com.ruoyi.web.core.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;

/**
 * ============================================================================
 * 【功能】WebSocket 在线会话池（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】以 userId 为键保存所有在线连接，提供上线登记/下线注销/
 *         定向推送/在线判断能力。采用 ConcurrentHashMap 保证并发安全；
 *         同一账号重复连接时以最新会话为准（旧连接被顶替）。
 * ============================================================================
 */
public class WsSessionPool
{
    private static final Logger log = LoggerFactory.getLogger(WsSessionPool.class);

    /** 在线连接池：userId -> WebSocket Session（一个用户同时只保留一条连接） */
    private static final Map<Long, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 用户上线登记：若该用户已有旧连接，先关闭旧连接（顶下线）
     */
    public static synchronized void online(Long userId, Session session)
    {
        Session old = ONLINE_SESSIONS.put(userId, session);
        if (old != null && old.isOpen())
        {
            try
            {
                // 旧连接被新连接顶替，礼貌关闭
                old.close();
            }
            catch (IOException ignored)
            {
                // 关闭失败不影响新连接使用
            }
        }
        log.info("[WS] 用户[{}]上线，当前在线数：{}", userId, ONLINE_SESSIONS.size());
    }

    /**
     * 用户下线注销：仅当池内登记的正是这条连接时才移除，
     * 避免旧连接关闭回调误删新连接
     */
    public static synchronized void offline(Long userId, Session session)
    {
        if (ONLINE_SESSIONS.remove(userId, session))
        {
            log.info("[WS] 用户[{}]下线，当前在线数：{}", userId, ONLINE_SESSIONS.size());
        }
    }

    /**
     * 向指定在线用户推送文本帧；用户离线或推送失败时静默忽略
     * （离线用户下次打开页面通过 REST 拉取未读消息，不丢数据）
     */
    public static void pushToUser(Long userId, String text)
    {
        Session session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen())
        {
            try
            {
                // 同步发送：连续两帧（chat广播+sent回执）时异步写会撞
                // Tomcat的TEXT_FULL_WRITING状态，basic串行写最稳
                session.getBasicRemote().sendText(text);
            }
            catch (Exception e)
            {
                log.error("[WS] 推送给用户[{}]失败：{}", userId, e.getMessage());
            }
        }
    }

    /**
     * 判断某用户是否在线
     */
    public static boolean isOnline(Long userId)
    {
        Session session = ONLINE_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 当前全部在线用户ID集合（公告批量推送时先过滤离线用户）
     */
    public static Set<Long> onlineUserIds()
    {
        return ONLINE_SESSIONS.keySet();
    }

    /**
     * 构造标准 JSON 报文（仅支持字符串/数字字段，满足本项目协议即可）
     *
     * @param type    报文类型：chat/sent/pong/notice/error/open
     * @param keyVals 交替出现的 键1, 值1, 键2, 值2 ...
     */
    public static String buildJson(String type, Object... keyVals)
    {
        StringBuilder sb = new StringBuilder("{\"type\":\"").append(type).append("\"");
        for (int i = 0; i + 1 < keyVals.length; i += 2)
        {
            Object key = keyVals[i];
            Object val = keyVals[i + 1];
            if (key == null || val == null)
            {
                continue;
            }
            sb.append(",\"").append(key).append("\":");
            if (val instanceof Number)
            {
                // 数字类型不加引号
                sb.append(val);
            }
            else
            {
                // 字符串类型转义引号后输出
                sb.append('"').append(StringUtils.trim(String.valueOf(val)).replace("\"", "\\\"")).append('"');
            }
        }
        return sb.append('}').toString();
    }
}
