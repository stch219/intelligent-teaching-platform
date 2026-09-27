package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeAnnouncement;

/**
 * ============================================================================
 * 【功能】班级公告 Mapper 接口（表 te_announcement）— 阶段6消息系统
 * ============================================================================
 */
public interface TeAnnouncementMapper
{
    /** 查询公告列表（学生按班级；教师按指导班级集合过滤） */
    public List<TeAnnouncement> selectTeAnnouncementList(TeAnnouncement announcement);

    /** 按ID查询公告 */
    public TeAnnouncement selectTeAnnouncementById(Long id);

    /** 新增公告 */
    public int insertTeAnnouncement(TeAnnouncement announcement);
}
