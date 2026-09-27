package com.ruoyi.teach.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】模块贡献率实体（对应表 te_contribution）
 * ----------------------------------------------------------------------------
 * 【说明】模块5-10需分配贡献率且每模块组内合计100%（前4模块不计贡献率）。
 *         - 组长负责分配（保存时清空该模块旧记录并重置确认状态）
 *         - confirmed：组员是否已确认本组分配（0未确认 1已确认）
 *         - 联查字段 nickName / studentNo 用于前端展示，不落库
 * ============================================================================
 */
public class TeContribution implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 贡献率ID（主键） */
    private Long id;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 模块编号（5-10） */
    private Long moduleCode;

    /** 组员用户ID */
    private Long userId;

    /** 贡献率百分比（0-100） */
    private BigDecimal ratio;

    /** 组员是否已确认（0未确认 1已确认） */
    private Long confirmed;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ---------------- 以下为联查展示字段（不落库） ----------------

    /** 组员姓名（联查 sys_user.nick_name） */
    private String nickName;

    /** 组员学号（联查 te_student.student_no） */
    private String studentNo;

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

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public BigDecimal getRatio()
    {
        return ratio;
    }

    public void setRatio(BigDecimal ratio)
    {
        this.ratio = ratio;
    }

    public Long getConfirmed()
    {
        return confirmed;
    }

    public void setConfirmed(Long confirmed)
    {
        this.confirmed = confirmed;
    }

    public Date getConfirmTime()
    {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime)
    {
        this.confirmTime = confirmTime;
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
}
