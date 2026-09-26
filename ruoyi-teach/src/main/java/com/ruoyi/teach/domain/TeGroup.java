package com.ruoyi.teach.domain;

import java.util.Date;
import java.util.List;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】小组实体（对应表 te_group）
 * ----------------------------------------------------------------------------
 * 【说明】组名按字母命名（A组/B组/…），班级创建时按分组数自动生成，
 *         每组拥有专属色条底纹颜色。学生通过 te_student.group_id 归属小组；
 *         任务通过 te_task_assign 分配到组（一组一任务）。
 *         members / taskName 等为分组看板联查展示字段（不落库）。
 * ============================================================================
 */
public class TeGroup implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 小组ID（主键） */
    private Long id;

    /** 小组名称（A组/B组/…按字母命名） */
    private String groupName;

    /** 组序号（1..6，自动分组规则依据：学号末2位对组数取模，余0归最后一组） */
    private Long groupOrder;

    /** 所属班级ID（关联 te_class.id） */
    private Long classId;

    /** 专属色条底纹颜色值（如 #409EFF） */
    private String colorCode;

    /** 总稿提交状态（0未提交 1已提交，阶段5使用） */
    private Long submitStatus;

    /** 总稿提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 总稿PDF文件路径（阶段5使用） */
    private String finalPdf;

    /** AI辅助参考分（仅教师可见） */
    private Double aiRefScore;

    /** 教师录入的组最终分（发布前学生不可见） */
    private Double teacherScore;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ---------------- 以下为联查/组装展示字段（不落库） ----------------

    /** 组内成员列表（分组看板组装） */
    private List<TeStudent> members;

    /** 组内成员数 */
    private Long memberCount;

    /** 已分配任务的编号（如 A，联查 te_task_assign + te_task） */
    private String taskCode;

    /** 已分配任务的题目名称 */
    private String taskName;

    /** 已分配任务ID */
    private Long taskId;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
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

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public String getColorCode()
    {
        return colorCode;
    }

    public void setColorCode(String colorCode)
    {
        this.colorCode = colorCode;
    }

    public Long getSubmitStatus()
    {
        return submitStatus;
    }

    public void setSubmitStatus(Long submitStatus)
    {
        this.submitStatus = submitStatus;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public String getFinalPdf()
    {
        return finalPdf;
    }

    public void setFinalPdf(String finalPdf)
    {
        this.finalPdf = finalPdf;
    }

    public Double getAiRefScore()
    {
        return aiRefScore;
    }

    public void setAiRefScore(Double aiRefScore)
    {
        this.aiRefScore = aiRefScore;
    }

    public Double getTeacherScore()
    {
        return teacherScore;
    }

    public void setTeacherScore(Double teacherScore)
    {
        this.teacherScore = teacherScore;
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

    public List<TeStudent> getMembers()
    {
        return members;
    }

    public void setMembers(List<TeStudent> members)
    {
        this.members = members;
    }

    public Long getMemberCount()
    {
        return memberCount;
    }

    public void setMemberCount(Long memberCount)
    {
        this.memberCount = memberCount;
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

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }
}
