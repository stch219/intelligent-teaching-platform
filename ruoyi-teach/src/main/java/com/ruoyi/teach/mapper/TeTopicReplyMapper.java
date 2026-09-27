package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeTopicReply;

/**
 * ============================================================================
 * 【功能】话题回复 Mapper 接口（表 te_topic_reply）— 阶段6消息系统
 * ============================================================================
 */
public interface TeTopicReplyMapper
{
    /** 查询话题下全部回复（时间正序，含回复人姓名与被回复人姓名） */
    public List<TeTopicReply> selectByTopicId(Long topicId);

    /** 新增回复 */
    public int insertTeTopicReply(TeTopicReply reply);
}
