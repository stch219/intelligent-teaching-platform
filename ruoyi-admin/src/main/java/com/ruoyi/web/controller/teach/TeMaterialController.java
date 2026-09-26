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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.teach.domain.TeMaterial;
import com.ruoyi.teach.service.ITeMaterialService;

/**
 * ============================================================================
 * 【功能】学习资料控制器（教师端门户）
 * ----------------------------------------------------------------------------
 * 【说明】文件本体走若依 /common/upload 落盘，本控制器维护资料元数据；
 *         参考答案类资料严格保密（学生端永不可见）。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/material")
public class TeMaterialController extends BaseController
{
    @Autowired
    private ITeMaterialService materialService;

    /**
     * 查询资料列表（按班级过滤）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/list")
    public AjaxResult list(TeMaterial teMaterial)
    {
        return success(materialService.selectTeMaterialList(teMaterial));
    }

    /**
     * 新增资料记录
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "资料发布", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeMaterial teMaterial)
    {
        return toAjax(materialService.insertTeMaterial(teMaterial, getUsername()));
    }

    /**
     * 修改资料（名称/可见性）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "资料发布", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeMaterial teMaterial)
    {
        return toAjax(materialService.updateTeMaterial(teMaterial));
    }

    /**
     * 删除资料
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "资料发布", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(materialService.deleteTeMaterialByIds(ids));
    }
}
