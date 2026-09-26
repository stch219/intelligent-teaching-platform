package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeClass;

/**
 * ============================================================================
 * 【功能】班级管理 Mapper 接口（表 te_class，联查统计学生数/小组数）
 * ============================================================================
 */
public interface TeClassMapper
{
    /** 查询班级列表（联查教师姓名、学生数、小组数） */
    public List<TeClass> selectTeClassList(TeClass teClass);

    /** 按ID查询班级详情 */
    public TeClass selectTeClassById(Long id);

    /** 按班级名称查班级（建班判重） */
    public TeClass selectTeClassByName(String className);

    /** 新增班级 */
    public int insertTeClass(TeClass teClass);

    /** 修改班级 */
    public int updateTeClass(TeClass teClass);

    /** 批量删除班级 */
    public int deleteTeClassByIds(Long[] ids);

    /** 统计班级学生数（删班/调整分组数前校验） */
    public int countStudentByClassId(Long classId);

    // --------------- 以下为删除班级时的级联清理（引用小组/班级的表） ---------------

    /** 删除班级下全部模块内容（协同编辑内容） */
    public int deleteContentByClassId(Long classId);

    /** 删除班级下全部贡献率记录 */
    public int deleteContributionByClassId(Long classId);

    /** 删除班级下全部成员确认记录 */
    public int deleteConfirmByClassId(Long classId);

    /** 删除班级下全部成绩记录 */
    public int deleteScoreByClassId(Long classId);

    /** 删除班级下全部预警记录 */
    public int deleteWarningRecordByClassId(Long classId);
}
