package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeMessage;

/**
 * ============================================================================
 * 【功能】消息系统 Service 接口（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】提供会话列表、历史消息、已读回执、联系人、发起单聊、
 *         发送消息（供WebSocket服务端调用落库）等能力。
 * ============================================================================
 */
public interface ITeMessageService
{
    /**
     * 我的会话列表（消息中心左侧）
     *
     * @param userId 当前用户
     * @return 每行含 chatId/chatType/displayName/unreadCount/lastContent/lastTime
     */
    public List<Map<String, Object>> contacts(Long userId);

    /**
     * 历史消息（进入会话即顺带推进已读回执）
     *
     * @param userId 当前用户
     * @param chatId 会话ID
     * @param limit  最多返回条数（最新）
     * @return 含 messages（时间正序）与 chatId
     */
    public Map<String, Object> history(Long userId, Long chatId, int limit);

    /**
     * 标记会话已读（last_read_msg_id 推进到最新消息）
     */
    public void markRead(Long userId, Long chatId);

    /**
     * 我的未读消息总数（导航角标）
     */
    public int unreadTotal(Long userId);

    /**
     * 可联系人列表：学生=指导教师+本组成员；教师=本班学生（去重）
     *
     * @return 每行含 userId/nickName/type（1教师 2组员 3学生）/groupName/className
     */
    public List<Map<String, Object>> buddies(Long userId);

    /**
     * 发起/进入单聊：与目标用户间无单聊会话则懒创建，返回会话ID
     */
    public Long startSingle(Long userId, Long targetUserId);

    /**
     * 发送消息（落库+刷新会话时间；WebSocket推送由服务端完成）
     *
     * @return 落库后的完整消息（含消息ID与发送人姓名）
     */
    public TeMessage send(Long userId, Long chatId, String content);
}
