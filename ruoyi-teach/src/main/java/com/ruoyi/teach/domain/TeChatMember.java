package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】会话成员实体（对应表 te_chat_member）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】每个会话的成员行，(chat_id, user_id) 唯一。
 *         last_read_msg_id 是已读/未读回执的核心依据：
 *         - 会话内消息ID > last_read_msg_id 且非本人发送 → 未读
 *         - 用户进入会话/打开消息中心时将 last_read_msg_id 推进到
 *           该会话最新消息ID，实现"一键已读"
 * ============================================================================
 */
public class TeChatMember implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成员ID（主键） */
    private Long id;

    /** 会话ID（关联 te_chat.id） */
    private Long chatId;

    /** 成员用户ID（关联 sys_user.user_id） */
    private Long userId;

    /** 最后已读消息ID（已读回执依据，0=从未读过） */
    private Long lastReadMsgId;

    /** 加入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date joinTime;

    /** 成员姓名（联查 sys_user.nick_name，不落库） */
    private String nickName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getChatId()
    {
        return chatId;
    }

    public void setChatId(Long chatId)
    {
        this.chatId = chatId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getLastReadMsgId()
    {
        return lastReadMsgId;
    }

    public void setLastReadMsgId(Long lastReadMsgId)
    {
        this.lastReadMsgId = lastReadMsgId;
    }

    public Date getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(Date joinTime)
    {
        this.joinTime = joinTime;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }
}
