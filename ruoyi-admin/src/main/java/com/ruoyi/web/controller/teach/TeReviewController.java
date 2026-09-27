package com.ruoyi.web.controller.teach;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.teach.domain.TeAiReview;
import com.ruoyi.teach.service.ITeReviewService;

/**
 * ============================================================================
 * 【功能】AI批改与成绩判分控制器（教师端门户，阶段7）
 * ----------------------------------------------------------------------------
 * 【说明】权限统一走 @ss.hasRole('teacher')（管理员自动放行）：
 *         - POST /{groupId}          触发AI批改（模型不可用自动规则兜底）
 *         - GET  /{groupId}          查看批改结果（AI分/评语/模型来源）
 *         - PUT  /{groupId}/finalize 教师终审核定各模块分数
 *         - POST /{groupId}/score    成绩汇总（组分=Σ模块生效分；个人分=Σ生效分×贡献率）
 *         - POST /{groupId}/publish  发布成绩（发布后学生可见）
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/review")
public class TeReviewController extends BaseController
{
    @Autowired
    private ITeReviewService reviewService;

    /**
     * 批改总览：班级下小组列表（总稿状态/AI参考分/组最终分/批改数/发布状态）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/board/{classId}")
    public AjaxResult board(@PathVariable Long classId)
    {
        return success(reviewService.reviewBoard(classId, getUserId()));
    }

    /**
     * 小组学生成绩列表（发布核对用）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{groupId}/scores")
    public AjaxResult scores(@PathVariable Long groupId)
    {
        return success(reviewService.listScores(groupId, getUserId()));
    }

    /**
     * 触发 AI 批改（耗时操作：GPU 每模块5-15秒 / CPU 10-30秒；失败自动规则兜底）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "AI批改", businessType = BusinessType.INSERT)
    @PostMapping("/{groupId}")
    public AjaxResult review(@PathVariable Long groupId)
    {
        String message = reviewService.reviewGroup(groupId, getUserId());
        return success(message);
    }

    /**
     * 查看小组批改结果（含批改过程评语，仅教师可见）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{groupId}")
    public AjaxResult list(@PathVariable Long groupId)
    {
        return success(reviewService.listReview(groupId, getUserId()));
    }

    /**
     * 教师终审：逐模块核定分数并标记核查完成
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "AI批改终审", businessType = BusinessType.UPDATE)
    @PutMapping("/{groupId}/finalize")
    public AjaxResult finalize(@PathVariable Long groupId, @RequestBody List<TeAiReview> list)
    {
        return toAjax(reviewService.finalizeReview(groupId, list, SecurityUtils.getUsername(), getUserId()));
    }

    /**
     * 成绩汇总（小组最终分 + 各成员个人得分，写入后未发布）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "成绩汇总", businessType = BusinessType.INSERT)
    @PostMapping("/{groupId}/score")
    public AjaxResult computeScore(@PathVariable Long groupId)
    {
        return success(reviewService.computeScores(groupId, getUserId()));
    }

    /**
     * 发布成绩（发布后组内学生可见，重新汇算会自动撤回发布）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "成绩发布", businessType = BusinessType.UPDATE)
    @PostMapping("/{groupId}/publish")
    public AjaxResult publish(@PathVariable Long groupId)
    {
        return toAjax(reviewService.publishScores(groupId, getUserId()));
    }
}
