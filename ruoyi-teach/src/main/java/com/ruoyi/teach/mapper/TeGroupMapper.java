package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeGroup;

/**
 * ============================================================================
 * 【功能】小组管理 Mapper 接口（表 te_group）
 * ============================================================================
 */
public interface TeGroupMapper
{
    /** 查询班级下小组列表（按组序号排序） */
    public List<TeGroup> selectByClassId(Long classId);

    /** 按ID查询小组 */
    public TeGroup selectById(Long id);

    /** 统计班级下小组数 */
    public int countByClassId(Long classId);

    /** 新增小组 */
    public int insertTeGroup(TeGroup teGroup);

    /** 修改小组 */
    public int updateTeGroup(TeGroup teGroup);

    /** 删除班级下全部小组（调整分组数/删班时使用） */
    public int deleteByClassId(Long classId);
}
