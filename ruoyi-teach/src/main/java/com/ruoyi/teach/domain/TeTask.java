package com.ruoyi.teach.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * ============================================================================
 * 【功能】课程设计任务实体（对应表 te_task）
 * ----------------------------------------------------------------------------
 * 【说明】教师按班级发布任务，任务编号按字母排序（A/B/C/D…最多8个），
 *         内容包含课程设计任务的四要素：设计目标、具体要求、评分标准、重要提示。
 *         题目由教师直接分配到组（te_task_assign），取消组长选题。
 *         assignedCount 为联查统计字段（已分配组数，不落库）。
 * ============================================================================
 */
public class TeTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID（主键） */
    private Long id;

    /** 任务编号（字母 A/B/C/D…，班内唯一，自动生成） */
    private String taskCode;

    /** 任务题目 */
    private String taskName;

    /** 所属班级ID（关联 te_class.id） */
    private Long classId;

    /** 设计目标 */
    private String designGoal;

    /** 具体要求 */
    private String requirement;

    /** 评分标准 */
    private String gradingStandard;

    /** 重要提示 */
    private String tips;

    /** 提交截止时间（三级预警扫描的时间基准，阶段8新增） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 发布教师用户ID */
    private Long teacherId;

    /** 班级名称（联查 te_class.class_name，不落库） */
    private String className;

    /** 发布教师姓名（联查 sys_user.nick_name，不落库） */
    private String teacherName;

    /** 已分配组数（联查 te_task_assign 统计，不落库） */
    private Long assignedCount;

    /** 分配目标小组ID集合（新增/编辑分配时前端提交，不落库） */
    private Long[] groupIds;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
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

    public String getDesignGoal()
    {
        return designGoal;
    }

    public void setDesignGoal(String designGoal)
    {
        this.designGoal = designGoal;
    }

    public String getRequirement()
    {
        return requirement;
    }

    public void setRequirement(String requirement)
    {
        this.requirement = requirement;
    }

    public String getGradingStandard()
    {
        return gradingStandard;
    }

    public void setGradingStandard(String gradingStandard)
    {
        this.gradingStandard = gradingStandard;
    }

    public String getTips()
    {
        return tips;
    }

    public void setTips(String tips)
    {
        this.tips = tips;
    }

    public Date getDeadline()
    {
        return deadline;
    }

    public void setDeadline(Date deadline)
    {
        this.deadline = deadline;
    }

    public Long getTeacherId()
    {
        return teacherId;
    }

    public void setTeacherId(Long teacherId)
    {
        this.teacherId = teacherId;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
    }

    public Long getAssignedCount()
    {
        return assignedCount;
    }

    public void setAssignedCount(Long assignedCount)
    {
        this.assignedCount = assignedCount;
    }

    public Long[] getGroupIds()
    {
        return groupIds;
    }

    public void setGroupIds(Long[] groupIds)
    {
        this.groupIds = groupIds;
    }
}
