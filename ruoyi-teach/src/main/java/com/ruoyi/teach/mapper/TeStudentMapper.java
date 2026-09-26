package com.ruoyi.teach.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeStudent;

/**
 * ============================================================================
 * 【功能】学生管理 Mapper 接口（操作表 te_student，联查 sys_user/te_class/te_group）
 * ----------------------------------------------------------------------------
 * 【说明】学生账号复用 sys_user，本接口维护 te_student 扩展表，
 *         支持按班级/小组筛选的联查列表、"班级→小组"层级树、
 *         学号唯一性校验、批量导入（匹配班级名/组名）等。
 * ============================================================================
 */
public interface TeStudentMapper
{
    /**
     * 查询学生列表（联查账号/姓名/班级名/组名，支持班级、小组、姓名、学号筛选）
     *
     * @param student 查询条件
     * @return 学生集合
     */
    public List<TeStudent> selectTeStudentList(TeStudent student);

    /**
     * 根据用户ID查询学生扩展信息（联查 sys_user）
     *
     * @param userId 用户ID
     * @return 学生信息
     */
    public TeStudent selectTeStudentByUserId(Long userId);

    /**
     * 根据学号查询学生扩展信息（导入时判重）
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    public TeStudent selectTeStudentByNo(String studentNo);

    /**
     * 查询"班级→小组"层级树（学生管理左侧导航树）
     *
     * @return 树节点集合（level=1 班级，level=2 小组）
     */
    public List<Map<String, Object>> selectClassGroupTree();

    /**
     * 根据班级名称查询班级ID（注册审批、导入时按名称匹配）
     *
     * @param className 班级名称
     * @return 班级ID（不存在返回 null）
     */
    public Long selectClassIdByName(String className);

    /**
     * 根据班级ID与组序号查询小组ID（导入时按"第N组"匹配）
     *
     * @param param 查询参数（classId 班级ID，groupOrder 组序号）
     * @return 小组ID（不存在返回 null）
     */
    public Long selectGroupIdByOrder(Map<String, Object> param);

    /**
     * 新增学生扩展记录（sys_user 由系统用户服务创建）
     *
     * @param student 学生扩展信息
     * @return 影响行数
     */
    public int insertTeStudent(TeStudent student);

    /**
     * 修改学生扩展记录（班级/小组/角色调整）
     *
     * @param student 学生扩展信息
     * @return 影响行数
     */
    public int updateTeStudent(TeStudent student);

    /**
     * 批量删除学生扩展记录（sys_user 由系统用户服务删除）
     *
     * @param userIds 用户ID数组
     * @return 影响行数
     */
    public int deleteTeStudentByUserIds(Long[] userIds);
}
