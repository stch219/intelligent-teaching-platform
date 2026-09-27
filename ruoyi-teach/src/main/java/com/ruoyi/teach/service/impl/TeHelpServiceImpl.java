package com.ruoyi.teach.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.teach.domain.TeHelp;
import com.ruoyi.teach.mapper.TeHelpMapper;
import com.ruoyi.teach.service.ITeHelpService;

/**
 * ============================================================================
 * 【功能】帮助中心服务实现（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】师生端仅可见已发布（status=0）条目；管理员端全量维护，
 *         新增时补创建时间与默认发布状态。
 * ============================================================================
 */
@Service
public class TeHelpServiceImpl implements ITeHelpService
{
    @Autowired
    private TeHelpMapper helpMapper;

    /**
     * 列表查询：publishedOnly=true 过滤下架条目（师生端）
     */
    @Override
    public List<TeHelp> list(TeHelp query, boolean publishedOnly)
    {
        if (publishedOnly)
        {
            query.setStatus("0");
        }
        return helpMapper.selectTeHelpList(query);
    }

    /**
     * 新增（管理员）：默认发布状态，补创建时间
     */
    @Override
    public int insertTeHelp(TeHelp teHelp, String operateBy)
    {
        if (teHelp.getStatus() == null || teHelp.getStatus().isEmpty())
        {
            teHelp.setStatus("0");
        }
        teHelp.setCreateTime(new Date());
        return helpMapper.insertTeHelp(teHelp);
    }

    /**
     * 修改（管理员）
     */
    @Override
    public int updateTeHelp(TeHelp teHelp, String operateBy)
    {
        return helpMapper.updateTeHelp(teHelp);
    }

    /**
     * 批量删除（管理员）
     */
    @Override
    public int deleteTeHelpByIds(Long[] ids)
    {
        return helpMapper.deleteTeHelpByIds(ids);
    }
}
