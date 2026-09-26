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
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.service.ITeStudentService;

/**
 * ============================================================================
 * 【功能】学生管理控制器（管理员端）
 * ----------------------------------------------------------------------------
 * 【说明】支持按"班级→小组"层级树筛选的学生列表、新增、修改、启用禁用、
 *         重置密码、删除（前端二次确认）、Excel 批量导入/导出。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/student")
public class TeStudentController extends BaseController
{
    @Autowired
    private ITeStudentService studentService;

    /**
     * 查询学生列表（支持班级/小组/姓名/学号/状态筛选）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeStudent student)
    {
        startPage();
        List<TeStudent> list = studentService.selectTeStudentList(student);
        return getDataTable(list);
    }

    /**
     * 查询班级→小组层级树（左侧导航）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:list')")
    @GetMapping("/classGroupTree")
    public AjaxResult classGroupTree()
    {
        return success(studentService.selectClassGroupTree());
    }

    /**
     * 查询学生详情
     */
    @PreAuthorize("@ss.hasPermi('teach:student:query')")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId)
    {
        return success(studentService.selectTeStudentByUserId(userId));
    }

    /**
     * 新增学生（学号即登录账号，初始密码 123456）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:add')")
    @Log(title = "学生管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeStudent student)
    {
        return toAjax(studentService.insertTeStudent(student, getUsername()));
    }

    /**
     * 修改学生信息
     */
    @PreAuthorize("@ss.hasPermi('teach:student:edit')")
    @Log(title = "学生管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeStudent student)
    {
        return toAjax(studentService.updateTeStudent(student, getUsername()));
    }

    /**
     * 修改学生账号状态（启用/禁用）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:edit')")
    @Log(title = "学生管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody TeStudent student)
    {
        return toAjax(studentService.changeStatus(student, getUsername()));
    }

    /**
     * 重置学生登录密码
     */
    @PreAuthorize("@ss.hasPermi('teach:student:resetPwd')")
    @Log(title = "学生管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody TeStudent student)
    {
        return toAjax(studentService.resetPwd(student.getUserId(), student.getPassword(), getUsername()));
    }

    /**
     * 删除学生（支持批量；前端做删除二次确认）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:remove')")
    @Log(title = "学生管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(studentService.deleteTeStudentByUserIds(userIds));
    }

    /**
     * 导出学生列表 Excel
     */
    @PreAuthorize("@ss.hasPermi('teach:student:export')")
    @Log(title = "学生管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TeStudent student)
    {
        List<TeStudent> list = studentService.selectTeStudentList(student);
        ExcelUtil<TeStudent> util = new ExcelUtil<TeStudent>(TeStudent.class);
        util.exportExcel(response, list, "学生数据");
    }

    /**
     * Excel 批量导入学生（列：学号/姓名/班级/联系电话）
     */
    @PreAuthorize("@ss.hasPermi('teach:student:import')")
    @Log(title = "学生管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<TeStudent> util = new ExcelUtil<TeStudent>(TeStudent.class);
        List<TeStudent> studentList = util.importExcel(file.getInputStream());
        String message = studentService.importStudent(studentList, updateSupport, getUsername());
        return success(message);
    }

    /**
     * 下载学生导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<TeStudent> util = new ExcelUtil<TeStudent>(TeStudent.class);
        util.importTemplateExcel(response, "学生数据");
    }
}
