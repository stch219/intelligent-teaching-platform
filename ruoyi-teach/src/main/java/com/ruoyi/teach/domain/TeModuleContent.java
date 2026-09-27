package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】模块内容实体（对应表 te_module_content）
 * ----------------------------------------------------------------------------
 * 【说明】小组协同编辑核心表：每组 × 每模块一条记录（uk_group_module 唯一）。
 *         - content：富文本 HTML（支持图片/公式/代码块）
 *         - status 三态：0未开展 1暂存 2已提交（提交后锁定不可再编辑）
 *         - progress：暂存进度百分比（0-100，按字数/条目折算）
 *         - version：乐观锁版本号（组员同时编辑防覆盖，更新时校验）
 *         - word_count / item_count：去 HTML 标签后的有效字数/条目数
 * ============================================================================
 */
public class TeModuleContent implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 内容ID（主键） */
    private Long id;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 模块编号（1-10） */
    private Long moduleCode;

    /** 富文本内容（HTML） */
    private String content;

    /** 状态（0未开展 1暂存 2已提交） */
    private Long status;

    /** 暂存进度百分比（0-100） */
    private Long progress;

    /** 乐观锁版本号（协同编辑防覆盖） */
    private Long version;

    /** 当前有效字数（去HTML标签统计） */
    private Long wordCount;

    /** 当前条目数（参考资料模块使用） */
    private Long itemCount;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后编辑者 */
    private String updateBy;

    /** 最后编辑时间 */
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

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getStatus()
    {
        return status;
    }

    public void setStatus(Long status)
    {
        this.status = status;
    }

    public Long getProgress()
    {
        return progress;
    }

    public void setProgress(Long progress)
    {
        this.progress = progress;
    }

    public Long getVersion()
    {
        return version;
    }

    public void setVersion(Long version)
    {
        this.version = version;
    }

    public Long getWordCount()
    {
        return wordCount;
    }

    public void setWordCount(Long wordCount)
    {
        this.wordCount = wordCount;
    }

    public Long getItemCount()
    {
        return itemCount;
    }

    public void setItemCount(Long itemCount)
    {
        this.itemCount = itemCount;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
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
