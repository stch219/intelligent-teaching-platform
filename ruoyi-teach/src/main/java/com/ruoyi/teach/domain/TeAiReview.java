package com.ruoyi.teach.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】AI批改结果实体（对应表 te_ai_review）
 * ----------------------------------------------------------------------------
 * 【说明】阶段7核心表：每组 × 每计分模块（5-10）一条批改记录。
 *         - ai_score：AI给出的模块参考分（按模块满分折算后的绝对分，≤模块满分）
 *         - review_process：批改过程 JSON（优点/问题/修改建议，教师可见可核查）
 *         - model_name：批改来源标识（模型名 / rule-sim 本地规则模拟）
 *         - teacher_score：教师终审核定分（同口径绝对分，NULL 表示未调整，取分时优先于 AI 分）
 *         - similarity：与参考答案相似度（暂无参考答案数据时为 NULL）
 *         - moduleName / moduleMax 为联查展示字段（不落库）
 * ============================================================================
 */
public class TeAiReview implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 批改ID（主键） */
    private Long id;

    /** 小组ID（关联 te_group.id） */
    private Long groupId;

    /** 模块编号（5-10 计分模块） */
    private Long moduleCode;

    /** 批改过程（JSON：优点/问题定位/修改建议） */
    private String reviewProcess;

    /** AI参考分（按模块满分折算后的绝对分，≤模块满分） */
    private BigDecimal aiScore;

    /** 与参考答案相似度（0-100，无参考答案时为空） */
    private BigDecimal similarity;

    /** 使用的模型名称（rule-sim 表示本地规则模拟兜底） */
    private String modelName;

    /** 教师核定分（终审调整后，NULL表示未调整） */
    private BigDecimal teacherScore;

    /** 教师是否已核查（0未核查 1已核查） */
    private Long teacherChecked;

    /** 核查教师 */
    private String checkedBy;

    /** 批改时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    // ---------------- 以下为联查/组装展示字段（不落库） ----------------

    /** 模块名称（联查展示） */
    private String moduleName;

    /** 模块满分值（联查 te_module_score_set） */
    private BigDecimal moduleMax;

    /** 生效分（teacher_score 优先，否则 ai_score；均为模块绝对分，前端展示用） */
    public BigDecimal getFinalScore()
    {
        return teacherScore != null ? teacherScore : aiScore;
    }

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

    public String getReviewProcess()
    {
        return reviewProcess;
    }

    public void setReviewProcess(String reviewProcess)
    {
        this.reviewProcess = reviewProcess;
    }

    public BigDecimal getAiScore()
    {
        return aiScore;
    }

    public void setAiScore(BigDecimal aiScore)
    {
        this.aiScore = aiScore;
    }

    public BigDecimal getSimilarity()
    {
        return similarity;
    }

    public void setSimilarity(BigDecimal similarity)
    {
        this.similarity = similarity;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public BigDecimal getTeacherScore()
    {
        return teacherScore;
    }

    public void setTeacherScore(BigDecimal teacherScore)
    {
        this.teacherScore = teacherScore;
    }

    public Long getTeacherChecked()
    {
        return teacherChecked;
    }

    public void setTeacherChecked(Long teacherChecked)
    {
        this.teacherChecked = teacherChecked;
    }

    public String getCheckedBy()
    {
        return checkedBy;
    }

    public void setCheckedBy(String checkedBy)
    {
        this.checkedBy = checkedBy;
    }

    public Date getReviewTime()
    {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime)
    {
        this.reviewTime = reviewTime;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public BigDecimal getModuleMax()
    {
        return moduleMax;
    }

    public void setModuleMax(BigDecimal moduleMax)
    {
        this.moduleMax = moduleMax;
    }
}
