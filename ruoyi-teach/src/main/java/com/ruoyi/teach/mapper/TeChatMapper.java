package com.ruoyi.teach.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeChat;

/**
 * ============================================================================
 * 【功能】聊天会话 Mapper 接口（表 te_chat）— 阶段6消息系统
 * ============================================================================
 */
public interface TeChatMapper
{
    /** 按ID查询会话 */
    public TeChat selectTeChatById(Long id);

    /** 查询两用户之间的单聊会话（懒创建前先查重） */
    public TeChat selectSingleChat(Long userA, Long userB);

    /** 查询某小组的群聊会话（懒创建前先查重） */
    public TeChat selectGroupChat(Long groupId);

    /** 新增会话（返回自增ID） */
    public int insertTeChat(TeChat teChat);

    /** 我的会话列表（含未读数/最后一条消息/展示名，供消息中心左侧列表） */
    public List<Map<String, Object>> selectMyChatList(Long userId);

    /** 更新会话最后消息时间 */
    public int updateChatTime(Long id);
}
