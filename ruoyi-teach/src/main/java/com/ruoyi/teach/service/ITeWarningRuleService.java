package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeWarningRule;

/**
 * ============================================================================
 * 【功能】进度预警规则业务接口（每班最多3条：黄/橙/红三级）
 * ============================================================================
 */
public interface ITeWarningRuleService
{
    /** 查询班级下预警规则列表 */
    public List<TeWarningRule> selectByClassId(Long classId);

    /** 新增规则（上限3条 + 触发组合查重） */
    public int insertTeWarningRule(TeWarningRule rule);

    /** 修改规则（触发值/启停） */
    public int updateTeWarningRule(TeWarningRule rule);

    /** 批量删除规则 */
    public int deleteByIds(Long[] ids);
}
