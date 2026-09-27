package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeMessage;

/**
 * ============================================================================
 * 【功能】聊天消息 Mapper 接口（表 te_message）— 阶段6消息系统
 * ============================================================================
 */
public interface TeMessageMapper
{
    /** 新增消息（useGeneratedKeys 回填自增ID） */
    public int insertTeMessage(TeMessage message);

    /** 按会话查询历史消息（最新 limit 条，ID倒序，由服务层反转为正序） */
    public List<TeMessage> selectByChatId(Long chatId, int limit);

    /** 查询会话最后一条消息（会话列表预览） */
    public TeMessage selectLastByChatId(Long chatId);
}
