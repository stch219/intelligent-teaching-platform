package com.ruoyi.teach.service;

import java.util.List;
import com.ruoyi.teach.domain.TeTask;
import com.ruoyi.teach.domain.TeTaskAssign;

/**
 * ============================================================================
 * 【功能】任务管理业务接口（任务ABCD发布 + 分配到组）
 * ============================================================================
 */
public interface ITeTaskService
{
    /** 查询任务列表（按班级过滤） */
    public List<TeTask> selectTeTaskList(TeTask teTask);

    /** 查询任务详情 */
    public TeTask selectTeTaskById(Long id);

    /** 新增任务（编号自动生成：班内未使用的最早字母 A~H） */
    public int insertTeTask(TeTask teTask, String operateBy);

    /** 修改任务内容 */
    public int updateTeTask(TeTask teTask, String operateBy);

    /** 批量删除任务（已被分配则拒绝） */
    public int deleteTeTaskByIds(Long[] ids);

    /** 分配任务到组（整体覆盖：组内原有其它任务分配被清除，保证一组一任务） */
    public int assignGroups(Long taskId, Long[] groupIds);

    /** 查询班级下分配视图（组→任务） */
    public List<TeTaskAssign> selectAssignList(Long classId);
}
