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
import com.ruoyi.teach.domain.TeTask;
import com.ruoyi.teach.service.ITeTaskService;

/**
 * ============================================================================
 * 【功能】任务管理控制器（教师端门户）
 * ----------------------------------------------------------------------------
 * 【说明】课程设计任务 A~H 编号自动生成；任务内容含设计目标/具体要求/
 *         评分标准/重要提示四要素；分配规则：教师分配到组（一组一任务）。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/task")
public class TeTaskController extends BaseController
{
    @Autowired
    private ITeTaskService taskService;

    /**
     * 查询任务列表（按班级过滤）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/list")
    public AjaxResult list(TeTask teTask)
    {
        return success(taskService.selectTeTaskList(teTask));
    }

    /**
     * 查询任务详情
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(taskService.selectTeTaskById(id));
    }

    /**
     * 新增任务（编号自动生成）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeTask teTask)
    {
        teTask.setTeacherId(getUserId());
        return toAjax(taskService.insertTeTask(teTask, getUsername()));
    }

    /**
     * 修改任务内容
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeTask teTask)
    {
        return toAjax(taskService.updateTeTask(teTask, getUsername()));
    }

    /**
     * 删除任务（已被分配则拒绝）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(taskService.deleteTeTaskByIds(ids));
    }

    /**
     * 分配任务到组（整体覆盖，一组只领一个任务）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "任务分配", businessType = BusinessType.UPDATE)
    @PutMapping("/assign")
    public AjaxResult assign(@RequestBody TeTask teTask)
    {
        return success(taskService.assignGroups(teTask.getId(), teTask.getGroupIds()));
    }

    /**
     * 查询班级下分配视图（组→任务）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/assignList/{classId}")
    public AjaxResult assignList(@PathVariable Long classId)
    {
        return success(taskService.selectAssignList(classId));
    }
}
