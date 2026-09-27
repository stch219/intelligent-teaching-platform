package com.ruoyi.teach.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeWarningRecord;

/**
 * ============================================================================
 * 【功能】进度预警记录 Mapper（表 te_warning_record）
 * ----------------------------------------------------------------------------
 * 【说明】供预警扫描引擎与师生端页面使用：
 *         教师触发扫描 → 引擎查重/插入/刷新 → 师生查询 → 教师处置。
 * ============================================================================
 */
public interface TeWarningRecordMapper
{
    /**
     * 查询某组某级别的未处置记录（扫描幂等查重用）
     */
    TeWarningRecord selectUnresolved(@Param("groupId") Long groupId, @Param("level") Long level);

    /**
     * 新增预警记录
     */
    int insertRecord(TeWarningRecord record);

    /**
     * 刷新已有未处置记录的发送时间与内容/完成率（重复扫描幂等）
     */
    int refreshRecord(TeWarningRecord record);

    /**
     * 按班级查询预警记录列表（联查组名/班级名/任务名，教师端展示）
     */
    List<TeWarningRecord> selectListByClass(Long classId);

    /**
     * 按小组查询预警记录列表（学生端查看本组预警）
     */
    List<TeWarningRecord> selectListByGroup(Long groupId);

    /**
     * 标记记录为已处置/忽略
     */
    int resolve(Long id);

    /**
     * 按班级统计各级别未处置记录数（教师仪表盘分布图）
     * 返回 [{level: 1, cnt: 2}, ...]
     */
    List<Map<String, Object>> countByClassGroupByLevel(Long classId);

    /**
     * 按小组统计未处置预警数（学生仪表盘提示）
     */
    int countUnresolvedByGroup(Long groupId);
}
