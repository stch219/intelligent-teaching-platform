package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeChatMemberMapper;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeDashboardMapper;
import com.ruoyi.teach.mapper.TeGroupMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeTeacherMapper;
import com.ruoyi.teach.mapper.TeWarningRecordMapper;
import com.ruoyi.teach.service.ITeDashboardService;

/**
 * ============================================================================
 * 【功能】三端仪表盘统计服务实现（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】纯聚合查询组装（无写操作）：
 *   ① adminStats   全平台规模计数 + AI批改进度 + 最近未处置预警
 *   ② teacherStats 本班学生/小组/总稿提交计数 + 各组完成率明细 +
 *                  成绩分布（四段）+ 未处置预警分布（黄橙红计数）
 *   ③ studentStats 本组模块完成度/任务与截止/贡献率确认/未读消息/
 *                  未处置预警数/已发布成绩
 * ============================================================================
 */
@Service
public class TeDashboardServiceImpl implements ITeDashboardService
{
    /** 课程设计固定 10 个模块 */
    private static final int TOTAL_MODULES = 10;

    @Autowired
    private TeDashboardMapper dashboardMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeGroupMapper groupMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeTeacherMapper teacherMapper;

    @Autowired
    private TeChatMemberMapper chatMemberMapper;

    @Autowired
    private TeWarningRecordMapper warningRecordMapper;

    /**
     * 管理员端：全平台规模与批改进度
     */
    @Override
    public Map<String, Object> adminStats()
    {
        Map<String, Object> m = new HashMap<>();
        int groupCount = dashboardMapper.countGroup();
        int aiReviewed = dashboardMapper.countAiReviewedGroup();
        m.put("classCount", dashboardMapper.countClass());
        m.put("studentCount", dashboardMapper.countStudent());
        m.put("teacherCount", teacherMapper.countTeTeacher());
        m.put("groupCount", groupCount);
        m.put("taskCount", dashboardMapper.countTask());
        m.put("aiReviewedGroup", aiReviewed);
        m.put("aiReviewRate", groupCount == 0 ? 0 : Math.round(aiReviewed * 100.0 / groupCount));
        m.put("publishedScore", dashboardMapper.countPublishedScore());
        m.put("recentWarnings", dashboardMapper.selectRecentWarnings());
        return m;
    }

    /**
     * 教师端：本班教学总览（归属校验，admin 放行）
     */
    @Override
    public Map<String, Object> teacherStats(Long classId, Long operator)
    {
        // 班级归属校验
        TeClass teClass = classMapper.selectTeClassById(classId);
        if (teClass == null)
        {
            throw new ServiceException("班级不存在");
        }
        if (!SecurityUtils.isAdmin(operator) && !operator.equals(teClass.getTeacherId()))
        {
            throw new ServiceException("无权查看其他教师的班级");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("className", teClass.getClassName());
        m.put("studentCount", classMapper.countStudentByClassId(classId));
        int groupCount = groupMapper.countByClassId(classId);
        m.put("groupCount", groupCount);
        int submittedGroup = dashboardMapper.countSubmittedGroup(classId);
        m.put("submittedGroup", submittedGroup);
        m.put("submitRate", groupCount == 0 ? 0 : Math.round(submittedGroup * 100.0 / groupCount));
        // 各组完成率明细（进度条数据源，含任务名/截止时间/AI参考分）
        m.put("groupProgress", dashboardMapper.selectGroupProgressByClass(classId));
        // 成绩分布（已发布成绩四段计数）
        m.put("scoreDist", dashboardMapper.selectScoreDistribution(classId));
        // 未处置预警分布：[{level:1,cnt:n}...] → 转为 level→cnt 映射便于前端取值
        Map<String, Object> warnMap = new HashMap<>();
        for (Map<String, Object> row : warningRecordMapper.countByClassGroupByLevel(classId))
        {
            warnMap.put(String.valueOf(row.get("level")), row.get("cnt"));
        }
        m.put("warningDist", warnMap);
        return m;
    }

    /**
     * 学生端：个人学习与提交状态
     */
    @Override
    public Map<String, Object> studentStats(Long userId)
    {
        Map<String, Object> m = new HashMap<>();
        TeStudent me = studentMapper.selectTeStudentByUserId(userId);
        if (me == null || me.getGroupId() == null)
        {
            // 未入组学生：返回空态标记，前端展示引导
            m.put("inGroup", false);
            return m;
        }
        m.put("inGroup", true);
        m.put("groupId", me.getGroupId());
        m.put("classId", me.getClassId());
        m.put("roleType", me.getRoleType());
        // 本组进度：已提交模块数（10模块制）
        int submitted = dashboardMapper.countSubmittedModule(me.getGroupId());
        m.put("moduleSubmitted", submitted);
        m.put("moduleTotal", TOTAL_MODULES);
        m.put("moduleRate", Math.round(submitted * 100.0 / TOTAL_MODULES));
        // 本组任务名与截止时间（从组进度明细中取本组一行）
        for (Map<String, Object> g : dashboardMapper.selectGroupProgressByClass(me.getClassId()))
        {
            if (((Number) g.get("groupId")).longValue() == me.getGroupId())
            {
                m.put("taskName", g.get("taskName"));
                m.put("deadline", g.get("deadline"));
                m.put("submitStatus", g.get("submitStatus"));
                break;
            }
        }
        // 贡献率确认状态（0未确认 1已确认）
        m.put("contributionConfirmed", dashboardMapper.countMyContributionConfirm(me.getGroupId(), userId) > 0 ? 1 : 0);
        // 未读消息数（复用阶段6消息已读机制）
        m.put("unreadCount", chatMemberMapper.countMyUnread(userId));
        // 本组未处置预警数 + 最近预警列表
        m.put("warningCount", warningRecordMapper.countUnresolvedByGroup(me.getGroupId()));
        m.put("warnings", warningRecordMapper.selectListByGroup(me.getGroupId()));
        // 已发布成绩（未发布为 null，前端提示"教师尚未发布"）
        m.put("finalScore", dashboardMapper.selectMyFinalScore(userId));
        return m;
    }
}
