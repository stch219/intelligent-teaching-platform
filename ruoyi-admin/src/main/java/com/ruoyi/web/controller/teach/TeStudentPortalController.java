package com.ruoyi.web.controller.teach;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.teach.domain.TeContribution;
import com.ruoyi.teach.domain.TeModuleContent;
import com.ruoyi.teach.service.ITeStudentPortalService;

/**
 * ============================================================================
 * 【功能】学生端门户控制器（阶段4）
 * ----------------------------------------------------------------------------
 * 【说明】权限统一走 @ss.hasRole('student')（管理员自动放行）。
 *         接口按"学生只能看/改自己小组的数据"原则实现：
 *         - 我的信息/角色选定：仅操作当前登录学生本人
 *         - 公示板/模块看板：服务端按当前学生的班级、小组过滤
 *         - 模块编辑：暂存/提交（字数条目校验+乐观锁防覆盖）
 *         - 贡献率：组长分配（每模块合计100%）+ 组员确认
 *         - 分工确认：组长确认组员填报的分工
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/student/portal")
public class TeStudentPortalController extends BaseController
{
    @Autowired
    private ITeStudentPortalService portalService;

    /** AI批改与成绩服务（阶段7：学生端"我的成绩"） */
    @Autowired
    private com.ruoyi.teach.service.ITeReviewService reviewService;

    /**
     * 我的信息（学籍/班级/小组/角色/分工/指导教师/截止时间）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/myInfo")
    public AjaxResult myInfo()
    {
        return success(portalService.myInfo(getUserId()));
    }

    /**
     * 选定组内角色并填报分工（组长唯一校验；分工变化重置确认状态）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-角色分工", businessType = BusinessType.UPDATE)
    @PutMapping("/myRole")
    public AjaxResult updateMyRole(@RequestBody Map<String, Object> body)
    {
        Long roleType = body.get("roleType") == null ? null : Long.valueOf(body.get("roleType").toString());
        String duty = body.get("dutyAssignment") == null ? "" : body.get("dutyAssignment").toString();
        portalService.updateMyRole(getUserId(), roleType, duty);
        return success();
    }

    /**
     * 公示板（本组任务 + 组员分工一览 + 可见资料）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/board")
    public AjaxResult board()
    {
        return success(portalService.board(getUserId()));
    }

    /**
     * 模块看板（10模块设置+内容状态/进度+赋分）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/modules")
    public AjaxResult modules()
    {
        return success(portalService.moduleBoard(getUserId()));
    }

    /**
     * 模块内容详情（任务要求模块自动合成）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/module/{moduleCode}")
    public AjaxResult moduleDetail(@PathVariable Long moduleCode)
    {
        return success(portalService.moduleDetail(getUserId(), moduleCode));
    }

    /**
     * 暂存模块内容（协同编辑：乐观锁+上限校验+进度折算）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-模块暂存", businessType = BusinessType.UPDATE)
    @PutMapping("/module/save")
    public AjaxResult saveModule(@RequestBody TeModuleContent form)
    {
        portalService.saveContent(getUserId(), form);
        return success();
    }

    /**
     * 提交模块内容（下限校验，提交后锁定）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-模块提交", businessType = BusinessType.UPDATE)
    @PutMapping("/module/submit")
    public AjaxResult submitModule(@RequestBody TeModuleContent form)
    {
        portalService.submitContent(getUserId(), form);
        return success();
    }

    /**
     * 贡献率面板（组员+分配行+赋分+是否组长）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/contribution")
    public AjaxResult contributionPanel()
    {
        return success(portalService.contributionPanel(getUserId()));
    }

    /**
     * 组长保存贡献率分配（模块5-10每模块合计必须100%）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-贡献率分配", businessType = BusinessType.UPDATE)
    @PutMapping("/contribution")
    public AjaxResult saveContribution(@RequestBody List<TeContribution> list)
    {
        portalService.saveContribution(getUserId(), list);
        return success();
    }

    /**
     * 组员确认贡献率（一键确认本人全部行）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-贡献率确认", businessType = BusinessType.UPDATE)
    @PutMapping("/contribution/confirm")
    public AjaxResult confirmContribution()
    {
        portalService.confirmContribution(getUserId());
        return success();
    }

    /**
     * 组长确认组员分工
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-分工确认", businessType = BusinessType.UPDATE)
    @PostMapping("/duty/confirm/{memberUserId}")
    public AjaxResult confirmDuty(@PathVariable Long memberUserId)
    {
        portalService.confirmDuty(getUserId(), memberUserId);
        return success();
    }

    /**
     * 总稿合规预检（阶段5）：4项检查结果（组长身份/模块完成/分工确认/贡献率）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/submitPrecheck")
    public AjaxResult submitPrecheck()
    {
        return success(portalService.submitPrecheck(getUserId()));
    }

    /**
     * 提交总稿（阶段5）：预检全过后封面落库锁定 + 小组置已提交
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-总稿提交", businessType = BusinessType.UPDATE)
    @PostMapping("/submitFinal")
    public AjaxResult submitFinal()
    {
        portalService.submitFinal(getUserId());
        return success("总稿提交成功");
    }

    /**
     * 导出总稿PDF（阶段5）：封面+目录+10模块正文，仅已提交总稿可下载。
     * Service 生成PDF字节流，本方法负责文件名与响应写出
     */
    @PreAuthorize("@ss.hasRole('student')")
    @Log(title = "学生门户-总稿PDF导出", businessType = BusinessType.EXPORT)
    @GetMapping("/exportPdf")
    public void exportPdf(jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        byte[] pdf = portalService.exportFinalPdf(getUserId());
        // 文件名含组名（从我的信息聚合中获取），URL编码防中文乱码
        Object groupName = portalService.myInfo(getUserId()).get("groupName");
        String fileName = java.net.URLEncoder.encode(
                (groupName == null ? "小组" : groupName.toString()) + "-课程设计总稿.pdf", "UTF-8");
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }

    /**
     * 我的成绩（阶段7）：教师发布后可见组最终分/个人得分/各模块终分/本人贡献率；
     * 批改过程对 学生不可见（仅见终分），未发布时仅返回 published=false
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/myReview")
    public AjaxResult myReview()
    {
        return success(reviewService.myReview(getUserId()));
    }
}
