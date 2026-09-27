package com.ruoyi.teach.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * ============================================================================
 * 【功能】阶段8 聚合统计 Mapper（三端仪表盘 + 三级预警扫描引擎共用）
 * ----------------------------------------------------------------------------
 * 【说明】不建新表，全部为跨表聚合查询：
 *         管理员端 → 全平台规模计数与批改进度；
 *         教师端 → 本班各组完成率/成绩分布/预警分布；
 *         学生端 → 个人模块完成度/贡献率确认/成绩；
 *         扫描引擎 → 各组完成率+任务截止时间、组内成员列表（推送用）。
 * ============================================================================
 */
public interface TeDashboardMapper
{
    /** ==================== 管理员端全平台统计 ==================== */

    /** 班级总数 */
    int countClass();

    /** 学生总数 */
    int countStudent();

    /** 任务总数 */
    int countTask();

    /** 小组总数 */
    int countGroup();

    /** 已触发过 AI 批改的小组数（ai_ref_score 非空） */
    int countAiReviewedGroup();

    /** 已发布成绩的学生数 */
    int countPublishedScore();

    /** 最近 5 条未处置预警（管理员首页展示，跨班级） */
    List<Map<String, Object>> selectRecentWarnings();

    /** ==================== 教师端本班统计 ==================== */

    /**
     * 本班各组进度总览（扫描引擎与教师仪表盘共用）：
     * 返回 [{groupId, groupName, submitStatus, aiRefScore, submitted, deadline, taskName}]
     * submitted = 该组已提交（status=1）的模块数（0-10）
     */
    List<Map<String, Object>> selectGroupProgressByClass(Long classId);

    /**
     * 本班成绩分布（已发布成绩按分数段统计）：
     * 返回 [{range: '90-100', cnt: n}, ...] 固定四段
     */
    List<Map<String, Object>> selectScoreDistribution(Long classId);

    /** 本班已提交总稿的小组数 */
    int countSubmittedGroup(Long classId);

    /** ==================== 学生端个人统计 ==================== */

    /** 本组已提交模块数（status=1 计入完成） */
    int countSubmittedModule(Long groupId);

    /** 本人贡献率分配确认状态（te_confirm_record confirm_type=2 存在即已确认） */
    int countMyContributionConfirm(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /** 本人已发布的最终成绩（未发布返回 null） */
    Double selectMyFinalScore(Long userId);

    /** ==================== 预警扫描引擎支撑 ==================== */

    /** 组内全部学生用户ID（WebSocket 推送目标） */
    List<Long> selectStudentUserIdsByGroup(Long groupId);
}
