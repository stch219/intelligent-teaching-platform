package com.ruoyi.web.core.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * ============================================================================
 * 【功能】WebSocket 握手令牌认证器（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】浏览器 WebSocket 握手无法自定义请求头，因此前端通过
 *         /websocket/message?token=xxx 携带登录令牌。本类复用若依
 *         TokenService 相同的校验链路：jjwt 解析出 uuid 声明，
 *         再用 uuid 去 Redis 取回 LoginUser，保证与 REST 接口同一套会话。
 * ============================================================================
 */
@Component
public class WsAuth
{
    /** 令牌秘钥，与 application.yml 的 token.secret 完全一致 */
    @Value("${token.secret}")
    private String secret;

    /** Redis 缓存工具（LoginUser 存放处） */
    private final RedisCache redisCache;

    public WsAuth(RedisCache redisCache)
    {
        this.redisCache = redisCache;
    }

    /**
     * 校验连接令牌，合法则返回登录用户，非法返回 null
     *
     * @param token 前端携带的 JWT（兼容带 Bearer 前缀的写法）
     */
    public LoginUser auth(String token)
    {
        if (token == null || token.isEmpty())
        {
            return null;
        }
        try
        {
            // 容错：允许前端误带 "Bearer " 前缀
            if (token.startsWith("Bearer "))
            {
                token = token.substring(7);
            }
            // 与 TokenService.parseToken 相同的解析方式
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            // 取出登录会话 uuid，拼出 Redis 键
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            return redisCache.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + uuid);
        }
        catch (Exception e)
        {
            // 令牌过期/伪造/Redis 失效统一视为未认证
            return null;
        }
    }
}
