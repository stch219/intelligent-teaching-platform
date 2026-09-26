package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeTeacher;

/**
 * ============================================================================
 * 【功能】教师管理业务接口
 * ----------------------------------------------------------------------------
 * 【说明】提供教师账号的创建（全平台上限 4 名校验）、修改、启用/禁用、
 *         删除（仅可删除无指导班级的教师，删除前 Service 层强校验）等能力。
 * ============================================================================
 */
public interface ITeTeacherService
{
    /**
     * 查询教师列表（联查账号信息与指导班级数）
     *
     * @param teacher 查询条件
     * @return 教师集合
     */
    public List<TeTeacher> selectTeTeacherList(TeTeacher teacher);

    /**
     * 查询教师详情
     *
     * @param userId 教师用户ID
     * @return 教师信息
     */
    public TeTeacher selectTeTeacherByUserId(Long userId);

    /**
     * 新增教师（上限 4 校验 + 创建 sys_user 账号 + 写扩展表）
     *
     * @param teacher 教师信息（含初始密码）
     * @param operateBy 操作人
     * @return 结果
     */
    public int insertTeTeacher(TeTeacher teacher, String operateBy);

    /**
     * 修改教师（更新 sys_user 基本信息与扩展表职称）
     *
     * @param teacher 教师信息
     * @param operateBy 操作人
     * @return 结果
     */
    public int updateTeTeacher(TeTeacher teacher, String operateBy);

    /**
     * 修改教师账号状态（启用/禁用）
     *
     * @param teacher 教师信息（userId + status）
     * @param operateBy 操作人
     * @return 结果
     */
    public int changeStatus(TeTeacher teacher, String operateBy);

    /**
     * 重置教师登录密码
     *
     * @param userId 教师用户ID
     * @param password 新密码（明文）
     * @param operateBy 操作人
     * @return 结果
     */
    public int resetPwd(Long userId, String password, String operateBy);

    /**
     * 批量删除教师（强校验：名下有指导班级则拒绝删除）
     *
     * @param userIds 教师用户ID数组
     * @return 结果
     */
    public int deleteTeTeacherByUserIds(Long[] userIds);
}
