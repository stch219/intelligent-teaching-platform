package com.ruoyi.web.controller.teach;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.service.ITeTopicService;

/**
 * ============================================================================
 * 【功能】模块讨论话题控制器（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】学生围绕模块发起小组话题并回复；教师可查看本班话题并回复。
 *         数据可见范围（本组/本班）由 Service 层按登录身份过滤。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/topic")
public class TeTopicController extends BaseController
{
    @Autowired
    private ITeTopicService topicService;

    /**
     * 话题列表（可选按模块编号过滤；学生=本组话题，教师=本班话题）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long moduleCode)
    {
        return success(topicService.list(getUserId(), moduleCode));
    }

    /**
     * 学生发起话题（须已分组；模块编号1-10；标题非空且≤200字）
     */
    @PreAuthorize("@ss.hasRole('student')")
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body)
    {
        Long moduleCode = body.get("moduleCode") == null ? null : Long.valueOf(body.get("moduleCode").toString());
        String title = body.get("title") == null ? "" : body.get("title").toString();
        topicService.create(getUserId(), moduleCode, title);
        return success();
    }

    /**
     * 话题回复列表（时间正序，含回复人姓名）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/{topicId}/replies")
    public AjaxResult replies(@PathVariable Long topicId)
    {
        return success(topicService.replies(getUserId(), topicId));
    }

    /**
     * 回复话题（replyId 为空=首层回复；权限校验在 Service 内完成）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @PostMapping("/{topicId}/reply")
    public AjaxResult reply(@PathVariable Long topicId, @RequestBody Map<String, Object> body)
    {
        Long replyId = body.get("replyId") == null ? null : Long.valueOf(body.get("replyId").toString());
        String content = body.get("content") == null ? "" : body.get("content").toString();
        topicService.reply(getUserId(), topicId, replyId, content);
        return success();
    }
}
