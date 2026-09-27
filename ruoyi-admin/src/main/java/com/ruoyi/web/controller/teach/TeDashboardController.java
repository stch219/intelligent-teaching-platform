package com.ruoyi.web.controller.teach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.service.ITeDashboardService;

/**
 * ============================================================================
 * 【功能】三端仪表盘统计控制器（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】管理员=全平台规模/批改进度/最近预警；教师=本班教学总览；
 *         学生=个人任务进度/贡献率/消息/预警/成绩。角色鉴权分离。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/dashboard")
public class TeDashboardController extends BaseController
{
    @Autowired
    private ITeDashboardService dashboardService;

    /**
     * 管理员端全平台统计
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/admin")
    public AjaxResult admin()
    {
        return success(dashboardService.adminStats());
    }

    /**
     * 教师端本班教学总览（班级归属校验在 Service 内）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/{classId}")
    public AjaxResult teacher(@PathVariable Long classId)
    {
        return success(dashboardService.teacherStats(classId, getUserId()));
    }

    /**
     * 学生端个人统计
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student")
    public AjaxResult student()
    {
        return success(dashboardService.studentStats(getUserId()));
    }
}
