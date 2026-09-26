package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeMaterial;

/**
 * ============================================================================
 * 【功能】学习资料 Mapper 接口（表 te_material，联查班级名称）
 * ============================================================================
 */
public interface TeMaterialMapper
{
    /** 查询资料列表（按班级过滤） */
    public List<TeMaterial> selectTeMaterialList(TeMaterial teMaterial);

    /** 按ID查询资料 */
    public TeMaterial selectTeMaterialById(Long id);

    /** 新增资料记录 */
    public int insertTeMaterial(TeMaterial teMaterial);

    /** 修改资料记录（名称/可见性） */
    public int updateTeMaterial(TeMaterial teMaterial);

    /** 批量删除资料记录 */
    public int deleteTeMaterialByIds(Long[] ids);

    /** 删除班级下全部资料（删班级级联） */
    public int deleteByClassId(Long classId);
}
