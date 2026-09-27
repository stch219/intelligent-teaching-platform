package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeTopic;

/**
 * ============================================================================
 * 【功能】模块讨论话题 Mapper 接口（表 te_topic）— 阶段6消息系统
 * ============================================================================
 */
public interface TeTopicMapper
{
    /** 查询话题列表（学生按小组过滤；教师传 classId 查本班全部小组） */
    public List<TeTopic> selectTeTopicList(TeTopic teTopic);

    /** 按ID查询话题 */
    public TeTopic selectTeTopicById(Long id);

    /** 新增话题 */
    public int insertTeTopic(TeTopic teTopic);

    /** 刷新话题最后回复时间 */
    public int updateTopicTime(Long id);
}
