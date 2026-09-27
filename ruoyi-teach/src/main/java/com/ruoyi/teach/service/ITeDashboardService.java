package com.ruoyi.teach.service;

import java.util.Map;

/**
 * ============================================================================
 * 【功能】三端仪表盘统计服务接口（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】管理员=全平台规模与批改进度；教师=本班各组完成率/成绩分布/预警分布；
 *         学生=个人模块完成度/贡献率确认/未读消息/预警/成绩。
 * ============================================================================
 */
public interface ITeDashboardService
{
    /**
     * 管理员端全平台统计
     */
    Map<String, Object> adminStats();

    /**
     * 教师端本班教学总览（班级归属校验，admin 放行）
     */
    Map<String, Object> teacherStats(Long classId, Long operator);

    /**
     * 学生端个人统计（按当前登录学生）
     */
    Map<String, Object> studentStats(Long userId);
}
