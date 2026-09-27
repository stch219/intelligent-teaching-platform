package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】聊天会话实体（对应表 te_chat）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】会话分两类：
 *         - 单聊（chat_type=1）：两用户首次互发时懒创建，chat_name 为空，
 *           前端展示对方姓名
 *         - 群聊（chat_type=2）：小组首次使用消息功能时懒创建，
 *           成员=组员+指导教师，chat_name 默认"XX组"
 *         会话创建后不删除，消息按 chat_id 归档。
 * ============================================================================
 */
public class TeChat implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 会话ID（主键） */
    private Long id;

    /** 会话类型（1单聊 2群聊） */
    private Long chatType;

    /** 会话名称（群聊名，单聊为空由前端展示对方姓名） */
    private String chatName;

    /** 关联小组ID（群聊时使用，关联 te_group.id） */
    private Long groupId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后消息时间（用于会话列表按最近排序） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 小组名称（联查 te_group.group_name，不落库） */
    private String groupName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getChatType()
    {
        return chatType;
    }

    public void setChatType(Long chatType)
    {
        this.chatType = chatType;
    }

    public String getChatName()
    {
        return chatName;
    }

    public void setChatName(String chatName)
    {
        this.chatName = chatName;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }
}
