package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeModuleScoreSet;

/**
 * ============================================================================
 * 【功能】模块赋分业务接口（模块5-10赋分，总和必须=100）
 * ============================================================================
 */
public interface ITeModuleScoreSetService
{
    /** 查询班级赋分设置（返回模块5-10列表，未设置时分数为空） */
    public List<TeModuleScoreSet> selectByClassId(Long classId);

    /** 批量保存赋分（强校验：6个模块齐全且总分恰好=100） */
    public int saveBatch(Long classId, List<TeModuleScoreSet> list);
}
