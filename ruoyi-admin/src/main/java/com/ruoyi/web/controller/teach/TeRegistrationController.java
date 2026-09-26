package com.ruoyi.web.controller.teach;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.teach.domain.TeRegistration;
import com.ruoyi.teach.service.ITeRegistrationService;

/**
 * ============================================================================
 * 【功能】学生注册审批管理控制器（管理员端）
 * ----------------------------------------------------------------------------
 * 【说明】提供注册申请的列表查询、详情、审批通过（自动创建学生账号）、
 *         审批驳回（记录原因）、删除等接口；所有操作需管理员权限。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/registration")
public class TeRegistrationController extends BaseController
{
    @Autowired
    private ITeRegistrationService registrationService;

    /**
     * 查询注册申请列表（支持按姓名/学号/班级/状态筛选）
     */
    @PreAuthorize("@ss.hasPermi('teach:registration:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeRegistration registration)
    {
        startPage();
        List<TeRegistration> list = registrationService.selectTeRegistrationList(registration);
        return getDataTable(list);
    }

    /**
     * 查询注册申请详情
     */
    @PreAuthorize("@ss.hasPermi('teach:registration:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(registrationService.selectTeRegistrationById(id));
    }

    /**
     * 审批通过（事务内自动创建学生登录账号与学生扩展记录）
     */
    @PreAuthorize("@ss.hasPermi('teach:registration:approve')")
    @Log(title = "注册审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public AjaxResult approve(@PathVariable Long id)
    {
        return toAjax(registrationService.approve(id, getUsername()));
    }

    /**
     * 审批驳回（必填驳回原因，学生可据此修改后重新申请）
     */
    @PreAuthorize("@ss.hasPermi('teach:registration:approve')")
    @Log(title = "注册审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject")
    public AjaxResult reject(@RequestBody TeRegistration registration)
    {
        return toAjax(registrationService.reject(registration.getId(),
                registration.getRejectReason(), getUsername()));
    }

    /**
     * 删除注册申请（支持批量）
     */
    @PreAuthorize("@ss.hasPermi('teach:registration:remove')")
    @Log(title = "注册审批", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(registrationService.deleteTeRegistrationByIds(ids));
    }
}
