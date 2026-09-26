package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeRegistration;

/**
 * ============================================================================
 * 【功能】学生注册审批业务接口
 * ----------------------------------------------------------------------------
 * 【说明】提供学生匿名提交注册申请、管理员审批（通过自动建号/驳回）、
 *         申请列表查询与删除等业务能力。
 * ============================================================================
 */
public interface ITeRegistrationService
{
    /**
     * 查询注册申请列表
     *
     * @param registration 查询条件
     * @return 申请集合
     */
    public List<TeRegistration> selectTeRegistrationList(TeRegistration registration);

    /**
     * 查询注册申请详情
     *
     * @param id 申请ID
     * @return 申请信息
     */
    public TeRegistration selectTeRegistrationById(Long id);

    /**
     * 学生匿名提交注册申请（登录页注册入口调用）
     *
     * @param registration 注册信息（姓名/学号/班级/电话）
     * @return 结果
     */
    public int submitRegistration(TeRegistration registration);

    /**
     * 审批通过（事务：创建登录账号 sys_user + 学生扩展 te_student，回填申请状态）
     *
     * @param id 申请ID
     * @param operateBy 审批人
     * @return 结果
     */
    public int approve(Long id, String operateBy);

    /**
     * 审批驳回（记录驳回原因）
     *
     * @param id 申请ID
     * @param rejectReason 驳回原因
     * @param operateBy 审批人
     * @return 结果
     */
    public int reject(Long id, String rejectReason, String operateBy);

    /**
     * 批量删除注册申请
     *
     * @param ids 申请ID数组
     * @return 结果
     */
    public int deleteTeRegistrationByIds(Long[] ids);
}
