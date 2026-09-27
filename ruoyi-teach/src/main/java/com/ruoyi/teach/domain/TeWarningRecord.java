package com.ruoyi.teach.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】进度预警记录实体（对应表 te_warning_record）
 * ----------------------------------------------------------------------------
 * 【说明】教师触发「立即扫描」后由预警引擎生成：按班级启用的规则判定各组
 *         完成率，命中则落库记录并通过 WebSocket 实时推送组内学生。
 *         level 语义与规则表一致：1一般-黄 2重要-橙 3紧急-红；
 *         同组同级别幂等（已有未处置记录时仅刷新发送时间，不重复插入）。
 * ============================================================================
 */
public class TeWarningRecord implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 记录ID（主键） */
    private Long id;

    /** 班级ID（关联 te_class.id） */
    private Long classId;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 预警级别（1一般-黄 2重要-橙 3紧急-红） */
    private Long level;

    /** 预警内容（人话描述：组名+触发原因+当前完成率） */
    private String content;

    /** 触发时的小组完成率百分比（0-100） */
    private Integer progress;

    /** 发送（触发）时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** 处置状态（0未处置 1已忽略/已处置） */
    private Long resolved;

    /** ========== 以下为联查展示字段（不落库） ========== */

    /** 小组名称（联查 te_group.group_name） */
    private String groupName;

    /** 班级名称（联查 te_class.class_name） */
    private String className;

    /** 任务名称（联查分配到该组的任务名，展示用） */
    private String taskName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Long getLevel()
    {
        return level;
    }

    public void setLevel(Long level)
    {
        this.level = level;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getProgress()
    {
        return progress;
    }

    public void setProgress(Integer progress)
    {
        this.progress = progress;
    }

    public Date getSendTime()
    {
        return sendTime;
    }

    public void setSendTime(Date sendTime)
    {
        this.sendTime = sendTime;
    }

    public Long getResolved()
    {
        return resolved;
    }

    public void setResolved(Long resolved)
    {
        this.resolved = resolved;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }
}
