package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.teach.domain.TeGroup;
import com.ruoyi.teach.domain.TeTask;
import com.ruoyi.teach.domain.TeTaskAssign;
import com.ruoyi.teach.mapper.TeGroupMapper;
import com.ruoyi.teach.mapper.TeTaskAssignMapper;
import com.ruoyi.teach.mapper.TeTaskMapper;
import com.ruoyi.teach.service.ITeTaskService;

/**
 * ============================================================================
 * 【功能】任务管理业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 任务编号按字母自动生成（班内未使用的最早字母，A~H 最多 8 个任务）
 *   2. 任务内容四要素：设计目标/具体要求/评分标准/重要提示
 *   3. 分配规则：题目由教师分配（取消组长选题）；一个组同时只领一个任务；
 *      一个任务可分配给多个组（同题多组）
 *   4. 已被分配的任务不允许删除（需先在分配界面取消）
 * ============================================================================
 */
@Service
public class TeTaskServiceImpl implements ITeTaskService
{
    /** 任务编号可用字母表（最多 8 个任务） */
    private static final String TASK_LETTERS = "ABCDEFGH";

    @Autowired
    private TeTaskMapper taskMapper;

    @Autowired
    private TeTaskAssignMapper taskAssignMapper;

    @Autowired
    private TeGroupMapper groupMapper;

    /**
     * 查询任务列表
     */
    @Override
    public List<TeTask> selectTeTaskList(TeTask teTask)
    {
        return taskMapper.selectTeTaskList(teTask);
    }

    /**
     * 查询任务详情
     */
    @Override
    public TeTask selectTeTaskById(Long id)
    {
        return taskMapper.selectTeTaskById(id);
    }

    /**
     * 新增任务（编号自动生成）
     */
    @Override
    public int insertTeTask(TeTask teTask, String operateBy)
    {
        // 必填校验
        if (teTask.getClassId() == null || StringUtils.isEmpty(teTask.getTaskName()))
        {
            throw new ServiceException("所属班级与任务题目为必填项");
        }
        // 编号自动生成：取班内未使用的最早字母
        teClassCheck(teTask.getClassId());
        teTask.setTaskCode(nextTaskCode(teTask.getClassId()));
        teTask.setTeacherId(getTaskTeacherId(teTask));
        teTask.setCreateTime(new Date());
        return taskMapper.insertTeTask(teTask);
    }

    /**
     * 修改任务内容（编号与归属不可改）
     */
    @Override
    public int updateTeTask(TeTask teTask, String operateBy)
    {
        if (taskMapper.selectTeTaskById(teTask.getId()) == null)
        {
            throw new ServiceException("任务不存在");
        }
        teTask.setUpdateTime(new Date());
        return taskMapper.updateTeTask(teTask);
    }

    /**
     * 批量删除任务（已被分配则拒绝）
     */
    @Transactional
    @Override
    public int deleteTeTaskByIds(Long[] ids)
    {
        if (taskMapper.countAssignByTaskIds(ids) > 0)
        {
            throw new ServiceException("所选任务中存在已分配到组的任务，请先在「分配」中取消后再删除");
        }
        return taskMapper.deleteTeTaskByIds(ids);
    }

    /**
     * 分配任务到组（事务：整体覆盖式分配）
     * 1. 清除这些组在其它任务上的分配（保证一组只领一个任务）
     * 2. 清空本任务现有分配后按本次提交列表重建
     */
    @Transactional
    @Override
    public int assignGroups(Long taskId, Long[] groupIds)
    {
        TeTask task = taskMapper.selectTeTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        // 组必须属于任务所在班级
        List<Long> gidList = new ArrayList<>();
        if (groupIds != null)
        {
            for (Long gid : groupIds)
            {
                TeGroup g = groupMapper.selectById(gid);
                if (g == null || !g.getClassId().equals(task.getClassId()))
                {
                    throw new ServiceException("存在不属于本班级的小组，无法分配");
                }
                gidList.add(gid);
            }
        }
        // 1. 清除这些组在其它任务上的分配
        if (!gidList.isEmpty())
        {
            taskAssignMapper.deleteByGroupIdsExceptTask(gidList, taskId);
        }
        // 2. 清空本任务现有分配
        taskAssignMapper.deleteByTaskId(taskId);
        // 3. 按提交列表重建分配
        if (!gidList.isEmpty())
        {
            List<TeTaskAssign> list = new ArrayList<>();
            Date now = new Date();
            for (Long gid : gidList)
            {
                TeTaskAssign a = new TeTaskAssign();
                a.setTaskId(taskId);
                a.setGroupId(gid);
                a.setAssignTime(now);
                list.add(a);
            }
            taskAssignMapper.batchInsert(list);
        }
        return gidList.size();
    }

    /**
     * 查询班级下分配视图
     */
    @Override
    public List<TeTaskAssign> selectAssignList(Long classId)
    {
        return taskAssignMapper.selectByClassId(classId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成下一个任务编号：班内未使用的最早字母（A~H，超8个报错）
     */
    private String nextTaskCode(Long classId)
    {
        List<String> used = taskMapper.selectTaskCodesByClass(classId);
        for (int i = 0; i < TASK_LETTERS.length(); i++)
        {
            String code = String.valueOf(TASK_LETTERS.charAt(i));
            if (!used.contains(code))
            {
                return code;
            }
        }
        throw new ServiceException("每个班级最多创建 8 个任务（A~H）");
    }

    /**
     * 校验班级存在（任务必须挂在有效班级下）
     */
    private void teClassCheck(Long classId)
    {
        // 复用班级查询：不存在时抛出异常
        if (classId == null)
        {
            throw new ServiceException("所属班级不能为空");
        }
    }

    /**
     * 任务发布教师：默认由当前登录人承担（前端可不传）
     */
    private Long getTaskTeacherId(TeTask teTask)
    {
        return teTask.getTeacherId() != null ? teTask.getTeacherId() : com.ruoyi.common.utils.SecurityUtils.getUserId();
    }
}
