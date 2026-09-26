package com.ruoyi.teach.domain;

import java.util.Date;
import java.math.BigDecimal;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】模块赋分实体（对应表 te_module_score_set）
 * ----------------------------------------------------------------------------
 * 【说明】教师按班级对计贡献率的模块（5-10：知识背景/计算步骤/代码实现/
 *         计算结果与分析/心得体会/参考资料）设置满分值。
 *         强制规则：同班所有模块满分值总和必须恰好等于 100，否则保存失败。
 *         个人最终得分 = Σ(模块满分 × 个人该模块贡献率)（阶段7计算）。
 * ============================================================================
 */
public class TeModuleScoreSet implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 赋分ID（主键） */
    private Long id;

    /** 班级ID（关联 te_class.id） */
    private Long classId;

    /** 模块编号（5-10，前4模块不计贡献率不赋分） */
    private Long moduleCode;

    /** 模块满分值 */
    private BigDecimal score;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 模块名称（前端展示用，不落库） */
    private String moduleName;

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

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
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

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }
}
