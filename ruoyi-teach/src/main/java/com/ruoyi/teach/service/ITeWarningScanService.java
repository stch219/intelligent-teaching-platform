package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeWarningRecord;

/**
 * ============================================================================
 * 【功能】三级预警扫描服务接口（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】教师触发「立即扫描」：按班级启用的预警规则逐组判定完成率，
 *         命中则落库 te_warning_record（同组同级别幂等），并返回触发明细
 *         交由 Controller 层做 WebSocket 实时推送（teach 模块不依赖
 *         admin 的会话池，保持依赖方向单一）。
 *         级别语义：1一般-黄 2重要-橙 3紧急-红；同组同时命中多级只发最高级。
 *         硬规则：任务截止已过且总稿未提交 → 强制红色，无需配置规则。
 * ============================================================================
 */
public interface ITeWarningScanService
{
    /**
     * 扫描指定班级的全部小组，命中规则则落库预警记录
     *
     * @param classId  班级ID
     * @param operator 操作人用户ID（教师归属校验用，admin 放行）
     * @return 触发统计：{groupTotal, newCount, refreshCount, triggered:[{groupId,groupName,level,content,progress,memberIds}]}
     */
    Map<String, Object> scanClass(Long classId, Long operator);

    /**
     * 教师查询班级预警记录列表（未处置在前、级别高在前）
     */
    List<TeWarningRecord> listRecords(Long classId, Long operator);

    /**
     * 教师标记预警记录为已处置/忽略
     */
    int resolve(Long id, Long operator);

    /**
     * 学生查询本组预警记录列表
     */
    List<TeWarningRecord> myWarnings(Long userId);
}
