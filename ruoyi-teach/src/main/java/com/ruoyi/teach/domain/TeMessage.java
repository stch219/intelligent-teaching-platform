package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】聊天消息实体（对应表 te_message）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】单聊与群聊消息统一存储，按会话 chat_id 归档。
 *         senderName 为联查展示字段（不落库），用于前端气泡展示发送人。
 * ============================================================================
 */
public class TeMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 消息ID（主键，自增，已读回执按ID比较） */
    private Long id;

    /** 会话ID（关联 te_chat.id） */
    private Long chatId;

    /** 发送者用户ID（关联 sys_user.user_id） */
    private Long senderId;

    /** 消息类型（1文本 2图片 3文件；本阶段仅实现1文本） */
    private Long msgType;

    /** 消息内容（文本内容） */
    private String content;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** 发送者姓名（联查 sys_user.nick_name，不落库） */
    private String senderName;

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

    public Long getSenderId()
    {
        return senderId;
    }

    public void setSenderId(Long senderId)
    {
        this.senderId = senderId;
    }

    public Long getMsgType()
    {
        return msgType;
    }

    public void setMsgType(Long msgType)
    {
        this.msgType = msgType;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getSendTime()
    {
        return sendTime;
    }

    public void setSendTime(Date sendTime)
    {
        this.sendTime = sendTime;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public void setSenderName(String senderName)
    {
        this.senderName = senderName;
    }
}
