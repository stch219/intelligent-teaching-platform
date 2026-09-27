package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.domain.TeWarningRecord;
import com.ruoyi.teach.domain.TeWarningRule;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeDashboardMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeWarningRecordMapper;
import com.ruoyi.teach.mapper.TeWarningRuleMapper;
import com.ruoyi.teach.service.ITeWarningScanService;

/**
 * ============================================================================
 * 【功能】三级预警扫描服务实现（阶段8核心引擎）
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 判定基准：组级完成率 = 已提交模块数/10×100（总稿已提交恒为100）；
 *      截止时间取分配到该组任务的 deadline
 *   2. 规则命中：距截止 ≤ 规则.daysBefore 天 且 完成率 < 规则.progressThreshold
 *      → 触发规则.level 级预警（每班最多3条规则对应黄/橙/红）
 *   3. 硬规则：截止时间已过且总稿未提交 → 强制红色（3级），无需配置规则
 *   4. 同组同时命中多级只发最高级（红>橙>黄），避免重复轰炸
 *   5. 幂等落库：同组同级别已有未处置记录时仅刷新内容/完成率/时间，不重复插入
 *   6. 本实现只负责判定与落库，返回触发明细（含组员ID列表），
 *      WebSocket 实时推送由 admin 模块的 Controller 层完成（依赖方向：admin→teach）
 * ============================================================================
 */
@Service
public class TeWarningScanServiceImpl implements ITeWarningScanService
{
    /** 课程设计固定 10 个模块（完成率分母） */
    private static final int TOTAL_MODULES = 10;

    /** 一天的毫秒数（窗口判定用） */
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    @Autowired
    private TeWarningRuleMapper ruleMapper;

    @Autowired
    private TeWarningRecordMapper recordMapper;

    @Autowired
    private TeDashboardMapper dashboardMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    /**
     * 扫描班级：规则判定 → 幂等落库 → 返回触发明细
     */
    @Transactional
    @Override
    public Map<String, Object> scanClass(Long classId, Long operator)
    {
        // 1. 班级归属校验（教师仅能扫描自己指导的班级，admin 放行）
        checkClassOwner(classId, operator);

        // 2. 取启用中的规则与本班全部小组进度
        List<TeWarningRule> rules = new ArrayList<>();
        for (TeWarningRule r : ruleMapper.selectByClassId(classId))
        {
            if (r.getEnabled() != null && r.getEnabled() == 1L)
            {
                rules.add(r);
            }
        }
        List<Map<String, Object>> groups = dashboardMapper.selectGroupProgressByClass(classId);

        Date now = new Date();
        int newCount = 0;
        int refreshCount = 0;
        List<Map<String, Object>> triggered = new ArrayList<>();

        // 3. 逐组判定
        for (Map<String, Object> g : groups)
        {
            Long groupId = ((Number) g.get("groupId")).longValue();
            String groupName = String.valueOf(g.get("groupName"));
            int submitStatus = g.get("submitStatus") == null ? 0 : ((Number) g.get("submitStatus")).intValue();
            int submitted = g.get("submitted") == null ? 0 : ((Number) g.get("submitted")).intValue();
            int progress = submitStatus == 1 ? 100 : submitted * (100 / TOTAL_MODULES);
            // DATETIME 列在 resultType=map 时可能返回 LocalDateTime（MyBatis 3.5+ 默认）或 Timestamp，双类型兼容
            Date deadline = null;
            Object dObj = g.get("deadline");
            if (dObj instanceof java.sql.Timestamp)
            {
                deadline = new Date(((java.sql.Timestamp) dObj).getTime());
            }
            else if (dObj instanceof java.time.LocalDateTime)
            {
                deadline = new Date(((java.time.LocalDateTime) dObj)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            String taskName = g.get("taskName") == null ? "" : String.valueOf(g.get("taskName"));

            // 4. 计算命中级别（取最高级）：先看规则，再看硬红
            int hitLevel = 0;
            String reason = null;
            if (deadline != null)
            {
                long remain = deadline.getTime() - now.getTime();
                for (TeWarningRule r : rules)
                {
                    boolean inWindow = remain <= r.getDaysBefore() * DAY_MILLIS;   // 已进入提前量窗口（含已过期）
                    boolean belowBar = progress < r.getProgressThreshold();        // 完成率低于阈值
                    if (inWindow && belowBar && r.getLevel() > hitLevel)
                    {
                        hitLevel = r.getLevel().intValue();
                        long days = remain > 0 ? (remain + DAY_MILLIS - 1) / DAY_MILLIS : 0;
                        reason = "距截止还有 " + days + " 天，当前完成率 " + progress + "%（"
                                + submitted + "/" + TOTAL_MODULES + " 模块），低于阈值 " + r.getProgressThreshold() + "%";
                    }
                }
                // 硬规则：已过截止且总稿未提交 → 强制红色（覆盖规则命中）
                if (remain < 0 && submitStatus != 1)
                {
                    hitLevel = 3;
                    reason = "已于 " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(deadline)
                            + " 截止仍未提交总稿，请立即处理";
                }
            }

            // 5. 命中则幂等落库并记录触发明细
            if (hitLevel > 0)
            {
                String content = "「" + groupName + "」"
                        + (taskName.isEmpty() ? "" : "任务「" + taskName + "」") + reason;
                TeWarningRecord exist = recordMapper.selectUnresolved(groupId, (long) hitLevel);
                Date now2 = new Date();
                if (exist != null)
                {
                    // 已有同级别未处置记录：仅刷新内容/完成率/时间
                    TeWarningRecord refresh = new TeWarningRecord();
                    refresh.setGroupId(groupId);
                    refresh.setLevel((long) hitLevel);
                    refresh.setContent(content);
                    refresh.setProgress(progress);
                    refresh.setSendTime(now2);
                    recordMapper.refreshRecord(refresh);
                    refreshCount++;
                }
                else
                {
                    TeWarningRecord rec = new TeWarningRecord();
                    rec.setClassId(classId);
                    rec.setGroupId(groupId);
                    rec.setLevel((long) hitLevel);
                    rec.setContent(content);
                    rec.setProgress(progress);
                    rec.setSendTime(now2);
                    recordMapper.insertRecord(rec);
                    newCount++;
                }
                // 组装触发明细（含组员ID列表，供 Controller 推送）
                Map<String, Object> item = new HashMap<>();
                item.put("groupId", groupId);
                item.put("groupName", groupName);
                item.put("level", hitLevel);
                item.put("content", content);
                item.put("progress", progress);
                item.put("memberIds", dashboardMapper.selectStudentUserIdsByGroup(groupId));
                triggered.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupTotal", groups.size());
        result.put("newCount", newCount);
        result.put("refreshCount", refreshCount);
        result.put("triggered", triggered);
        return result;
    }

    /**
     * 班级预警记录列表（教师，归属校验）
     */
    @Override
    public List<TeWarningRecord> listRecords(Long classId, Long operator)
    {
        checkClassOwner(classId, operator);
        return recordMapper.selectListByClass(classId);
    }

    /**
     * 标记处置（教师，记录须属于自己班级）
     */
    @Override
    public int resolve(Long id, Long operator)
    {
        // 简校验：教师只能处置自己班级的记录（admin 放行）
        if (!SecurityUtils.isAdmin(operator))
        {
            TeWarningRecord rec = new TeWarningRecord();
            for (TeWarningRecord r : ownClassRecords(operator))
            {
                if (r.getId().equals(id))
                {
                    rec = r;
                    break;
                }
            }
            if (rec.getId() == null)
            {
                throw new ServiceException("预警记录不存在或不属于您指导的班级");
            }
        }
        return recordMapper.resolve(id);
    }

    /**
     * 学生查询本组预警
     */
    @Override
    public List<TeWarningRecord> myWarnings(Long userId)
    {
        TeStudent me = studentMapper.selectTeStudentByUserId(userId);
        if (me == null || me.getGroupId() == null)
        {
            return new ArrayList<>();
        }
        return recordMapper.selectListByGroup(me.getGroupId());
    }

    /**
     * 教师自己全部班级的记录（resolve 归属校验辅助）
     */
    private List<TeWarningRecord> ownClassRecords(Long teacherId)
    {
        List<TeWarningRecord> all = new ArrayList<>();
        for (TeClass c : classMapper.selectTeClassList(new TeClass()))
        {
            if (teacherId.equals(c.getTeacherId()))
            {
                all.addAll(recordMapper.selectListByClass(c.getId()));
            }
        }
        return all;
    }

    /**
     * 班级归属校验：当前操作人须为该班指导教师（admin 超管放行）
     */
    private void checkClassOwner(Long classId, Long operator)
    {
        TeClass teClass = classMapper.selectTeClassById(classId);
        if (teClass == null)
        {
            throw new ServiceException("班级不存在");
        }
        if (!SecurityUtils.isAdmin(operator) && !operator.equals(teClass.getTeacherId()))
        {
            throw new ServiceException("无权操作其他教师的班级");
        }
    }
}
