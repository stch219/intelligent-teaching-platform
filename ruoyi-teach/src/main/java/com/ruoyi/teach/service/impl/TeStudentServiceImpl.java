package com.ruoyi.teach.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.service.ITeStudentService;

/**
 * ============================================================================
 * 【功能】学生管理业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 学生账号复用 sys_user（学号即登录账号），授予"学生"角色（role_key=student）
 *   2. 管理员可直接建号，也可 Excel 批量导入（列：学号/姓名/班级/联系电话）；
 *      注册审批制账号由 TeRegistrationServiceImpl 创建
 *   3. 删除时同时清理 sys_user 与 te_student 扩展记录
 *   4. 分组信息由教师端"自动分组"功能维护，本模块仅支持查看与班级调整
 * ============================================================================
 */
@Service
public class TeStudentServiceImpl implements ITeStudentService
{
    /** 学生角色 role_key（sys_role 中预置） */
    private static final String STUDENT_ROLE_KEY = "student";

    /** 学生初始密码 */
    private static final String INIT_PASSWORD = "123456";

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 查询学生列表
     */
    @Override
    public List<TeStudent> selectTeStudentList(TeStudent student)
    {
        return studentMapper.selectTeStudentList(student);
    }

    /**
     * 查询学生详情
     */
    @Override
    public TeStudent selectTeStudentByUserId(Long userId)
    {
        return studentMapper.selectTeStudentByUserId(userId);
    }

    /**
     * 新增学生（事务：学号判重 → 建 sys_user → 写扩展表）
     */
    @Transactional
    @Override
    public int insertTeStudent(TeStudent student, String operateBy)
    {
        // 学号即登录账号，必填
        if (StringUtils.isEmpty(student.getStudentNo()) || StringUtils.isEmpty(student.getNickName()))
        {
            throw new ServiceException("学号与姓名为必填项");
        }
        // 学号唯一性校验
        if (studentMapper.selectTeStudentByNo(student.getStudentNo()) != null)
        {
            throw new ServiceException("学号 " + student.getStudentNo() + " 已存在");
        }
        // 创建系统账号（初始密码 123456，授予学生角色）
        SysUser user = new SysUser();
        user.setUserName(student.getStudentNo());
        user.setNickName(student.getNickName());
        user.setPhonenumber(student.getPhonenumber());
        user.setPassword(SecurityUtils.encryptPassword(INIT_PASSWORD));
        user.setStatus("0");
        user.setRoleIds(new Long[]{ getStudentRoleId() });
        user.setCreateBy(operateBy);
        userService.insertUser(user);

        // 写学生扩展表
        student.setUserId(user.getUserId());
        student.setRoleType(student.getRoleType() == null ? 4L : student.getRoleType());
        student.setDutyStatus(0L);
        student.setCreateTime(new Date());
        return studentMapper.insertTeStudent(student);
    }

    /**
     * 修改学生（更新 sys_user 基本信息 + 扩展表学籍信息）
     */
    @Transactional
    @Override
    public int updateTeStudent(TeStudent student, String operateBy)
    {
        if (student.getUserId() == null)
        {
            throw new ServiceException("缺少学生用户ID");
        }
        // 更新系统用户表姓名/电话（保留学生角色）
        SysUser user = new SysUser();
        user.setUserId(student.getUserId());
        user.setNickName(student.getNickName());
        user.setPhonenumber(student.getPhonenumber());
        user.setRoleIds(new Long[]{ getStudentRoleId() });
        user.setUpdateBy(operateBy);
        userService.updateUser(user);

        // 更新扩展表（学号/班级/小组/组内角色）
        student.setUpdateTime(new Date());
        return studentMapper.updateTeStudent(student);
    }

    /**
     * 修改学生账号状态（启用/禁用）
     */
    @Override
    public int changeStatus(TeStudent student, String operateBy)
    {
        SysUser user = new SysUser();
        user.setUserId(student.getUserId());
        user.setStatus(student.getStatus());
        user.setUpdateBy(operateBy);
        return userService.updateUserStatus(user);
    }

    /**
     * 重置学生登录密码
     */
    @Override
    public int resetPwd(Long userId, String password, String operateBy)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setUpdateBy(operateBy);
        return userService.resetPwd(user);
    }

    /**
     * 批量删除学生（清理扩展表 + 系统账号）
     */
    @Transactional
    @Override
    public int deleteTeStudentByUserIds(Long[] userIds)
    {
        studentMapper.deleteTeStudentByUserIds(userIds);
        return userService.deleteUserByIds(userIds);
    }

    /**
     * 查询班级→小组层级树
     */
    @Override
    public List<Map<String, Object>> selectClassGroupTree()
    {
        return studentMapper.selectClassGroupTree();
    }

    /**
     * Excel 批量导入学生
     * 列：学号、姓名、班级、联系电话；已存在学号按 updateSupport 决定更新或跳过
     */
    @Override
    public String importStudent(List<TeStudent> studentList, boolean updateSupport, String operateBy)
    {
        if (studentList == null || studentList.isEmpty())
        {
            throw new ServiceException("导入数据不能为空！");
        }
        int successNum = 0;   // 成功条数
        int failureNum = 0;   // 失败条数
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        for (TeStudent row : studentList)
        {
            try
            {
                // 行级必填校验
                if (StringUtils.isEmpty(row.getStudentNo()) || StringUtils.isEmpty(row.getNickName()))
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、学号或姓名为空，跳过");
                    continue;
                }
                // 班级名称匹配（匹配不到则本行失败）
                Long classId = null;
                if (StringUtils.isNotEmpty(row.getClassName()))
                {
                    classId = studentMapper.selectClassIdByName(row.getClassName());
                }
                if (classId == null)
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、学号 " + row.getStudentNo()
                            + " 导入失败：班级「" + row.getClassName() + "」不存在");
                    continue;
                }
                // 判重：已存在按 updateSupport 决定更新或跳过
                TeStudent exist = studentMapper.selectTeStudentByNo(row.getStudentNo());
                if (exist == null)
                {
                    // 新建：账号 + 扩展记录
                    SysUser user = new SysUser();
                    user.setUserName(row.getStudentNo());
                    user.setNickName(row.getNickName());
                    user.setPhonenumber(row.getPhonenumber());
                    user.setPassword(SecurityUtils.encryptPassword(INIT_PASSWORD));
                    user.setStatus("0");
                    user.setRoleIds(new Long[]{ getStudentRoleId() });
                    user.setCreateBy(operateBy);
                    userService.insertUser(user);

                    TeStudent student = new TeStudent();
                    student.setUserId(user.getUserId());
                    student.setStudentNo(row.getStudentNo());
                    student.setClassId(classId);
                    student.setRoleType(4L);
                    student.setDutyStatus(0L);
                    student.setCreateTime(new Date());
                    studentMapper.insertTeStudent(student);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、学号 " + row.getStudentNo() + " 导入成功");
                }
                else if (updateSupport)
                {
                    // 更新已有学生：姓名/电话/班级
                    TeStudent student = new TeStudent();
                    student.setUserId(exist.getUserId());
                    student.setNickName(row.getNickName());
                    student.setPhonenumber(row.getPhonenumber());
                    student.setClassId(classId);
                    student.setUpdateTime(new Date());
                    studentMapper.updateTeStudent(student);

                    SysUser user = new SysUser();
                    user.setUserId(exist.getUserId());
                    user.setNickName(row.getNickName());
                    user.setPhonenumber(row.getPhonenumber());
                    user.setUpdateBy(operateBy);
                    userService.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、学号 " + row.getStudentNo() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、学号 " + row.getStudentNo() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、学号 " + row.getStudentNo() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
            }
        }
        // 汇总导入结果
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
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
}
