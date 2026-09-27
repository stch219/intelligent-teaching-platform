package com.ruoyi.teach.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeAiReview;

/**
 * ============================================================================
 * 【功能】AI批改结果 Mapper 接口（操作表 te_ai_review，唯一键 group_id+module_code）
 * ----------------------------------------------------------------------------
 * 【说明】批改采用"先删后插"整体覆盖模式：教师每次触发批改，
 *         清空该组旧记录后写入 6 条新记录（模块 5-10）。
 * ============================================================================
 */
public interface TeAiReviewMapper
{
    /**
     * 查询本组全部批改记录（联查模块名称与满分值，按模块号排序）
     *
     * @param groupId 小组ID
     * @return 批改结果集合
     */
    public List<TeAiReview> selectByGroup(Long groupId);

    /**
     * 删除本组全部批改记录（重新批改前先清空）
     *
     * @param groupId 小组ID
     * @return 影响行数
     */
    public int deleteByGroup(Long groupId);

    /**
     * 批量插入批改记录
     *
     * @param list 批改结果集合
     * @return 影响行数
     */
    public int batchInsert(List<TeAiReview> list);

    /**
     * 教师终审：更新单条记录的核定分与核查状态
     *
     * @param review 携带 id / teacherScore / teacherChecked / checkedBy
     * @return 影响行数
     */
    public int updateTeacherCheck(TeAiReview review);

    /**
     * 统计本组已批改记录数（触发批改前判断是否已有结果）
     *
     * @param groupId 小组ID
     * @return 记录数
     */
    public int countByGroup(Long groupId);
}
