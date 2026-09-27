package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】班级公告实体（对应表 te_announcement）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】教师面向自己指导的班级发布公告，学生端"班级公告"页可见。
 *         发布成功后通过 WebSocket 向该班在线学生推送提醒通知。
 * ============================================================================
 */
public class TeAnnouncement implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 公告ID（主键） */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 发布目标班级ID（关联 te_class.id） */
    private Long classId;

    /** 发布教师姓名 */
    private String publishBy;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 班级名称（联查 te_class.class_name，不落库） */
    private String className;

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

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public String getPublishBy()
    {
        return publishBy;
    }

    public void setPublishBy(String publishBy)
    {
        this.publishBy = publishBy;
    }

    public Date getPublishTime()
    {
        return publishTime;
    }

    public void setPublishTime(Date publishTime)
    {
        this.publishTime = publishTime;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
