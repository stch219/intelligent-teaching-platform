package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeModuleScoreSet;

/**
 * ============================================================================
 * 【功能】模块赋分 Mapper 接口（表 te_module_score_set，唯一键 class_id+module_code）
 * ============================================================================
 */
public interface TeModuleScoreSetMapper
{
    /** 查询班级下赋分设置（按模块编号排序） */
    public List<TeModuleScoreSet> selectByClassId(Long classId);

    /** 保存单条赋分（存在则更新，不存在则插入） */
    public int upsert(TeModuleScoreSet teModuleScoreSet);

    /** 删除班级下全部赋分（删班级级联） */
    public int deleteByClassId(Long classId);
}
