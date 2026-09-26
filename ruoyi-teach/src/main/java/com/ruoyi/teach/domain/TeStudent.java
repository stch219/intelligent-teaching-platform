package com.ruoyi.teach.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * ============================================================================
 * 【功能】学生扩展信息实体（对应表 te_student）
 * ----------------------------------------------------------------------------
 * 【说明】学生登录账号复用系统用户表 sys_user，本表扩展学籍与分组属性：
 *         学号、班级、小组、组内角色（组长/汇报人/组长兼汇报人/成员）、
 *         个人分工与分工确认状态。
 *         来源两种：①注册审批通过后自动创建；②管理员 Excel 批量导入。
 *         列表联查 sys_user（账号/姓名/手机/状态）、te_class（班级名）、
 *         te_group（组名），支持"班级→小组"层级树筛选展示。
 * ============================================================================
 */
public class TeStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID（主键，关联 sys_user.user_id） */
    private Long userId;

    /** 学号（同时作为登录账号） */
    @Excel(name = "学号")
    private String studentNo;

    /** 所属班级ID（关联 te_class.id） */
    private Long classId;

    /** 所属小组ID（关联 te_group.id） */
    private Long groupId;

    /** 组内角色（1组长 2汇报人 3组长兼汇报人 4成员） */
    private Long roleType;

    /** 个人分工说明（提交组长确认） */
    private String dutyAssignment;

    /** 分工确认状态（0待组长确认 1已确认） */
    private Long dutyStatus;

    // ---------------- 以下为联查展示字段（不落库） ----------------

    /** 登录账号（联查 sys_user.user_name） */
    @Excel(name = "登录账号")
    private String userName;

    /** 学生姓名（联查 sys_user.nick_name） */
    @Excel(name = "姓名")
    private String nickName;

    /** 联系电话（联查 sys_user.phonenumber） */
    @Excel(name = "联系电话")
    private String phonenumber;

    /** 账号状态（联查 sys_user.status：0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 班级名称（联查 te_class.class_name） */
    @Excel(name = "班级")
    private String className;

    /** 小组名称（联查 te_group.group_name） */
    private String groupName;

    /** 组内角色文本（前端展示用） */
    private String roleTypeName;

    /** 重置密码用明文密码（仅前后端传递，不落库） */
    private String password;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
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

    public Long getRoleType()
    {
        return roleType;
    }

    public void setRoleType(Long roleType)
    {
        this.roleType = roleType;
    }

    public String getDutyAssignment()
    {
        return dutyAssignment;
    }

    public void setDutyAssignment(String dutyAssignment)
    {
        this.dutyAssignment = dutyAssignment;
    }

    public Long getDutyStatus()
    {
        return dutyStatus;
    }

    public void setDutyStatus(Long dutyStatus)
    {
        this.dutyStatus = dutyStatus;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getRoleTypeName()
    {
        return roleTypeName;
    }

    public void setRoleTypeName(String roleTypeName)
    {
        this.roleTypeName = roleTypeName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
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

    @Override
    public Date getUpdateTime()
    {
        return updateTime;
    }

    @Override
    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
