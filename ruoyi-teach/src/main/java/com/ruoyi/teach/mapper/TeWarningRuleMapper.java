package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeWarningRule;

/**
 * ============================================================================
 * 【功能】预警规则 Mapper 接口（表 te_warning_rule，每班最多3条）
 * ============================================================================
 */
public interface TeWarningRuleMapper
{
    /** 查询班级下预警规则列表 */
    public List<TeWarningRule> selectByClassId(Long classId);

    /** 统计班级下规则数（新增前校验最多3条） */
    public int countByClassId(Long classId);

    /** 新增规则 */
    public int insertTeWarningRule(TeWarningRule teWarningRule);

    /** 修改规则（触发值/启停） */
    public int updateTeWarningRule(TeWarningRule teWarningRule);

    /** 批量删除规则 */
    public int deleteByIds(Long[] ids);

    /** 删除班级下全部规则（删班级级联） */
    public int deleteByClassId(Long classId);
}
