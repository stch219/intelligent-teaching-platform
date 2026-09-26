package com.ruoyi.teach.mapper;

import java.util.List;
import com.ruoyi.teach.domain.TeTask;

/**
 * ============================================================================
 * 【功能】任务管理 Mapper 接口（表 te_task，联查教师姓名与已分配组数）
 * ============================================================================
 */
public interface TeTaskMapper
{
    /** 查询任务列表（按班级过滤） */
    public List<TeTask> selectTeTaskList(TeTask teTask);

    /** 按ID查询任务详情 */
    public TeTask selectTeTaskById(Long id);

    /** 查询班级下已使用的任务编号集合（自动生成下一个编号用） */
    public List<String> selectTaskCodesByClass(Long classId);

    /** 新增任务 */
    public int insertTeTask(TeTask teTask);

    /** 修改任务 */
    public int updateTeTask(TeTask teTask);

    /** 批量删除任务 */
    public int deleteTeTaskByIds(Long[] ids);

    /** 删除班级下全部任务（删班级级联） */
    public int deleteByClassId(Long classId);

    /** 统计任务集合已被分配的记录数（删除前校验） */
    public int countAssignByTaskIds(Long[] ids);
}
