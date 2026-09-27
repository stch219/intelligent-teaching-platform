package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeAnnouncement;

/**
 * ============================================================================
 * 【功能】班级公告 Service 接口（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】教师面向自己指导的班级发布公告；学生查看本班公告。
 *         发布成功后向该班在线学生推送 WebSocket 提醒。
 * ============================================================================
 */
public interface ITeAnnouncementService
{
    /**
     * 公告列表：学生=本班公告；教师=我指导的全部班级公告
     */
    public List<TeAnnouncement> list(Long userId);

    /**
     * 教师发布公告（校验班级归属；落库后返回公告对象，
     * WebSocket在线提醒推送由表现层完成，保持业务层不依赖Web设施）
     */
    public TeAnnouncement publish(Long userId, String title, String content, Long classId);
}
