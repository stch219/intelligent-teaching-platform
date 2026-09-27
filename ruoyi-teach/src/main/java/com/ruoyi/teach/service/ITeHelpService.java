package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeHelp;

/**
 * ============================================================================
 * 【功能】帮助中心服务接口（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】师生端查询已发布条目（按分类分组）；管理员端维护增删改与上下架。
 * ============================================================================
 */
public interface ITeHelpService
{
    /**
     * 查询帮助条目列表（published=true 时仅返回已发布，供师生端；false 全量，供管理端）
     */
    List<TeHelp> list(TeHelp query, boolean publishedOnly);

    /**
     * 新增帮助条目（管理员）
     */
    int insertTeHelp(TeHelp teHelp, String operateBy);

    /**
     * 修改帮助条目（管理员）
     */
    int updateTeHelp(TeHelp teHelp, String operateBy);

    /**
     * 批量删除帮助条目（管理员）
     */
    int deleteTeHelpByIds(Long[] ids);
}
