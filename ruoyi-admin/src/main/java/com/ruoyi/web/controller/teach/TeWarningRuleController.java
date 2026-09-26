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
import com.ruoyi.teach.domain.TeWarningRule;
import com.ruoyi.teach.service.ITeWarningRuleService;

/**
 * ============================================================================
 * 【功能】进度预警规则控制器（教师端门户）
 * ----------------------------------------------------------------------------
 * 【说明】每班最多 3 条规则（一般-黄/重要-橙/紧急-红三级），
 *         规则语义：距截止 X 天完成率低于 Y% 触发；阶段8定时任务执行触发。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/warningrule")
public class TeWarningRuleController extends BaseController
{
    @Autowired
    private ITeWarningRuleService warningRuleService;

    /**
     * 查询班级下预警规则列表
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/list/{classId}")
    public AjaxResult list(@PathVariable Long classId)
    {
        return success(warningRuleService.selectByClassId(classId));
    }

    /**
     * 新增规则（上限3条 + 查重）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "预警规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeWarningRule rule)
    {
        return toAjax(warningRuleService.insertTeWarningRule(rule));
    }

    /**
     * 修改规则（触发值/启停）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "预警规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeWarningRule rule)
    {
        return toAjax(warningRuleService.updateTeWarningRule(rule));
    }

    /**
     * 删除规则
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "预警规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(warningRuleService.deleteByIds(ids));
    }
}
