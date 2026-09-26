package com.ruoyi.web.controller.teach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.domain.TeRegistration;
import com.ruoyi.teach.service.ITeRegistrationService;

/**
 * ============================================================================
 * 【功能】学生注册入口控制器（匿名接口，无需登录）
 * ----------------------------------------------------------------------------
 * 【说明】登录页"注册"入口提交的申请统一走本控制器；
 *         使用 @Anonymous 注解放行（由 PermitAllUrlProperties 自动收集），
 *         审批管理操作在 TeRegistrationController（需管理员权限）。
 * ============================================================================
 */
@Anonymous
@RestController
@RequestMapping("/teach/register")
public class TeRegisterController extends BaseController
{
    @Autowired
    private ITeRegistrationService registrationService;

    /**
     * 学生提交注册申请（姓名/学号/班级/联系电话）
     */
    @PostMapping
    public AjaxResult register(@RequestBody TeRegistration registration)
    {
        registrationService.submitRegistration(registration);
        return success("申请已提交，请等待管理员审批");
    }
}
