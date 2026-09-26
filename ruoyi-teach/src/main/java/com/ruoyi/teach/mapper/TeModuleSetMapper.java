package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeModuleSet;

/**
 * ============================================================================
 * 【功能】模块设置 Mapper 接口（表 te_module_set，唯一键 class_id+module_code）
 * ============================================================================
 */
public interface TeModuleSetMapper
{
    /** 查询班级下全部模块设置（按模块编号排序） */
    public List<TeModuleSet> selectByClassId(Long classId);

    /** 保存单条模块设置（存在则更新，不存在则插入） */
    public int upsert(TeModuleSet teModuleSet);

    /** 删除班级下全部模块设置（删班级联） */
    public int deleteByClassId(Long classId);
}
