package com.ruoyi.teach.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.teach.domain.TeWarningRule;
import com.ruoyi.teach.mapper.TeWarningRuleMapper;
import com.ruoyi.teach.service.ITeWarningRuleService;

/**
 * ============================================================================
 * 【功能】进度预警规则业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 每个班级最多 3 条规则（对应三级预警：1一般-黄 2重要-橙 3紧急-红）
 *   2. 规则语义：距截止提交 X 天时，小组完成率低于 Y% 则触发对应级别预警
 *   3. 同班内"级别+提前天数+阈值"完全相同的规则视为重复，拒绝新增
 *   4. 触发计算由阶段8定时任务执行，本模块仅维护规则
 * ============================================================================
 */
@Service
public class TeWarningRuleServiceImpl implements ITeWarningRuleService
{
    /** 每班预警规则上限 */
    private static final int MAX_RULE_COUNT = 3;

    @Autowired
    private TeWarningRuleMapper warningRuleMapper;

    /**
     * 查询班级下预警规则列表
     */
    @Override
    public List<TeWarningRule> selectByClassId(Long classId)
    {
        return warningRuleMapper.selectByClassId(classId);
    }

    /**
     * 新增规则（上限校验 + 查重 + 取值合法性）
     */
    @Override
    public int insertTeWarningRule(TeWarningRule rule)
    {
        // 取值校验
        validate(rule);
        // 上限校验：每班最多3条
        if (warningRuleMapper.countByClassId(rule.getClassId()) >= MAX_RULE_COUNT)
        {
            throw new ServiceException("每个班级最多设置 " + MAX_RULE_COUNT + " 条预警规则（对应三级预警）");
        }
        // 查重：同班同级别+提前天数+阈值 完全一致视为重复
        for (TeWarningRule exist : warningRuleMapper.selectByClassId(rule.getClassId()))
        {
            if (exist.getLevel().equals(rule.getLevel())
                    && exist.getDaysBefore().equals(rule.getDaysBefore())
                    && exist.getProgressThreshold().equals(rule.getProgressThreshold()))
            {
                throw new ServiceException("已存在相同触发条件的规则，请勿重复添加");
            }
        }
        rule.setEnabled(rule.getEnabled() == null ? 1L : rule.getEnabled());
        rule.setCreateTime(new Date());
        return warningRuleMapper.insertTeWarningRule(rule);
    }

    /**
     * 修改规则
     */
    @Override
    public int updateTeWarningRule(TeWarningRule rule)
    {
        validate(rule);
        return warningRuleMapper.updateTeWarningRule(rule);
    }

    /**
     * 批量删除规则
     */
    @Override
    public int deleteByIds(Long[] ids)
    {
        return warningRuleMapper.deleteByIds(ids);
    }

    /**
     * 取值合法性校验
     */
    private void validate(TeWarningRule rule)
    {
        if (rule.getClassId() == null || rule.getLevel() == null)
        {
            throw new ServiceException("班级与预警级别为必填项");
        }
        if (rule.getLevel() < 1 || rule.getLevel() > 3)
        {
            throw new ServiceException("预警级别取值 1(一般)/2(重要)/3(紧急)");
        }
        if (rule.getDaysBefore() == null || rule.getDaysBefore() < 1 || rule.getDaysBefore() > 90)
        {
            throw new ServiceException("距截止天数须在 1~90 之间");
        }
        if (rule.getProgressThreshold() == null || rule.getProgressThreshold() < 1
                || rule.getProgressThreshold() > 99)
        {
            throw new ServiceException("完成率阈值须在 1~99 之间");
        }
    }
}
