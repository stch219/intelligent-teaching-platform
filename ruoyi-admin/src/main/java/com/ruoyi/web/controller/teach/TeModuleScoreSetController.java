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
import com.ruoyi.teach.domain.TeModuleScoreSet;
import com.ruoyi.teach.service.ITeModuleScoreSetService;

/**
 * ============================================================================
 * 【功能】模块赋分控制器（教师端门户）
 * ----------------------------------------------------------------------------
 * 【说明】对计贡献率模块（5-10）设置满分值，总和必须恰好等于 100，
 *         否则保存失败；个人最终得分 = Σ(模块满分 × 个人贡献率)。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/scoreset")
public class TeModuleScoreSetController extends BaseController
{
    @Autowired
    private ITeModuleScoreSetService scoreSetService;

    /**
     * 查询班级赋分设置（固定返回模块5-10）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{classId}")
    public AjaxResult getInfo(@PathVariable Long classId)
    {
        return success(scoreSetService.selectByClassId(classId));
    }

    /**
     * 批量保存赋分（总分=100 强校验）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "模块赋分", businessType = BusinessType.UPDATE)
    @PutMapping("/{classId}")
    public AjaxResult save(@PathVariable Long classId, @RequestBody List<TeModuleScoreSet> list)
    {
        return toAjax(scoreSetService.saveBatch(classId, list));
    }
}
