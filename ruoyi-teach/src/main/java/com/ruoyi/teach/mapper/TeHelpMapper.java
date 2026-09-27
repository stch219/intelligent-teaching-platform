package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeHelp;

/**
 * ============================================================================
 * 【功能】帮助中心 Mapper（表 te_help）
 * ----------------------------------------------------------------------------
 * 【说明】师生端按分类查询已发布条目；管理员端维护（增删改、上下架）。
 * ============================================================================
 */
public interface TeHelpMapper
{
    /**
     * 条件查询帮助条目列表（category/status/sortOrder 排序）
     */
    List<TeHelp> selectTeHelpList(TeHelp teHelp);

    /**
     * 按ID查询帮助条目
     */
    TeHelp selectTeHelpById(Long id);

    /**
     * 新增帮助条目
     */
    int insertTeHelp(TeHelp teHelp);

    /**
     * 修改帮助条目
     */
    int updateTeHelp(TeHelp teHelp);

    /**
     * 批量删除帮助条目
     */
    int deleteTeHelpByIds(Long[] ids);
}
