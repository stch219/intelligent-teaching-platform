package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】进度预警规则实体（对应表 te_warning_rule）
 * ----------------------------------------------------------------------------
 * 【说明】教师按班级设定预警规则：距截止提交时间 X 天时，若小组完成率
 *         低于阈值 Y% 则触发对应级别预警（触发与推送在阶段8定时任务实现）。
 *         每个班级最多 3 条规则（对应三级：一般-黄 / 重要-橙 / 紧急-红）。
 * ============================================================================
 */
public class TeWarningRule implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 规则ID（主键） */
    private Long id;

    /** 班级ID（关联 te_class.id） */
    private Long classId;

    /** 预警级别（1一般-黄 2重要-橙 3紧急-红） */
    private Long level;

    /** 距截止提交时间天数（提前量） */
    private Integer daysBefore;

    /** 完成率阈值百分比（低于该值触发） */
    private Integer progressThreshold;

    /** 是否启用（0停用 1启用） */
    private Long enabled;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public Long getLevel()
    {
        return level;
    }

    public void setLevel(Long level)
    {
        this.level = level;
    }

    public Integer getDaysBefore()
    {
        return daysBefore;
    }

    public void setDaysBefore(Integer daysBefore)
    {
        this.daysBefore = daysBefore;
    }

    public Integer getProgressThreshold()
    {
        return progressThreshold;
    }

    public void setProgressThreshold(Integer progressThreshold)
    {
        this.progressThreshold = progressThreshold;
    }

    public Long getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Long enabled)
    {
        this.enabled = enabled;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
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
