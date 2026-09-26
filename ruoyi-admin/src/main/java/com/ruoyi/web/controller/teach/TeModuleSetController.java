package com.ruoyi.web.controller.teach;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.teach.domain.TeModuleSet;
import com.ruoyi.teach.service.ITeModuleSetService;

/**
 * ============================================================================
 * 【功能】模块设置控制器（教师端门户）
 * ----------------------------------------------------------------------------
 * 【说明】按班级维护 10 大模块的字数/条目区间与能力开关（公式/图片/代码），
 *         保存后同步学生端编辑规则。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/moduleset")
public class TeModuleSetController extends BaseController
{
    @Autowired
    private ITeModuleSetService moduleSetService;

    /**
     * 查询班级模块设置（无记录返回默认配置）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{classId}")
    public AjaxResult getInfo(@PathVariable Long classId)
    {
        return success(moduleSetService.selectByClassId(classId));
    }

    /**
     * 批量保存模块设置（10条整体提交）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "模块设置", businessType = BusinessType.UPDATE)
    @PutMapping("/{classId}")
    public AjaxResult save(@PathVariable Long classId, @RequestBody List<TeModuleSet> list)
    {
        return toAjax(moduleSetService.saveBatch(classId, list, getUsername()));
    }
}
