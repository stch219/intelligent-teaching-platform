package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】模块讨论话题实体（对应表 te_topic）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】学生围绕某一模块（编号1-10，讨论标识用任务序小写字母）
 *         发起小组内讨论话题，组员与指导教师均可回复。
 *         replyCount 为联查统计字段（不落库）。
 * ============================================================================
 */
public class TeTopic implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 话题ID（主键） */
    private Long id;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 关联模块编号（1-10；讨论标识小写abcd按任务序） */
    private Long moduleCode;

    /** 话题标题 */
    private String title;

    /** 发起人用户ID（关联 sys_user.user_id） */
    private Long creatorId;

    /** 发起时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后回复时间（用于排序） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 发起人姓名（联查 sys_user.nick_name，不落库） */
    private String creatorName;

    /** 小组名称（联查 te_group.group_name，不落库） */
    private String groupName;

    /** 回复数量（联查统计 te_topic_reply，不落库） */
    private Long replyCount;

    /** 班级ID（仅作查询条件：教师查本班全部小组话题，不落库） */
    private Long classId;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Long getModuleCode()
    {
        return moduleCode;
    }

    public void setModuleCode(Long moduleCode)
    {
        this.moduleCode = moduleCode;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
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

    public String getCreatorName()
    {
        return creatorName;
    }

    public void setCreatorName(String creatorName)
    {
        this.creatorName = creatorName;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public Long getReplyCount()
    {
        return replyCount;
    }

    public void setReplyCount(Long replyCount)
    {
        this.replyCount = replyCount;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }
}
