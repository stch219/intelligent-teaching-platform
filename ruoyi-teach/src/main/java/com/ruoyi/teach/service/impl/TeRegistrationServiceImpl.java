package com.ruoyi.teach.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.teach.domain.TeRegistration;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeRegistrationMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.service.ITeRegistrationService;

/**
 * ============================================================================
 * 【功能】学生注册审批业务实现
 * ----------------------------------------------------------------------------
 * 【流程】学生匿名提交申请 →（待审批0）→ 管理员审批：
 *         通过：事务内创建登录账号（sys_user，学号即账号，初始密码 123456，
 *               授予"学生"角色）+ 写学生扩展表（te_student，按班级名匹配班级）
 *               + 回填申请表状态（1通过、审批人、时间、user_id）
 *         驳回：记录驳回原因与审批信息（2驳回）
 * ============================================================================
 */
@Service
public class TeRegistrationServiceImpl implements ITeRegistrationService
{
    /** 学生角色 role_key（sys_role 中预置） */
    private static final String STUDENT_ROLE_KEY = "student";

    @Autowired
    private TeRegistrationMapper registrationMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 查询注册申请列表
     */
    @Override
    public List<TeRegistration> selectTeRegistrationList(TeRegistration registration)
    {
        return registrationMapper.selectTeRegistrationList(registration);
    }

    /**
     * 查询注册申请详情
     */
    @Override
    public TeRegistration selectTeRegistrationById(Long id)
    {
        return registrationMapper.selectTeRegistrationById(id);
    }

    /**
     * 学生匿名提交注册申请
     * 校验：姓名/学号/班级必填；同号存在待审批申请则拒绝重复提交
     */
    @Override
    public int submitRegistration(TeRegistration registration)
    {
        // 必填项校验
        if (StringUtils.isEmpty(registration.getRealName())
                || StringUtils.isEmpty(registration.getStudentNo())
                || StringUtils.isEmpty(registration.getClassName()))
        {
            throw new ServiceException("姓名、学号、班级均为必填项");
        }
        // 防止同一学号重复提交待审批申请
        if (registrationMapper.checkStudentNoExists(registration.getStudentNo()) > 0)
        {
            throw new ServiceException("该学号已提交过申请，请等待管理员审批");
        }
        // 初始状态：待审批
        registration.setStatus("0");
        registration.setCreateTime(new Date());
        return registrationMapper.insertTeRegistration(registration);
    }

    /**
     * 审批通过（事务保证三个库表操作的一致性）
     */
    @Transactional
    @Override
    public int approve(Long id, String operateBy)
    {
        // 1. 查询申请并校验状态（仅待审批可操作）
        TeRegistration reg = registrationMapper.selectTeRegistrationById(id);
        if (reg == null)
        {
            throw new ServiceException("申请不存在");
        }
        if (!"0".equals(reg.getStatus()))
        {
            throw new ServiceException("该申请已处理，请刷新列表");
        }

        // 2. 按班级名称匹配班级（不存在则提示先由教师创建班级）
        Long classId = studentMapper.selectClassIdByName(reg.getClassName());
        if (classId == null)
        {
            throw new ServiceException("班级「" + reg.getClassName() + "」不存在，请先由教师创建班级或驳回该申请");
        }

        // 3. 学号判重（防审批并发或管理员已手工导入同号学生）
        if (studentMapper.selectTeStudentByNo(reg.getStudentNo()) != null)
        {
            throw new ServiceException("学号 " + reg.getStudentNo() + " 已存在学生账号，请驳回该申请");
        }

        // 4. 创建登录账号：学号即账号，初始密码为学号后6位，授予学生角色
        SysUser user = new SysUser();
        user.setUserName(reg.getStudentNo());
        user.setNickName(reg.getRealName());
        user.setPhonenumber(reg.getPhone());
        user.setPassword(SecurityUtils.encryptPassword(initPassword(reg.getStudentNo())));
        user.setStatus("0");
        user.setRoleIds(new Long[]{ getStudentRoleId() });
        user.setCreateBy(operateBy);
        userService.insertUser(user);

        // 5. 写学生扩展表（默认角色=成员，分工待确认）
        TeStudent student = new TeStudent();
        student.setUserId(user.getUserId());
        student.setStudentNo(reg.getStudentNo());
        student.setClassId(classId);
        student.setRoleType(4L);
        student.setDutyStatus(0L);
        student.setCreateTime(new Date());
        studentMapper.insertTeStudent(student);

        // 6. 回填申请状态：通过
        reg.setStatus("1");
        reg.setUserId(user.getUserId());
        reg.setApproveBy(operateBy);
        reg.setApproveTime(new Date());
        return registrationMapper.updateTeRegistration(reg);
    }

    /**
     * 审批驳回：记录驳回原因与审批信息
     */
    @Override
    public int reject(Long id, String rejectReason, String operateBy)
    {
        TeRegistration reg = registrationMapper.selectTeRegistrationById(id);
        if (reg == null)
        {
            throw new ServiceException("申请不存在");
        }
        if (!"0".equals(reg.getStatus()))
        {
            throw new ServiceException("该申请已处理，请刷新列表");
        }
        reg.setStatus("2");
        reg.setRejectReason(StringUtils.isEmpty(rejectReason) ? "不符合要求" : rejectReason);
        reg.setApproveBy(operateBy);
        reg.setApproveTime(new Date());
        return registrationMapper.updateTeRegistration(reg);
    }

    /**
     * 批量删除注册申请
     */
    @Override
    public int deleteTeRegistrationByIds(Long[] ids)
    {
        return registrationMapper.deleteTeRegistrationByIds(ids);
    }

    /**
     * 获取学生角色的 role_id（按 role_key 查找）
     */
    private Long getStudentRoleId()
    {
        for (SysRole role : roleService.selectRoleAll())
        {
            if (STUDENT_ROLE_KEY.equals(role.getRoleKey()))
            {
                return role.getRoleId();
            }
        }
        throw new ServiceException("系统未初始化「学生」角色，请先执行 sql/itp_menu.sql");
    }

    /**
     * 生成学生初始密码：学号后6位（学号不足6位时取全学号）
     *
     * @param studentNo 学号
     * @return 初始密码（明文）
     */
    private String initPassword(String studentNo)
    {
        return studentNo.length() > 6 ? studentNo.substring(studentNo.length() - 6) : studentNo;
    }
}
