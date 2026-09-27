package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeAiReview;

/**
 * ============================================================================
 * 【功能】AI批改与成绩判分 Service 接口（阶段7）
 * ----------------------------------------------------------------------------
 * 【说明】教师端全流程：触发AI批改 → 查看批改结果 → 终审核定 →
 *         成绩汇总落库 → 发布成绩；学生端：查看本人已发布成绩。
 * ============================================================================
 */
public interface ITeReviewService
{
    /**
     * 教师端批改总览：班级下小组列表（含总稿状态/AI参考分/组最终分/
     * 任务信息/批改记录数/成绩发布状态），AI批改页一次加载
     *
     * @param classId 班级ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 总览行集合
     */
    public java.util.List<java.util.Map<String, Object>> reviewBoard(Long classId, Long operatorUserId);

    /**
     * 查询小组学生成绩列表（教师核对发布结果用）
     *
     * @param groupId 小组ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 成绩集合（联查姓名/学号）
     */
    public java.util.List<com.ruoyi.teach.domain.TeScore> listScores(Long groupId, Long operatorUserId);

    /**
     * 触发 AI 批改（教师）：组装模块5-10内容 → 调用 Python 推理服务 →
     * 服务不可用时自动切换本地规则模拟兜底 → 结果落库并回写小组AI参考分
     *
     * @param groupId 小组ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 批改来源说明（引擎名/规则模拟标记）
     */
    public String reviewGroup(Long groupId, Long operatorUserId);

    /**
     * 查询小组批改结果（含模块名/满分/批改过程，仅教师与组内学生可见）
     *
     * @param groupId 小组ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 批改结果列表
     */
    public List<TeAiReview> listReview(Long groupId, Long operatorUserId);

    /**
     * 教师终审：逐模块核定分数并标记核查完成（不改分则沿用AI参考分）
     *
     * @param groupId 小组ID
     * @param list 前端回传的模块核定列表（moduleCode + teacherScore）
     * @param checkedBy 核查教师用户名
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 影响条数
     */
    public int finalizeReview(Long groupId, List<TeAiReview> list, String checkedBy, Long operatorUserId);

    /**
     * 成绩汇总：小组最终分 = Σ模块核定分；个人最终得分 = Σ(模块核定分 × 个人贡献率)；
     * 写入 te_score（未发布状态，重新汇算自动撤回发布）
     *
     * @param groupId 小组ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 小组最终分
     */
    public java.math.BigDecimal computeScores(Long groupId, Long operatorUserId);

    /**
     * 发布本组成绩（发布后组内学生可见）
     *
     * @param groupId 小组ID
     * @param operatorUserId 操作教师用户ID（班级归属校验）
     * @return 影响行数
     */
    public int publishScores(Long groupId, Long operatorUserId);

    /**
     * 学生端"我的成绩"：仅已发布时透出分数（批改过程对学生不可见，仅见终分）
     *
     * @param userId 学生用户ID
     * @return 成绩视图（published / groupScore / finalScore / modules / contributions）
     */
    public Map<String, Object> myReview(Long userId);
}
