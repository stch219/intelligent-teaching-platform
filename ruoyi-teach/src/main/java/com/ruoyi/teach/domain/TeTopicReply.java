package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】话题回复实体（对应表 te_topic_reply）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】话题下的楼层回复，支持一层楼中楼（reply_id 指向父回复），
 *         本阶段前端以平铺展示为主（中楼仅显示"回复 @某人"前缀）。
 * ============================================================================
 */
public class TeTopicReply implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 回复ID（主键） */
    private Long id;

    /** 话题ID（关联 te_topic.id） */
    private Long topicId;

    /** 父回复ID（NULL 表示首层回复） */
    private Long replyId;

    /** 回复人用户ID（关联 sys_user.user_id） */
    private Long userId;

    /** 回复内容 */
    private String content;

    /** 回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date replyTime;

    /** 回复人姓名（联查 sys_user.nick_name，不落库） */
    private String nickName;

    /** 被回复人姓名（联查父回复作者，不落库） */
    private String replyToName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTopicId()
    {
        return topicId;
    }

    public void setTopicId(Long topicId)
    {
        this.topicId = topicId;
    }

    public Long getReplyId()
    {
        return replyId;
    }

    public void setReplyId(Long replyId)
    {
        this.replyId = replyId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getReplyTime()
    {
        return replyTime;
    }

    public void setReplyTime(Date replyTime)
    {
        this.replyTime = replyTime;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getReplyToName()
    {
        return replyToName;
    }

    public void setReplyToName(String replyToName)
    {
        this.replyToName = replyToName;
    }
}
