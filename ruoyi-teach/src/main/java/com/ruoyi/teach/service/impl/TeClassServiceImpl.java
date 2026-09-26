package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeGroup;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeGroupMapper;
import com.ruoyi.teach.mapper.TeModuleScoreSetMapper;
import com.ruoyi.teach.mapper.TeModuleSetMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeTaskAssignMapper;
import com.ruoyi.teach.mapper.TeTaskMapper;
import com.ruoyi.teach.mapper.TeWarningRuleMapper;
import com.ruoyi.teach.mapper.TeMaterialMapper;
import com.ruoyi.teach.service.ITeClassService;

/**
 * ============================================================================
 * 【功能】班级管理业务实现（教师端核心）
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 建班时按分组数（4/5/6）自动生成字母小组：A组/B组/…/F组，
 *      每组分配专属色条底纹颜色
 *   2. 名单导入自动建号：账号=学号，初始密码=学号后6位（不足6位取全学号），
 *      授予"学生"角色，并按"学号末2位 对 组数取模（余0归最后一组）"自动分组
 *   3. 师生私有绑定：班级归属指导教师（teacher_id），教师仅可见/管理自己的班级
 *   4. 删除班级强校验：班内有学生则拒绝；通过后级联清理内容/贡献率/成绩/
 *      任务分配/任务/小组/模块设置/资料/预警规则/赋分等全部关联数据
 *   5. 调整分组数强校验：班内有学生或存在任务分配时拒绝
 * ============================================================================
 */
@Service
public class TeClassServiceImpl implements ITeClassService
{
    /** 学生角色 role_key（sys_role 中预置） */
    private static final String STUDENT_ROLE_KEY = "student";

    /** 小组字母顺序（分组数最多6组：A~F） */
    private static final String GROUP_LETTERS = "ABCDEF";

    /** 小组专属色条底纹调色板（与字母顺序一一对应） */
    private static final String[] GROUP_COLORS = { "#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#9B59E6", "#00B4D8" };

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeGroupMapper groupMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeTaskMapper taskMapper;

    @Autowired
    private TeTaskAssignMapper taskAssignMapper;

    @Autowired
    private TeModuleSetMapper moduleSetMapper;

    @Autowired
    private TeMaterialMapper materialMapper;

    @Autowired
    private TeWarningRuleMapper warningRuleMapper;

    @Autowired
    private TeModuleScoreSetMapper moduleScoreSetMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 查询班级列表（是否过滤教师ID由控制器决定）
     */
    @Override
    public List<TeClass> selectTeClassList(TeClass teClass)
    {
        return classMapper.selectTeClassList(teClass);
    }

    /**
     * 查询班级详情
     */
    @Override
    public TeClass selectTeClassById(Long id)
    {
        return classMapper.selectTeClassById(id);
    }

    /**
     * 新增班级（事务：校验 → 建班 → 自动生成字母小组）
     */
    @Transactional
    @Override
    public int insertTeClass(TeClass teClass, String operateBy)
    {
        // 1. 必填与取值校验
        if (StringUtils.isEmpty(teClass.getClassName()) || teClass.getTeacherId() == null)
        {
            throw new ServiceException("班级名称与指导教师为必填项");
        }
        long groupCount = teClass.getGroupCount() == null ? 4 : teClass.getGroupCount();
        if (groupCount < 4 || groupCount > 6)
        {
            throw new ServiceException("分组数仅支持 4/5/6 组");
        }
        // 2. 班级名称唯一校验（名单导入按名称匹配班级，须全平台唯一）
        if (classMapper.selectTeClassByName(teClass.getClassName()) != null)
        {
            throw new ServiceException("班级名称「" + teClass.getClassName() + "」已存在");
        }
        // 3. 落库
        teClass.setGroupCount(groupCount);
        teClass.setStatus("0");
        teClass.setCreateBy(operateBy);
        teClass.setCreateTime(new Date());
        int rows = classMapper.insertTeClass(teClass);
        // 4. 按分组数自动生成字母小组
        createGroups(teClass.getId(), groupCount);
        return rows;
    }

    /**
     * 修改班级（调整分组数时强校验：无学生且无任务分配）
     */
    @Transactional
    @Override
    public int updateTeClass(TeClass teClass, String operateBy)
    {
        TeClass exist = classMapper.selectTeClassById(teClass.getId());
        if (exist == null)
        {
            throw new ServiceException("班级不存在");
        }
        // 班级名称唯一性校验（排除自身）
        if (StringUtils.isNotEmpty(teClass.getClassName()))
        {
            TeClass byName = classMapper.selectTeClassByName(teClass.getClassName());
            if (byName != null && !byName.getId().equals(teClass.getId()))
            {
                throw new ServiceException("班级名称「" + teClass.getClassName() + "」已被使用");
            }
        }
        // 分组数变化校验：已有学生则拒绝（避免打乱已绑定关系）
        if (teClass.getGroupCount() != null && !teClass.getGroupCount().equals(exist.getGroupCount()))
        {
            long gc = teClass.getGroupCount();
            if (gc < 4 || gc > 6)
            {
                throw new ServiceException("分组数仅支持 4/5/6 组");
            }
            if (classMapper.countStudentByClassId(teClass.getId()) > 0)
            {
                throw new ServiceException("班级已有学生名单，不允许调整分组数。如需调整请先清空名单");
            }
            if (taskAssignMapper.countByClassId(teClass.getId()) > 0)
            {
                throw new ServiceException("班级已存在任务分配记录，不允许调整分组数");
            }
            // 重建小组（删旧建新）
            groupMapper.deleteByClassId(teClass.getId());
            createGroups(teClass.getId(), gc);
        }
        teClass.setUpdateBy(operateBy);
        teClass.setUpdateTime(new Date());
        return classMapper.updateTeClass(teClass);
    }

    /**
     * 批量删除班级（强校验无学生；级联清理全部关联配置与业务数据）
     */
    @Transactional
    @Override
    public int deleteTeClassByIds(Long[] ids, String operateBy)
    {
        for (Long id : ids)
        {
            TeClass teClass = classMapper.selectTeClassById(id);
            if (teClass == null)
            {
                continue;
            }
            // 班内有学生则拒绝删除（保护学生账号与学籍数据）
            if (classMapper.countStudentByClassId(id) > 0)
            {
                throw new ServiceException("班级「" + teClass.getClassName() + "」名下仍有学生，不允许删除。请先清空学生名单");
            }
            // 级联清理：业务内容 → 配置数据 → 小组 → 班级
            classMapper.deleteContributionByClassId(id);
            classMapper.deleteConfirmByClassId(id);
            classMapper.deleteContentByClassId(id);
            classMapper.deleteScoreByClassId(id);
            classMapper.deleteWarningRecordByClassId(id);
            taskAssignMapper.deleteByClassId(id);
            taskMapper.deleteByClassId(id);
            moduleSetMapper.deleteByClassId(id);
            materialMapper.deleteByClassId(id);
            warningRuleMapper.deleteByClassId(id);
            moduleScoreSetMapper.deleteByClassId(id);
            groupMapper.deleteByClassId(id);
        }
        return classMapper.deleteTeClassByIds(ids);
    }

    /**
     * Excel 名单导入（事务内逐行处理）
     * 列：学号/姓名（联系电话选填）；自动建号 + 自动分组 + 归入本班
     */
    @Transactional
    @Override
    public String importStudents(Long classId, List<TeStudent> studentList, boolean updateSupport, String operateBy)
    {
        // 0. 班级校验（班级必须存在且小组已生成）
        TeClass teClass = classMapper.selectTeClassById(classId);
        if (teClass == null)
        {
            throw new ServiceException("班级不存在");
        }
        int groupCount = teClass.getGroupCount() == null ? 4 : teClass.getGroupCount().intValue();
        if (studentList == null || studentList.isEmpty())
        {
            throw new ServiceException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        for (TeStudent row : studentList)
        {
            try
            {
                // 1. 行级必填校验
                if (StringUtils.isEmpty(row.getStudentNo()) || StringUtils.isEmpty(row.getNickName()))
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、学号或姓名为空，跳过");
                    continue;
                }
                // 2. 学号末2位必须为数字（自动分组规则依赖）
                int groupOrder = calcGroupOrder(row.getStudentNo(), groupCount);

                // 3. 按组序号定位小组ID（建班时已生成）
                Map<String, Object> param = new HashMap<>();
                param.put("classId", classId);
                param.put("groupOrder", groupOrder);
                Long groupId = studentMapper.selectGroupIdByOrder(param);

                // 4. 判重：已存在按 updateSupport 决定更新或跳过
                TeStudent exist = studentMapper.selectTeStudentByNo(row.getStudentNo());
                if (exist == null)
                {
                    // 4.1 新建：创建登录账号（初始密码=学号后6位，授予学生角色）
                    SysUser user = new SysUser();
                    user.setUserName(row.getStudentNo());
                    user.setNickName(row.getNickName());
                    user.setPhonenumber(row.getPhonenumber());
                    user.setPassword(SecurityUtils.encryptPassword(initPassword(row.getStudentNo())));
                    user.setStatus("0");
                    user.setRoleIds(new Long[]{ getStudentRoleId() });
                    user.setCreateBy(operateBy);
                    userService.insertUser(user);

                    // 4.2 写学生扩展记录：归入本班 + 自动分组
                    TeStudent student = new TeStudent();
                    student.setUserId(user.getUserId());
                    student.setStudentNo(row.getStudentNo());
                    student.setClassId(classId);
                    student.setGroupId(groupId);
                    student.setRoleType(4L);
                    student.setDutyStatus(0L);
                    student.setCreateTime(new Date());
                    studentMapper.insertTeStudent(student);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、学号 " + row.getStudentNo()
                            + " 建号成功并分入 " + letterOf(groupOrder) + " 组");
                }
                else if (updateSupport)
                {
                    // 4.3 更新已有学生：改入本班并重新分组
                    TeStudent student = new TeStudent();
                    student.setUserId(exist.getUserId());
                    student.setClassId(classId);
                    student.setGroupId(groupId);
                    student.setUpdateTime(new Date());
                    studentMapper.updateTeStudent(student);

                    // 同步系统用户姓名/电话
                    SysUser user = new SysUser();
                    user.setUserId(exist.getUserId());
                    user.setNickName(row.getNickName());
                    user.setPhonenumber(row.getPhonenumber());
                    user.setUpdateBy(operateBy);
                    userService.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、学号 " + row.getStudentNo()
                            + " 更新成功并重新分入 " + letterOf(groupOrder) + " 组");
                }
                else
                {
                    // 4.4 已存在且不允许更新：跳过
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、学号 " + row.getStudentNo() + " 已存在，跳过");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                failureMsg.append("<br/>" + failureNum + "、学号 " + row.getStudentNo() + " 导入失败：" + e.getMessage());
            }
        }
        // 5. 汇总导入结果
        if (failureNum > 0)
        {
            return "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确（自动分组要求学号末2位为数字）：<br/>"
                    + failureMsg + (successNum > 0 ? "<br/>其余 " + successNum + " 条已成功导入" : "");
        }
        return "恭喜您，全部导入成功！共 " + successNum + " 条";
    }

    /**
     * 分组看板：小组列表 + 组内成员 + 已分配任务（师生绑定数据组装）
     */
    @Override
    public List<Map<String, Object>> selectGroupBoard(Long classId)
    {
        // 1. 查询小组与班内全部学生
        List<TeGroup> groups = groupMapper.selectByClassId(classId);
        TeStudent query = new TeStudent();
        query.setClassId(classId);
        List<TeStudent> students = studentMapper.selectTeStudentList(query);
        // 2. 组装：每个小组挂载成员列表与人数
        List<Map<String, Object>> board = new ArrayList<>();
        for (TeGroup g : groups)
        {
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("groupName", g.getGroupName());
            item.put("groupOrder", g.getGroupOrder());
            item.put("colorCode", g.getColorCode());
            List<TeStudent> members = new ArrayList<>();
            for (TeStudent s : students)
            {
                if (g.getId().equals(s.getGroupId()))
                {
                    members.add(s);
                }
            }
            item.put("members", members);
            item.put("memberCount", (long) members.size());
            board.add(item);
        }
        return board;
    }

    /**
     * 自动重新分组：清空本班学生的组归属后按规则重新计算
     */
    @Transactional
    @Override
    public String autoGrouping(Long classId)
    {
        TeClass teClass = classMapper.selectTeClassById(classId);
        if (teClass == null)
        {
            throw new ServiceException("班级不存在");
        }
        int groupCount = teClass.getGroupCount() == null ? 4 : teClass.getGroupCount().intValue();
        // 查询本班全部学生
        TeStudent query = new TeStudent();
        query.setClassId(classId);
        List<TeStudent> students = studentMapper.selectTeStudentList(query);
        int count = 0;
        for (TeStudent s : students)
        {
            // 按学号末2位对组数取模重新归组
            int groupOrder = calcGroupOrder(s.getStudentNo(), groupCount);
            Map<String, Object> param = new HashMap<>();
            param.put("classId", classId);
            param.put("groupOrder", groupOrder);
            Long groupId = studentMapper.selectGroupIdByOrder(param);
            TeStudent update = new TeStudent();
            update.setUserId(s.getUserId());
            update.setGroupId(groupId);
            update.setUpdateTime(new Date());
            studentMapper.updateTeStudent(update);
            count++;
        }
        return "自动分组完成，共调整 " + count + " 名学生";
    }

    /**
     * 手动调整单个学生的分组（校验小组必须属于该生所在班级）
     */
    @Override
    public int assignStudent(Long userId, Long groupId)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student == null)
        {
            throw new ServiceException("学生不存在");
        }
        TeGroup group = groupMapper.selectById(groupId);
        if (group == null || !group.getClassId().equals(student.getClassId()))
        {
            throw new ServiceException("目标小组不属于该生所在班级");
        }
        TeStudent update = new TeStudent();
        update.setUserId(userId);
        update.setGroupId(groupId);
        update.setUpdateTime(new Date());
        return studentMapper.updateTeStudent(update);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 按分组数为班级生成字母小组（A组~F组，分配专属颜色）
     */
    private void createGroups(Long classId, long groupCount)
    {
        for (int i = 1; i <= groupCount; i++)
        {
            TeGroup g = new TeGroup();
            g.setClassId(classId);
            g.setGroupOrder((long) i);
            g.setGroupName(letterOf(i) + "组");
            g.setColorCode(GROUP_COLORS[(i - 1) % GROUP_COLORS.length]);
            g.setCreateTime(new Date());
            groupMapper.insertTeGroup(g);
        }
    }

    /**
     * 自动分组规则：取学号末2位数字，对组数取模；
     * 余 1 → 第1组(A)，余 2 → 第2组(B)…… 余 0 → 最后一组
     */
    private int calcGroupOrder(String studentNo, int groupCount)
    {
        String tail = studentNo.length() > 2 ? studentNo.substring(studentNo.length() - 2) : studentNo;
        int tailNum;
        try
        {
            tailNum = Integer.parseInt(tail.trim());
        }
        catch (NumberFormatException e)
        {
            throw new ServiceException("学号「" + studentNo + "」末2位不是数字，无法自动分组");
        }
        int mod = tailNum % groupCount;
        return mod == 0 ? groupCount : mod;
    }

    /**
     * 组序号转字母（1→A，2→B……）
     */
    private String letterOf(int order)
    {
        return String.valueOf(GROUP_LETTERS.charAt(order - 1));
    }

    /**
     * 学生初始密码规则：学号后6位（不足6位取全学号）
     */
    private String initPassword(String studentNo)
    {
        return studentNo.length() > 6 ? studentNo.substring(studentNo.length() - 6) : studentNo;
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
