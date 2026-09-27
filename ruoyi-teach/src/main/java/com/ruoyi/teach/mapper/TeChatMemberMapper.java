package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeChatMember;

/**
 * ============================================================================
 * 【功能】会话成员 Mapper 接口（表 te_chat_member）— 阶段6消息系统
 * ----------------------------------------------------------------------------
 * 【说明】last_read_msg_id 为已读/未读回执核心字段。
 * ============================================================================
 */
public interface TeChatMemberMapper
{
    /** 查询会话内指定成员（成员权限校验用） */
    public TeChatMember selectByChatAndUser(Long chatId, Long userId);

    /** 查询会话全部成员（消息推送目标） */
    public List<TeChatMember> selectByChatId(Long chatId);

    /** 新增单个成员 */
    public int insertTeChatMember(TeChatMember member);

    /** 批量新增成员（群聊建会话时一次写入组员+教师） */
    public int insertMembers(List<TeChatMember> members);

    /** 推进已读回执：将我的 last_read_msg_id 更新为会话最新消息ID */
    public int updateLastReadToLatest(Long chatId, Long userId);

    /** 统计我的全部会话未读消息总数（导航角标） */
    public int countMyUnread(Long userId);
}
