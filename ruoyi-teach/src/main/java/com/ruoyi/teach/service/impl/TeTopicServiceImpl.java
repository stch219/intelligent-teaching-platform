package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeGroup;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.domain.TeTopic;
import com.ruoyi.teach.domain.TeTopicReply;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeGroupMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeTopicMapper;
import com.ruoyi.teach.mapper.TeTopicReplyMapper;
import com.ruoyi.teach.service.ITeTopicService;

/**
 * ============================================================================
 * 【功能】模块讨论话题 Service 实现（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】核心规则：
 *         - 学生：仅本组话题可见/可回复；发起话题须已分组、
 *           模块编号1-10、标题非空且≤200字
 *         - 教师：本班全部小组话题可见/可回复
 *         - 回复：内容非空≤2000字；回复后刷新话题最后回复时间（用于置顶排序）
 * ============================================================================
 */
@Service
public class TeTopicServiceImpl implements ITeTopicService
{
    @Autowired
    private TeTopicMapper topicMapper;

    @Autowired
    private TeTopicReplyMapper replyMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeGroupMapper groupMapper;

    /**
     * 话题列表：学生按小组过滤；教师合并本班各小组话题
     */
    @Override
    public List<TeTopic> list(Long userId, Long moduleCode)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student != null)
        {
            // 学生：本组话题（未分组则空列表）
            if (student.getGroupId() == null)
            {
                return new ArrayList<>();
            }
            TeTopic q = new TeTopic();
            q.setGroupId(student.getGroupId());
            q.setModuleCode(moduleCode);
            return topicMapper.selectTeTopicList(q);
        }
        // 教师：本班全部小组话题（多班合并）
        List<TeTopic> all = new ArrayList<>();
        TeClass cq = new TeClass();
        cq.setTeacherId(userId);
        for (TeClass teClass : classMapper.selectTeClassList(cq))
        {
            TeTopic q = new TeTopic();
            q.setClassId(teClass.getId());
            q.setModuleCode(moduleCode);
            all.addAll(topicMapper.selectTeTopicList(q));
        }
        return all;
    }

    /**
     * 学生发起话题：已分组 + 模块编号1-10 + 标题非空
     */
    @Override
    public void create(Long userId, Long moduleCode, String title)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student == null)
        {
            throw new ServiceException("仅学生可发起讨论话题");
        }
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，暂不能发起话题，请联系教师");
        }
        if (moduleCode == null || moduleCode < 1 || moduleCode > 10)
        {
            throw new ServiceException("请选择关联模块（编号1-10）");
        }
        if (StringUtils.isEmpty(title) || title.trim().isEmpty())
        {
            throw new ServiceException("话题标题不能为空");
        }
        if (title.trim().length() > 200)
        {
            throw new ServiceException("话题标题不能超过200字");
        }
        TeTopic topic = new TeTopic();
        topic.setGroupId(student.getGroupId());
        topic.setModuleCode(moduleCode);
        topic.setTitle(title.trim());
        topic.setCreatorId(userId);
        topic.setCreateTime(new Date());
        topicMapper.insertTeTopic(topic);
    }

    /**
     * 回复列表：先做话题可见权限校验（学生限本组，教师限本班）
     */
    @Override
    public List<TeTopicReply> replies(Long userId, Long topicId)
    {
        TeTopic topic = getTopicChecked(userId, topicId);
        return replyMapper.selectByTopicId(topic.getId());
    }

    /**
     * 回复话题：权限校验 + 内容校验 + 落库 + 刷新话题时间
     */
    @Override
    @Transactional
    public void reply(Long userId, Long topicId, Long replyId, String content)
    {
        TeTopic topic = getTopicChecked(userId, topicId);
        if (StringUtils.isEmpty(content) || content.trim().isEmpty())
        {
            throw new ServiceException("回复内容不能为空");
        }
        if (content.trim().length() > 2000)
        {
            throw new ServiceException("回复内容不能超过2000字");
        }
        TeTopicReply reply = new TeTopicReply();
        reply.setTopicId(topic.getId());
        reply.setReplyId(replyId); // 为空即首层回复
        reply.setUserId(userId);
        reply.setContent(content.trim());
        reply.setReplyTime(new Date());
        replyMapper.insertTeTopicReply(reply);
        topicMapper.updateTopicTime(topic.getId());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 话题可见性校验：学生=话题属于本组；教师=话题小组属于本班；其余拒绝
     */
    private TeTopic getTopicChecked(Long userId, Long topicId)
    {
        TeTopic topic = topicMapper.selectTeTopicById(topicId);
        if (topic == null)
        {
            throw new ServiceException("话题不存在或已被删除");
        }
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student != null)
        {
            if (!topic.getGroupId().equals(student.getGroupId()))
            {
                throw new ServiceException("只能访问本组的话题");
            }
            return topic;
        }
        // 教师：话题小组必须属于我指导的班级（经小组反查所属班级）
        TeGroup group = groupMapper.selectById(topic.getGroupId());
        if (group == null || group.getClassId() == null)
        {
            throw new ServiceException("话题所属小组不存在");
        }
        TeClass cq = new TeClass();
        cq.setTeacherId(userId);
        for (TeClass teClass : classMapper.selectTeClassList(cq))
        {
            if (teClass.getId().equals(group.getClassId()))
            {
                return topic;
            }
        }
        throw new ServiceException("只能访问本班小组的话题");
    }
}
