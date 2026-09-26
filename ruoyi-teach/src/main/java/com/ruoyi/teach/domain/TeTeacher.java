package com.ruoyi.teach.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * ============================================================================
 * 【功能】教师扩展信息实体（对应表 te_teacher）
 * ----------------------------------------------------------------------------
 * 【说明】教师登录账号复用系统用户表 sys_user，本表仅扩展教学业务属性（职称）。
 *         管理员创建教师时：先建 sys_user 并授予"教师"角色，再写本表扩展记录；
 *         全平台教师数量上限 4 名（创建前在 Service 层校验）。
 *         列表展示时联查 sys_user 拿账号/姓名/手机/状态等字段。
 * ============================================================================
 */
public class TeTeacher extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID（主键，关联 sys_user.user_id） */
    private Long userId;

    /** 登录账号（联查 sys_user.user_name） */
    @Excel(name = "登录账号")
    private String userName;

    /** 教师姓名（联查 sys_user.nick_name） */
    @Excel(name = "教师姓名")
    private String nickName;

    /** 职称 */
    @Excel(name = "职称")
    private String title;

    /** 联系电话（联查 sys_user.phonenumber） */
    @Excel(name = "联系电话")
    private String phonenumber;

    /** 邮箱（联查 sys_user.email） */
    private String email;

    /** 账号状态（联查 sys_user.status：0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 指导班级数（联查 te_class 统计，删除校验用：仅可删除无指导班级的教师） */
    private Long classCount;

    /** 重置密码用明文密码（仅前后端传递，不落库） */
    private String password;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注（父类 remark 已有，此处覆盖使用） */

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getClassCount()
    {
        return classCount;
    }

    public void setClassCount(Long classCount)
    {
        this.classCount = classCount;
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
