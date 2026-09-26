package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeStudent;

/**
 * ============================================================================
 * 【功能】学生管理业务接口
 * ----------------------------------------------------------------------------
 * 【说明】提供学生账号的创建（直接建号）、修改、启用/禁用、删除、
 *         Excel 批量导入/导出、班级→小组层级树查询等能力。
 * ============================================================================
 */
public interface ITeStudentService
{
    /**
     * 查询学生列表（联查账号/班级/小组信息）
     *
     * @param student 查询条件
     * @return 学生集合
     */
    public List<TeStudent> selectTeStudentList(TeStudent student);

    /**
     * 查询学生详情
     *
     * @param userId 学生用户ID
     * @return 学生信息
     */
    public TeStudent selectTeStudentByUserId(Long userId);

    /**
     * 新增学生（创建 sys_user 账号 + 写扩展表，学号即登录账号）
     *
     * @param student 学生信息
     * @param operateBy 操作人
     * @return 结果
     */
    public int insertTeStudent(TeStudent student, String operateBy);

    /**
     * 修改学生（更新 sys_user 基本信息与扩展表学籍信息）
     *
     * @param student 学生信息
     * @param operateBy 操作人
     * @return 结果
     */
    public int updateTeStudent(TeStudent student, String operateBy);

    /**
     * 修改学生账号状态（启用/禁用）
     *
     * @param student 学生信息（userId + status）
     * @param operateBy 操作人
     * @return 结果
     */
    public int changeStatus(TeStudent student, String operateBy);

    /**
     * 重置学生登录密码
     *
     * @param userId 学生用户ID
     * @param password 新密码（明文）
     * @param operateBy 操作人
     * @return 结果
     */
    public int resetPwd(Long userId, String password, String operateBy);

    /**
     * 批量删除学生
     *
     * @param userIds 学生用户ID数组
     * @return 结果
     */
    public int deleteTeStudentByUserIds(Long[] userIds);

    /**
     * 查询班级→小组层级树（学生管理左侧导航）
     *
     * @return 树节点集合
     */
    public List<Map<String, Object>> selectClassGroupTree();

    /**
     * Excel 批量导入学生（列：学号/姓名/班级/联系电话）
     *
     * @param studentList 解析后的学生数据
     * @param updateSupport 已存在学号时是否更新
     * @param operateBy 操作人
     * @return 导入结果消息
     */
    public String importStudent(List<TeStudent> studentList, boolean updateSupport, String operateBy);
}
