package com.ruoyi.web.controller.teach;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.domain.TeHelp;
import com.ruoyi.teach.service.ITeHelpService;

/**
 * ============================================================================
 * 【功能】帮助中心控制器（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】师生共用查询入口（仅已发布条目，按分类分组展示）；
 *         管理员负责内容维护（新增/修改/删除/上下架）。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/help")
public class TeHelpController extends BaseController
{
    @Autowired
    private ITeHelpService helpService;

    /**
     * 帮助条目列表（师生端仅已发布；管理端传 all=true 查全量）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher,admin')")
    @GetMapping("/list")
    public AjaxResult list(TeHelp query)
    {
        // admin 走管理端口径（可看下架条目），师生仅看已发布
        boolean publishedOnly = !isAdminUser();
        return success(helpService.list(query, publishedOnly));
    }

    /**
     * 新增帮助条目（管理员）
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @PostMapping
    public AjaxResult add(@RequestBody TeHelp teHelp)
    {
        return toAjax(helpService.insertTeHelp(teHelp, getUsername()));
    }

    /**
     * 修改帮助条目（管理员）
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @PutMapping
    public AjaxResult edit(@RequestBody TeHelp teHelp)
    {
        return toAjax(helpService.updateTeHelp(teHelp, getUsername()));
    }

    /**
     * 批量删除帮助条目（管理员）
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(helpService.deleteTeHelpByIds(ids));
    }

    /**
     * 当前登录用户是否 admin 超管（辅助判定查询口径）
     */
    private boolean isAdminUser()
    {
        try
        {
            return com.ruoyi.common.utils.SecurityUtils.isAdmin(getUserId());
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
