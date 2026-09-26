package com.ruoyi.web.controller.teach;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.teach.domain.TeTeacher;
import com.ruoyi.teach.service.ITeTeacherService;

/**
 * ============================================================================
 * 【功能】教师管理控制器（管理员端）
 * ----------------------------------------------------------------------------
 * 【说明】教师账号全平台上限 4 名：新增时后端强校验；
 *         支持修改（姓名/职称/联系方式）、启用禁用、重置密码、删除
 *         （后端校验"仅可删除无指导班级的教师"，前端另做二次确认弹窗）。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/teacher")
public class TeTeacherController extends BaseController
{
    @Autowired
    private ITeTeacherService teacherService;

    /**
     * 查询教师列表（含账号/职称/状态/指导班级数）
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeTeacher teacher)
    {
        startPage();
        List<TeTeacher> list = teacherService.selectTeTeacherList(teacher);
        return getDataTable(list);
    }

    /**
     * 查询教师详情
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:query')")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId)
    {
        return success(teacherService.selectTeTeacherByUserId(userId));
    }

    /**
     * 新增教师（后端校验上限 4 名，超限直接拒绝）
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:add')")
    @Log(title = "教师管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeTeacher teacher)
    {
        return toAjax(teacherService.insertTeTeacher(teacher, getUsername()));
    }

    /**
     * 修改教师信息
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:edit')")
    @Log(title = "教师管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeTeacher teacher)
    {
        return toAjax(teacherService.updateTeTeacher(teacher, getUsername()));
    }

    /**
     * 修改教师账号状态（启用/禁用）
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:edit')")
    @Log(title = "教师管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody TeTeacher teacher)
    {
        return toAjax(teacherService.changeStatus(teacher, getUsername()));
    }

    /**
     * 重置教师登录密码
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:resetPwd')")
    @Log(title = "教师管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody TeTeacher teacher)
    {
        return toAjax(teacherService.resetPwd(teacher.getUserId(), teacher.getPassword(), getUsername()));
    }

    /**
     * 删除教师（支持批量；名下有指导班级将被后端拒绝）
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:remove')")
    @Log(title = "教师管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(teacherService.deleteTeTeacherByUserIds(userIds));
    }

    /**
     * 导出教师列表 Excel
     */
    @PreAuthorize("@ss.hasPermi('teach:teacher:export')")
    @Log(title = "教师管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TeTeacher teacher)
    {
        List<TeTeacher> list = teacherService.selectTeTeacherList(teacher);
        ExcelUtil<TeTeacher> util = new ExcelUtil<TeTeacher>(TeTeacher.class);
        util.exportExcel(response, list, "教师数据");
    }
}
