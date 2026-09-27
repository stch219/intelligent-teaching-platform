package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.teach.domain.TeAnnouncement;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeAnnouncementMapper;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.service.ITeAnnouncementService;

/**
 * ============================================================================
 * 【功能】班级公告 Service 实现（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】核心规则：
 *         - 学生：仅本班公告可见（未分班则空列表）
 *         - 教师：仅可向自己指导的班级发布公告（班级归属强校验），
 *           标题≤200字、内容非空≤5000字
 *         - 发布后返回落库公告对象；在线学生提醒推送由表现层完成
 * ============================================================================
 */
@Service
public class TeAnnouncementServiceImpl implements ITeAnnouncementService
{
    @Autowired
    private TeAnnouncementMapper announcementMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private ISysUserService userService;

    /**
     * 公告列表：学生=本班公告；教师=我指导的全部班级公告
     */
    @Override
    public List<TeAnnouncement> list(Long userId)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        TeAnnouncement q = new TeAnnouncement();
        if (student != null)
        {
            // 学生：本班公告（未分班则空）
            if (student.getClassId() == null)
            {
                return new ArrayList<>();
            }
            q.setClassId(student.getClassId());
            return announcementMapper.selectTeAnnouncementList(q);
        }
        // 教师：多班公告合并
        List<TeAnnouncement> all = new ArrayList<>();
        TeClass cq = new TeClass();
        cq.setTeacherId(userId);
        for (TeClass teClass : classMapper.selectTeClassList(cq))
        {
            q = new TeAnnouncement();
            q.setClassId(teClass.getId());
            all.addAll(announcementMapper.selectTeAnnouncementList(q));
        }
        return all;
    }

    /**
     * 教师发布公告：班级归属校验 + 内容校验 + 落库
     */
    @Override
    public TeAnnouncement publish(Long userId, String title, String content, Long classId)
    {
        // 班级必须存在且归属当前教师
        TeClass teClass = classMapper.selectTeClassById(classId);
        if (teClass == null || teClass.getTeacherId() == null || !teClass.getTeacherId().equals(userId))
        {
            throw new ServiceException("只能向自己指导的班级发布公告");
        }
        if (StringUtils.isEmpty(title) || title.trim().isEmpty())
        {
            throw new ServiceException("公告标题不能为空");
        }
        if (title.trim().length() > 200)
        {
            throw new ServiceException("公告标题不能超过200字");
        }
        if (StringUtils.isEmpty(content) || content.trim().isEmpty())
        {
            throw new ServiceException("公告内容不能为空");
        }
        if (content.trim().length() > 5000)
        {
            throw new ServiceException("公告内容不能超过5000字");
        }
        TeAnnouncement ann = new TeAnnouncement();
        ann.setTitle(title.trim());
        ann.setContent(content.trim());
        ann.setClassId(classId);
        ann.setPublishBy(userService.selectUserById(userId).getNickName());
        ann.setPublishTime(new Date());
        announcementMapper.insertTeAnnouncement(ann);
        return ann;
    }
}
