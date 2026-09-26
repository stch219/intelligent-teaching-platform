package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeModuleSet;

/**
 * ============================================================================
 * 【功能】模块设置业务接口（10大模块字数区间/能力开关，同步学生端）
 * ============================================================================
 */
public interface ITeModuleSetService
{
    /** 查询班级模块设置（无记录时返回10条平台默认配置） */
    public List<TeModuleSet> selectByClassId(Long classId);

    /** 批量保存模块设置（10条整体提交，upsert） */
    public int saveBatch(Long classId, List<TeModuleSet> list, String operateBy);
}
