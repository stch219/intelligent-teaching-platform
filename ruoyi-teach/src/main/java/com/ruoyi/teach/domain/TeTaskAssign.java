package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】任务分配实体（对应表 te_task_assign）
 * ----------------------------------------------------------------------------
 * 【说明】记录"哪个小组领取哪个任务"（一组一任务，任务可分配给多个组）。
 *         题目由教师统一分配，取消组长选题。
 *         groupName / taskCode / taskName 为联查展示字段（不落库）。
 * ============================================================================
 */
public class TeTaskAssign implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 分配ID（主键） */
    private Long id;

    /** 任务ID（关联 te_task.id） */
    private Long taskId;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date assignTime;

    // ---------------- 以下为联查展示字段（不落库） ----------------

    /** 任务编号（字母） */
    private String taskCode;

    /** 任务题目 */
    private String taskName;

    /** 任务所属班级ID（联查，用于按班级过滤分配视图） */
    private Long classId;

    /** 小组名称 */
    private String groupName;

    /** 组序号（排序用） */
    private Long groupOrder;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Date getAssignTime()
    {
        return assignTime;
    }

    public void setAssignTime(Date assignTime)
    {
        this.assignTime = assignTime;
    }

    public String getTaskCode()
    {
        return taskCode;
    }

    public void setTaskCode(String taskCode)
    {
        this.taskCode = taskCode;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public Long getGroupOrder()
    {
        return groupOrder;
    }

    public void setGroupOrder(Long groupOrder)
    {
        this.groupOrder = groupOrder;
    }
}
