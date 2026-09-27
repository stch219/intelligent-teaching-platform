package com.ruoyi.teach.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeScore;

/**
 * ============================================================================
 * 【功能】成绩 Mapper 接口（操作表 te_score，唯一键 user_id）
 * ----------------------------------------------------------------------------
 * 【说明】成绩汇总采用 upsert（按用户唯一键覆盖更新）；
 *         发布开关按小组整体翻转（同组学生一起发布）。
 * ============================================================================
 */
public interface TeScoreMapper
{
    /**
     * 保存/更新单条成绩（唯一键 user_id 冲突时覆盖更新）
     *
     * @param score 成绩实体
     * @return 影响行数
     */
    public int upsert(TeScore score);

    /**
     * 查询本组全部学生成绩（联查姓名/学号/组名，按学号排序）
     *
     * @param groupId 小组ID
     * @return 成绩集合
     */
    public List<TeScore> selectByGroup(Long groupId);

    /**
     * 按用户查询成绩（学生端"我的成绩"）
     *
     * @param userId 用户ID
     * @return 成绩信息（未发布也可能返回，由 Service 决定是否透出）
     */
    public TeScore selectByUserId(Long userId);

    /**
     * 发布本组全部成绩（is_published=1 并记录发布时间）
     *
     * @param groupId 小组ID
     * @return 影响行数
     */
    public int publishByGroup(Long groupId);
}
