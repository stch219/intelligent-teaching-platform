package com.ruoyi.teach.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * ============================================================================
 * 【功能】学生注册审批实体（对应表 te_registration）
 * ----------------------------------------------------------------------------
 * 【说明】学生通过登录页"注册"入口填写真实姓名/学号/班级/联系电话后，
 *         先落入本表等待审批（status=0 待审批）；
 *         管理员审批通过后正式创建登录账号（sys_user + te_student），
 *         驳回则记录驳回原因。审批通过后回填 user_id 建立账号关联。
 * ============================================================================
 */
public class TeRegistration extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 申请ID（主键，自增） */
    private Long id;

    /** 用户ID（审批通过后回填，关联 sys_user.user_id） */
    private Long userId;

    /** 真实姓名 */
    @Excel(name = "真实姓名")
    private String realName;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 填报班级名称（审批时按名称匹配 te_class） */
    @Excel(name = "班级")
    private String className;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 审批状态（0待审批 1通过 2驳回） */
    @Excel(name = "审批状态", readConverterExp = "0=待审批,1=通过,2=驳回")
    private String status;

    /** 审批人 */
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 申请时间（create_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getApproveBy()
    {
        return approveBy;
    }

    public void setApproveBy(String approveBy)
    {
        this.approveBy = approveBy;
    }

    public Date getApproveTime()
    {
        return approveTime;
    }

    public void setApproveTime(Date approveTime)
    {
        this.approveTime = approveTime;
    }

    public String getRejectReason()
    {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason)
    {
        this.rejectReason = rejectReason;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
