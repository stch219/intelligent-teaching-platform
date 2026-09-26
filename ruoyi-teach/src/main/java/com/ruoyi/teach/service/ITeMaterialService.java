package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeMaterial;

/**
 * ============================================================================
 * 【功能】学习资料业务接口（教师发布资料，参考答案严格保密）
 * ============================================================================
 */
public interface ITeMaterialService
{
    /** 查询资料列表（按班级过滤） */
    public List<TeMaterial> selectTeMaterialList(TeMaterial teMaterial);

    /** 新增资料记录（参考答案类型强制学生不可见） */
    public int insertTeMaterial(TeMaterial teMaterial, String operateBy);

    /** 修改资料（名称/可见性；参考答案类型永不可见） */
    public int updateTeMaterial(TeMaterial teMaterial);

    /** 批量删除资料记录 */
    public int deleteTeMaterialByIds(Long[] ids);
}
