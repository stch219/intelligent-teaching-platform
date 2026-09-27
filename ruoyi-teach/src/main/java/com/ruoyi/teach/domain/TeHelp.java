package com.ruoyi.teach.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】帮助中心内容实体（对应表 te_help）
 * ----------------------------------------------------------------------------
 * 【说明】师生共用的帮助文档条目：category 作为左侧目录分类（快速上手/
 *         学生端指南/教师端指南/常见问题），title 为条目标题，content 为
 *         富文本内容；管理员可增删改，师生端仅查询已发布（status=0）条目。
 * ============================================================================
 */
public class TeHelp implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 帮助条目ID（主键） */
    private Long id;

    /** 条目标题 */
    private String title;

    /** 分类（快速上手/学生端指南/教师端指南/常见问题，作为左侧目录分组） */
    private String category;

    /** 富文本内容（HTML） */
    private String content;

    /** 同分类内排序号（越小越靠前） */
    private Integer sortOrder;

    /** 状态（0发布 1下架） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
}
