package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeTopic;
import com.ruoyi.teach.domain.TeTopicReply;

/**
 * ============================================================================
 * 【功能】模块讨论话题 Service 接口（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】学生围绕模块发起小组话题并回复；教师可查看本班全部话题并回复。
 * ============================================================================
 */
public interface ITeTopicService
{
    /**
     * 话题列表：学生=本组话题；教师=本班全部小组话题
     */
    public List<TeTopic> list(Long userId, Long moduleCode);

    /**
     * 学生发起话题（须已分组；模块编号1-10；标题非空）
     */
    public void create(Long userId, Long moduleCode, String title);

    /**
     * 话题回复列表（时间正序）
     */
    public List<TeTopicReply> replies(Long userId, Long topicId);

    /**
     * 回复话题（replyId 为空=首层回复；权限：学生限本组话题，教师限本班话题）
     */
    public void reply(Long userId, Long topicId, Long replyId, String content);
}
