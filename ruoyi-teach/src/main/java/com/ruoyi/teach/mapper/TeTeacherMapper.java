package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeTeacher;

/**
 * ============================================================================
 * 【功能】教师管理 Mapper 接口（操作表 te_teacher，联查 sys_user / te_class）
 * ----------------------------------------------------------------------------
 * 【说明】教师账号复用 sys_user，本接口维护 te_teacher 扩展表，
 *         并提供联查（列表含账号/姓名/指导班级数）、教师总数统计（上限4校验）、
 *         指导班级数统计（删除前校验"仅可删除无指导班级的教师"）。
 * ============================================================================
 */
public interface TeTeacherMapper
{
    /**
     * 查询教师列表（联查 sys_user 与指导班级数）
     *
     * @param teacher 查询条件（姓名/账号/状态）
     * @return 教师集合
     */
    public List<TeTeacher> selectTeTeacherList(TeTeacher teacher);

    /**
     * 根据用户ID查询教师扩展信息（联查 sys_user）
     *
     * @param userId 用户ID
     * @return 教师信息
     */
    public TeTeacher selectTeTeacherByUserId(Long userId);

    /**
     * 统计教师总数（创建前校验上限 4 名）
     *
     * @return 教师数量
     */
    public int countTeTeacher();

    /**
     * 统计教师指导的班级数量（删除前校验，0 才允许删除）
     *
     * @param userId 教师用户ID
     * @return 指导班级数量
     */
    public int countClassByTeacher(Long userId);

    /**
     * 新增教师扩展记录（sys_user 由系统用户服务创建）
     *
     * @param teacher 教师扩展信息
     * @return 影响行数
     */
    public int insertTeTeacher(TeTeacher teacher);

    /**
     * 修改教师扩展记录（职称等）
     *
     * @param teacher 教师扩展信息
     * @return 影响行数
     */
    public int updateTeTeacher(TeTeacher teacher);

    /**
     * 批量删除教师扩展记录（sys_user 由系统用户服务删除）
     *
     * @param userIds 用户ID数组
     * @return 影响行数
     */
    public int deleteTeTeacherByUserIds(Long[] userIds);
}
