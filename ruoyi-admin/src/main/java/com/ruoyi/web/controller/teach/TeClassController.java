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
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.service.ITeClassService;

/**
 * ============================================================================
 * 【功能】班级管理控制器（教师端门户 + 管理员）
 * ----------------------------------------------------------------------------
 * 【说明】权限统一走 @ss.hasRole('teacher')（管理员自动放行）。
 *         教师只能操作自己的指导班级（非管理员强制 teacherId=当前用户）。
 *         核心能力：建班/编辑/删除、Excel名单导入（自动建号+自动分组）、
 *         分组看板、自动重新分组、手动调组。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/class")
public class TeClassController extends BaseController
{
    @Autowired
    private ITeClassService classService;

    /**
     * 查询班级列表（教师仅自己的班级；管理员全部）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/list")
    public AjaxResult list(TeClass teClass)
    {
        // 师生私有绑定：非管理员强制过滤为自己的指导班级
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            teClass.setTeacherId(getUserId());
        }
        return success(classService.selectTeClassList(teClass));
    }

    /**
     * 查询班级详情
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(classService.selectTeClassById(id));
    }

    /**
     * 新建班级（按分组数自动生成 A/B/C…小组）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "班级管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TeClass teClass)
    {
        // 教师建班时班级归属自己；管理员建班需指定指导教师
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            teClass.setTeacherId(getUserId());
        }
        return toAjax(classService.insertTeClass(teClass, getUsername()));
    }

    /**
     * 修改班级（名称/截止时间/分组数等）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "班级管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TeClass teClass)
    {
        return toAjax(classService.updateTeClass(teClass, getUsername()));
    }

    /**
     * 删除班级（有学生拒绝；级联清理关联配置数据）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "班级管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(classService.deleteTeClassByIds(ids, getUsername()));
    }

    /**
     * Excel 名单导入（列：学号/姓名/联系电话）
     * 自动创建登录账号（初始密码=学号后6位）+ 按学号末2位自动分组
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "班级名单导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importStudents/{classId}")
    public AjaxResult importStudents(@PathVariable Long classId, MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<TeStudent> util = new ExcelUtil<TeStudent>(TeStudent.class);
        List<TeStudent> studentList = util.importExcel(file.getInputStream());
        String message = classService.importStudents(classId, studentList, updateSupport, getUsername());
        return success(message);
    }

    /**
     * 下载名单导入模板（学号/姓名/联系电话）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<TeStudent> util = new ExcelUtil<TeStudent>(TeStudent.class);
        util.importTemplateExcel(response, "学生名单");
    }

    /**
     * 分组看板（小组列表 + 组内成员）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/{classId}/groupBoard")
    public AjaxResult groupBoard(@PathVariable Long classId)
    {
        return success(classService.selectGroupBoard(classId));
    }

    /**
     * 自动重新分组（按学号末2位对组数取模重新归组）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "自动分组", businessType = BusinessType.UPDATE)
    @PostMapping("/autoGroup/{classId}")
    public AjaxResult autoGroup(@PathVariable Long classId)
    {
        return success(classService.autoGrouping(classId));
    }

    /**
     * 手动调整单个学生的分组
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @Log(title = "手动调组", businessType = BusinessType.UPDATE)
    @PutMapping("/assignGroup")
    public AjaxResult assignGroup(@RequestBody TeStudent student)
    {
        return toAjax(classService.assignStudent(student.getUserId(), student.getGroupId()));
    }
}
