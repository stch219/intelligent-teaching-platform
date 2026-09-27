package com.ruoyi.web.controller.teach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.service.ITeMessageService;

/**
 * ============================================================================
 * 【功能】消息中心 REST 控制器（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】会话列表/历史消息/已读回执/未读角标/可联系人/发起单聊。
 *         实时收发走 WebSocket 端点（/websocket/message），
 *         本控制器只负责"进页面拉数据"与"浏览即已读"两类动作。
 *         学生与教师共用，故权限用组合角色表达式。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/msg")
public class TeMessageController extends BaseController
{
    @Autowired
    private ITeMessageService messageService;

    /**
     * 我的会话列表（含展示名/未读数/最后一条消息）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/contacts")
    public AjaxResult contacts()
    {
        return success(messageService.contacts(getUserId()));
    }

    /**
     * 会话历史消息（时间正序；进入会话即顺带推进已读回执）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/history/{chatId}")
    public AjaxResult history(@PathVariable Long chatId)
    {
        // 上限200条：课程项目体量足够，避免一次拉全表
        return success(messageService.history(getUserId(), chatId, 200));
    }

    /**
     * 手动标记会话已读（last_read_msg_id 推进到最新）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @PostMapping("/read/{chatId}")
    public AjaxResult markRead(@PathVariable Long chatId)
    {
        messageService.markRead(getUserId(), chatId);
        return success();
    }

    /**
     * 我的未读消息总数（导航铃铛角标）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/unreadTotal")
    public AjaxResult unreadTotal()
    {
        return success(messageService.unreadTotal(getUserId()));
    }

    /**
     * 可联系人列表：学生=指导教师+本组成员；教师=本班学生（去重）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/buddies")
    public AjaxResult buddies()
    {
        return success(messageService.buddies(getUserId()));
    }

    /**
     * 发起/进入单聊：无会话则懒创建，返回会话ID（前端凭此打开聊天窗）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @PostMapping("/start/{targetUserId}")
    public AjaxResult startSingle(@PathVariable Long targetUserId)
    {
        return success(messageService.startSingle(getUserId(), targetUserId));
    }
}
