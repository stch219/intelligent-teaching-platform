package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】模块设置实体（对应表 te_module_set）
 * ----------------------------------------------------------------------------
 * 【说明】教师按班级设置课程设计各模块的编辑规则，设置同步到学生端：
 *         - 校验类型：字数 / 条目数（参考资料模块按条目，3-20条）
 *         - 字数（条目）区间：min ~ max（max=0 不限）
 *         - 能力开关：公式录入 / 图片上传 / 代码块
 *         - 是否需要学生编辑（封面自动生成、任务要求/参数配置为只读展示）
 *         模块顺序全平台统一（1-10）：封面 任务要求 角色与分工 参数配置
 *         知识背景 计算步骤 代码实现 计算结果与分析 心得体会 参考资料
 * ============================================================================
 */
public class TeModuleSet implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 设置ID（主键） */
    private Long id;

    /** 班级ID（关联 te_class.id） */
    private Long classId;

    /** 模块编号（1-10） */
    private Long moduleCode;

    /** 模块名称 */
    private String moduleName;

    /** 校验类型（1字数 2条目数） */
    private Long countType;

    /** 最低要求（字数或条目数） */
    private Long minCount;

    /** 最高要求（字数或条目数，0为不限） */
    private Long maxCount;

    /** 是否支持公式录入（0否 1是） */
    private Long needFormula;

    /** 是否支持图片上传/粘贴（0否 1是） */
    private Long needImage;

    /** 是否支持代码块（0否 1是） */
    private Long needCode;

    /** 是否需要学生编辑（0否如封面/参数 1是） */
    private Long editable;

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

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public Long getModuleCode()
    {
        return moduleCode;
    }

    public void setModuleCode(Long moduleCode)
    {
        this.moduleCode = moduleCode;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public Long getCountType()
    {
        return countType;
    }

    public void setCountType(Long countType)
    {
        this.countType = countType;
    }

    public Long getMinCount()
    {
        return minCount;
    }

    public void setMinCount(Long minCount)
    {
        this.minCount = minCount;
    }

    public Long getMaxCount()
    {
        return maxCount;
    }

    public void setMaxCount(Long maxCount)
    {
        this.maxCount = maxCount;
    }

    public Long getNeedFormula()
    {
        return needFormula;
    }

    public void setNeedFormula(Long needFormula)
    {
        this.needFormula = needFormula;
    }

    public Long getNeedImage()
    {
        return needImage;
    }

    public void setNeedImage(Long needImage)
    {
        this.needImage = needImage;
    }

    public Long getNeedCode()
    {
        return needCode;
    }

    public void setNeedCode(Long needCode)
    {
        this.needCode = needCode;
    }

    public Long getEditable()
    {
        return editable;
    }

    public void setEditable(Long editable)
    {
        this.editable = editable;
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
