package com.ruoyi.teach.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeTaskAssign;

/**
 * ============================================================================
 * 【功能】任务分配 Mapper 接口（表 te_task_assign，联查任务/小组信息）
 * ============================================================================
 */
public interface TeTaskAssignMapper
{
    /** 批量新增分配记录 */
    public int batchInsert(List<TeTaskAssign> list);

    /** 删除某任务的全部分配（重新分配时先清后插） */
    public int deleteByTaskId(Long taskId);

    /** 删除小组集合在其它任务上的分配（保证一组只领一个任务） */
    public int deleteByGroupIdsExceptTask(@Param("groupIds") List<Long> groupIds, @Param("taskId") Long taskId);

    /** 查询班级下全部分配视图（组→任务） */
    public List<TeTaskAssign> selectByClassId(Long classId);

    /** 统计班级下分配记录数（调整分组数前校验） */
    public int countByClassId(Long classId);

    /** 删除班级下全部分配记录（删班级级联） */
    public int deleteByClassId(Long classId);
}
