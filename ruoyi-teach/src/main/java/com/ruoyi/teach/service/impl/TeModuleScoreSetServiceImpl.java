package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.teach.domain.TeModuleScoreSet;
import com.ruoyi.teach.mapper.TeModuleScoreSetMapper;
import com.ruoyi.teach.service.ITeModuleScoreSetService;

/**
 * ============================================================================
 * 【功能】模块赋分业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 仅对计贡献率的模块（5-10：知识背景/计算步骤/代码实现/
 *      计算结果与分析/心得体会/参考资料）赋分，前4模块不赋分
 *   2. 强制规则：6 个模块必须全部填写，且满分值总和恰好 = 100，
 *      否则整体保存失败（事务回滚）
 *   3. 个人最终得分 = Σ(模块满分 × 个人该模块贡献率)，阶段7计算
 * ============================================================================
 */
@Service
public class TeModuleScoreSetServiceImpl implements ITeModuleScoreSetService
{
    /** 计贡献率模块编号范围（5~10） */
    private static final long MODULE_MIN = 5;

    private static final long MODULE_MAX = 10;

    /** 模块编号 → 模块名称（展示用） */
    private static final String[] MODULE_NAMES = {
            "", "", "", "", "", "知识背景", "计算步骤", "代码实现", "计算结果与分析", "心得体会", "参考资料"
    };

    /** 总分要求（必须恰好等于100） */
    private static final BigDecimal TOTAL_SCORE = new BigDecimal("100");

    @Autowired
    private TeModuleScoreSetMapper moduleScoreSetMapper;

    /**
     * 查询班级赋分设置（固定返回模块5-10共6条，未设置的分数为空）
     */
    @Override
    public List<TeModuleScoreSet> selectByClassId(Long classId)
    {
        List<TeModuleScoreSet> saved = moduleScoreSetMapper.selectByClassId(classId);
        // 组装固定6条返回结构（保证前端渲染顺序与完整性）
        List<TeModuleScoreSet> result = new ArrayList<>();
        for (long code = MODULE_MIN; code <= MODULE_MAX; code++)
        {
            TeModuleScoreSet item = findByCode(saved, code);
            TeModuleScoreSet row = new TeModuleScoreSet();
            row.setClassId(classId);
            row.setModuleCode(code);
            row.setModuleName(MODULE_NAMES[(int) code]);
            row.setScore(item != null ? item.getScore() : null);
            result.add(row);
        }
        return result;
    }

    /**
     * 批量保存赋分（事务：校验通过后整体 upsert，任一校验失败则全部回滚）
     */
    @Transactional
    @Override
    public int saveBatch(Long classId, List<TeModuleScoreSet> list)
    {
        if (list == null || list.size() != (int) (MODULE_MAX - MODULE_MIN + 1))
        {
            throw new ServiceException("必须完整提交模块 5-10 共 6 个模块的赋分");
        }
        BigDecimal sum = BigDecimal.ZERO;
        Set<Long> codes = new HashSet<>();
        Date now = new Date();
        for (TeModuleScoreSet item : list)
        {
            long code = item.getModuleCode() == null ? 0 : item.getModuleCode();
            // 编号必须在 5~10 且不重复
            if (code < MODULE_MIN || code > MODULE_MAX || !codes.add(code))
            {
                throw new ServiceException("模块编号必须为 5-10 且不重复");
            }
            if (item.getScore() == null || item.getScore().compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("模块「" + MODULE_NAMES[(int) code] + "」的满分值必须不小于 0");
            }
            sum = sum.add(item.getScore());
        }
        // 总分强校验：必须恰好等于 100
        if (sum.compareTo(TOTAL_SCORE) != 0)
        {
            throw new ServiceException("各模块满分值总和必须等于 100 分，当前为 " + sum.stripTrailingZeros().toPlainString() + " 分");
        }
        // 全部校验通过后逐条 upsert
        int rows = 0;
        for (TeModuleScoreSet item : list)
        {
            item.setClassId(classId);
            item.setUpdateTime(now);
            if (item.getCreateTime() == null)
            {
                item.setCreateTime(now);
            }
            rows += moduleScoreSetMapper.upsert(item);
        }
        return rows;
    }

    /**
     * 从已保存列表中查找指定模块编号的记录（未找到返回 null）
     */
    private TeModuleScoreSet findByCode(List<TeModuleScoreSet> list, long code)
    {
        for (TeModuleScoreSet item : list)
        {
            if (item.getModuleCode() != null && item.getModuleCode() == code)
            {
                return item;
            }
        }
        return null;
    }
}
