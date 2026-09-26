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
import com.ruoyi.teach.domain.TeTeacher;
import com.ruoyi.teach.mapper.TeTeacherMapper;
import com.ruoyi.teach.service.ITeTeacherService;

/**
 * ============================================================================
 * 【功能】教师管理业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 全平台教师数量上限 4 名（新增前统计 te_teacher 行数校验）
 *   2. 教师账号复用 sys_user，授予"教师"角色（role_key=teacher）
 *   3. 启用/禁用：修改 sys_user.status，禁用后教师无法登录
 *   4. 删除：强校验"仅可删除无指导班级的教师"（te_class.teacher_id 无记录），
 *      删除时同时清理 sys_user 与 te_teacher 扩展记录
 * ============================================================================
 */
@Service
public class TeTeacherServiceImpl implements ITeTeacherService
{
    /** 教师角色 role_key（sys_role 中预置） */
    private static final String TEACHER_ROLE_KEY = "teacher";

    /** 教师数量上限（需求规定全平台最多 4 名教师） */
    private static final int MAX_TEACHER_COUNT = 4;

    @Autowired
    private TeTeacherMapper teacherMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 查询教师列表
     */
    @Override
    public List<TeTeacher> selectTeTeacherList(TeTeacher teacher)
    {
        return teacherMapper.selectTeTeacherList(teacher);
    }

    /**
     * 查询教师详情
     */
    @Override
    public TeTeacher selectTeTeacherByUserId(Long userId)
    {
        return teacherMapper.selectTeTeacherByUserId(userId);
    }

    /**
     * 新增教师（事务：上限校验 → 账号判重 → 建 sys_user → 写扩展表）
     */
    @Transactional
    @Override
    public int insertTeTeacher(TeTeacher teacher, String operateBy)
    {
        // 1. 上限校验：全平台最多 4 名教师
        if (teacherMapper.countTeTeacher() >= MAX_TEACHER_COUNT)
        {
            throw new ServiceException("教师数量已达上限（" + MAX_TEACHER_COUNT + " 名），无法继续新增");
        }

        // 2. 必填校验
        if (StringUtils.isEmpty(teacher.getUserName()) || StringUtils.isEmpty(teacher.getNickName()))
        {
            throw new ServiceException("登录账号与教师姓名为必填项");
        }

        // 3. 登录账号唯一性校验（复用系统用户校验逻辑）
        SysUser check = new SysUser();
        check.setUserName(teacher.getUserName());
        if (!userService.checkUserNameUnique(check))
        {
            throw new ServiceException("登录账号「" + teacher.getUserName() + "」已存在");
        }

        // 4. 创建系统用户（初始密码统一 123456，授予教师角色）
        SysUser user = new SysUser();
        user.setUserName(teacher.getUserName());
        user.setNickName(teacher.getNickName());
        user.setPhonenumber(teacher.getPhonenumber());
        user.setEmail(teacher.getEmail());
        user.setPassword(SecurityUtils.encryptPassword("123456"));
        user.setStatus("0");
        user.setRoleIds(new Long[]{ getTeacherRoleId() });
        user.setCreateBy(operateBy);
        user.setRemark(teacher.getRemark());
        userService.insertUser(user);

        // 5. 写教师扩展表（职称）
        TeTeacher ext = new TeTeacher();
        ext.setUserId(user.getUserId());
        ext.setTitle(teacher.getTitle());
        ext.setCreateTime(new Date());
        ext.setRemark(teacher.getRemark());
        return teacherMapper.insertTeTeacher(ext);
    }

    /**
     * 修改教师（更新 sys_user 基本信息 + 扩展表职称）
     */
    @Transactional
    @Override
    public int updateTeTeacher(TeTeacher teacher, String operateBy)
    {
        if (teacher.getUserId() == null)
        {
            throw new ServiceException("缺少教师用户ID");
        }
        // 更新系统用户表（保留教师角色，避免角色被清空）
        SysUser user = new SysUser();
        user.setUserId(teacher.getUserId());
        user.setNickName(teacher.getNickName());
        user.setPhonenumber(teacher.getPhonenumber());
        user.setEmail(teacher.getEmail());
        user.setRoleIds(new Long[]{ getTeacherRoleId() });
        user.setUpdateBy(operateBy);
        userService.updateUser(user);

        // 更新扩展表职称
        TeTeacher ext = new TeTeacher();
        ext.setUserId(teacher.getUserId());
        ext.setTitle(teacher.getTitle());
        ext.setUpdateTime(new Date());
        ext.setRemark(teacher.getRemark());
        return teacherMapper.updateTeTeacher(ext);
    }

    /**
     * 修改教师账号状态（启用/禁用）
     */
    @Override
    public int changeStatus(TeTeacher teacher, String operateBy)
    {
        SysUser user = new SysUser();
        user.setUserId(teacher.getUserId());
        user.setStatus(teacher.getStatus());
        user.setUpdateBy(operateBy);
        return userService.updateUserStatus(user);
    }

    /**
     * 重置教师登录密码
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
     * 批量删除教师（强校验：名下有指导班级则整体拒绝删除）
     */
    @Transactional
    @Override
    public int deleteTeTeacherByUserIds(Long[] userIds)
    {
        // 逐个校验指导班级数，任一教师名下有班级则全部拒绝（保证操作语义清晰）
        for (Long userId : userIds)
        {
            TeTeacher t = teacherMapper.selectTeTeacherByUserId(userId);
            if (t == null)
            {
                continue;
            }
            if (teacherMapper.countClassByTeacher(userId) > 0)
            {
                throw new ServiceException("教师「" + t.getNickName() + "」名下仍有指导班级，不允许删除。请先转移或删除其班级");
            }
        }
        // 先删扩展表，再删系统账号
        teacherMapper.deleteTeTeacherByUserIds(userIds);
        return userService.deleteUserByIds(userIds);
    }

    /**
     * 获取教师角色的 role_id（按 role_key 查找）
     */
    private Long getTeacherRoleId()
    {
        for (SysRole role : roleService.selectRoleAll())
        {
            if (TEACHER_ROLE_KEY.equals(role.getRoleKey()))
            {
                return role.getRoleId();
            }
        }
        throw new ServiceException("系统未初始化「教师」角色，请先执行 sql/itp_menu.sql");
    }
}
