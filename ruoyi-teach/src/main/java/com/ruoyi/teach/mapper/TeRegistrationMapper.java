package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeRegistration;

/**
 * ============================================================================
 * 【功能】学生注册审批 Mapper 接口（操作表 te_registration）
 * ----------------------------------------------------------------------------
 * 【说明】提供注册申请的查询/新增/审批（通过、驳回）/删除等数据库操作。
 * ============================================================================
 */
public interface TeRegistrationMapper
{
    /**
     * 查询注册申请列表（支持条件：姓名、学号、班级、审批状态）
     *
     * @param registration 查询条件
     * @return 注册申请集合
     */
    public List<TeRegistration> selectTeRegistrationList(TeRegistration registration);

    /**
     * 根据申请ID查询注册申请详情
     *
     * @param id 申请ID
     * @return 注册申请信息
     */
    public TeRegistration selectTeRegistrationById(Long id);

    /**
     * 校验学号是否已存在申请（防止重复提交）
     *
     * @param studentNo 学号
     * @return 已存在数量
     */
    public int checkStudentNoExists(String studentNo);

    /**
     * 新增注册申请（学生匿名提交）
     *
     * @param registration 注册申请信息
     * @return 影响行数
     */
    public int insertTeRegistration(TeRegistration registration);

    /**
     * 审批更新注册申请（通过时回填 user_id，驳回时记录原因）
     *
     * @param registration 注册申请信息
     * @return 影响行数
     */
    public int updateTeRegistration(TeRegistration registration);

    /**
     * 批量删除注册申请
     *
     * @param ids 申请ID数组
     * @return 影响行数
     */
    public int deleteTeRegistrationByIds(Long[] ids);
}
