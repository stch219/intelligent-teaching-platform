package com.ruoyi.teach.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * ============================================================================
 * 【功能】班级实体（对应表 te_class）
 * ----------------------------------------------------------------------------
 * 【说明】教师创建指导班级，一个班级包含：截止提交时间、分组数（4/5/6）、
 *         学生名单与若干小组（组名字母 A/B/C/D/E/F）。
 *         教师与班级通过 teacher_id 绑定（师生私有绑定：非本班师生不可见）。
 *         studentCount / groupNum 为联查统计字段（不落库）。
 * ============================================================================
 */
public class TeClass extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 班级ID（主键） */
    private Long id;

    /** 班级名称（如：车辆2301班） */
    private String className;

    /** 指导教师用户ID（关联 sys_user.user_id） */
    private Long teacherId;

    /** 分组数（4/5/6，按学号末2位对组数取模自动分组） */
    private Long groupCount;

    /** 课程设计任务截止提交时间（不足7天前端倒计时标红） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 状态（0正常 1停用） */
    private String status;

    // ---------------- 以下为联查/统计展示字段（不落库） ----------------

    /** 指导教师姓名（联查 sys_user.nick_name） */
    private String teacherName;

    /** 班级学生数（联查 te_student 统计） */
    private Long studentCount;

    /** 班级小组数（联查 te_group 统计） */
    private Long groupNum;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public Long getTeacherId()
    {
        return teacherId;
    }

    public void setTeacherId(Long teacherId)
    {
        this.teacherId = teacherId;
    }

    public Long getGroupCount()
    {
        return groupCount;
    }

    public void setGroupCount(Long groupCount)
    {
        this.groupCount = groupCount;
    }

    public Date getDeadline()
    {
        return deadline;
    }

    public void setDeadline(Date deadline)
    {
        this.deadline = deadline;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
    }

    public Long getStudentCount()
    {
        return studentCount;
    }

    public void setStudentCount(Long studentCount)
    {
        this.studentCount = studentCount;
    }

    public Long getGroupNum()
    {
        return groupNum;
    }

    public void setGroupNum(Long groupNum)
    {
        this.groupNum = groupNum;
    }
}
