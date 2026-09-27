package com.ruoyi.teach.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】成绩实体（对应表 te_score）
 * ----------------------------------------------------------------------------
 * 【说明】阶段7成绩判分落库表：每名学生一条记录（uk_user 唯一）。
 *         - group_score：小组最终分 = Σ(各模块核定分)，教师终审后自动汇总
 *         - final_score：个人最终得分 = Σ(模块核定得分 × 个人该模块贡献率)
 *           （组满分类比：全组拿到 Σ满分=100 时即为贡献率直接加权和）
 *         - is_published：发布开关（0未发布 1已发布，发布后学生可见）
 *         - nickName / studentNo / groupName 为联查展示字段（不落库）
 * ============================================================================
 */
public class TeScore implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成绩ID（主键） */
    private Long id;

    /** 班级ID（关联 te_class.id） */
    private Long classId;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 学生用户ID（关联 sys_user.user_id，唯一键） */
    private Long userId;

    /** 小组最终分（Σ各模块核定分，教师录入口径） */
    private BigDecimal groupScore;

    /** 个人最终得分（Σ模块核定分 × 个人贡献率） */
    private BigDecimal finalScore;

    /** 是否发布（0未发布 1已发布，发布后学生可见） */
    private Long isPublished;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ---------------- 以下为联查展示字段（不落库） ----------------

    /** 学生姓名（联查 sys_user.nick_name） */
    private String nickName;

    /** 学号（联查 te_student.student_no） */
    private String studentNo;

    /** 小组名称（联查 te_group.group_name） */
    private String groupName;

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

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public BigDecimal getGroupScore()
    {
        return groupScore;
    }

    public void setGroupScore(BigDecimal groupScore)
    {
        this.groupScore = groupScore;
    }

    public BigDecimal getFinalScore()
    {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore)
    {
        this.finalScore = finalScore;
    }

    public Long getIsPublished()
    {
        return isPublished;
    }

    public void setIsPublished(Long isPublished)
    {
        this.isPublished = isPublished;
    }

    public Date getPublishTime()
    {
        return publishTime;
    }

    public void setPublishTime(Date publishTime)
    {
        this.publishTime = publishTime;
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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
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
