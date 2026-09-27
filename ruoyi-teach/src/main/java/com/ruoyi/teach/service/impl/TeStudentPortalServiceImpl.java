package com.ruoyi.teach.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeContribution;
import com.ruoyi.teach.domain.TeMaterial;
import com.ruoyi.teach.domain.TeModuleContent;
import com.ruoyi.teach.domain.TeModuleScoreSet;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.domain.TeTask;
import com.ruoyi.teach.domain.TeTaskAssign;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeContributionMapper;
import com.ruoyi.teach.mapper.TeMaterialMapper;
import com.ruoyi.teach.mapper.TeModuleContentMapper;
import com.ruoyi.teach.mapper.TeModuleScoreSetMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeTaskAssignMapper;
import com.ruoyi.teach.mapper.TeTaskMapper;
import com.ruoyi.teach.service.ITeStudentPortalService;

/**
 * ============================================================================
 * 【功能】学生端门户 Service 实现（阶段4）
 * ----------------------------------------------------------------------------
 * 【说明】核心校验规则：
 *         - 组内角色：组长（1/3）全组唯一；分工文本变化 → 重置待确认
 *         - 模块编辑：editable=0 拦截；已提交（status=2）锁定；
 *           暂存校验上限、提交校验下限；乐观锁版本冲突提示刷新
 *         - 进度折算：字数（条目）÷ 上限（无上限时按下限）× 100，封顶100
 *         - "任务要求"模块（编号2）：本组无内容时自动由任务要素合成只读展示
 *         - 贡献率：仅组长可分配；模块5-10每模块组内合计必须=100%；
 *           重分配后组员需重新确认
 * ============================================================================
 */
@Service
public class TeStudentPortalServiceImpl implements ITeStudentPortalService
{
    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeTaskMapper taskMapper;

    @Autowired
    private TeTaskAssignMapper taskAssignMapper;

    @Autowired
    private TeMaterialMapper materialMapper;

    @Autowired
    private TeModuleContentMapper contentMapper;

    @Autowired
    private TeModuleScoreSetMapper scoreSetMapper;

    @Autowired
    private TeContributionMapper contributionMapper;

    // ------------------------------------------------------------------
    // ① 我的信息 + 组内角色选定
    // ------------------------------------------------------------------

    /**
     * 我的信息聚合：学籍/班级/小组/角色/分工 + 指导教师 + 截止时间
     */
    @Override
    public Map<String, Object> myInfo(Long userId)
    {
        // 学生档案必须存在（注册审批通过后才有）
        TeStudent student = getStudentChecked(userId);
        TeClass teClass = student.getClassId() == null ? null : classMapper.selectTeClassById(student.getClassId());

        Map<String, Object> info = new HashMap<>();
        info.put("studentNo", student.getStudentNo());
        info.put("nickName", student.getNickName());
        info.put("phone", student.getPhonenumber());
        info.put("classId", student.getClassId());
        info.put("className", student.getClassName());
        info.put("groupId", student.getGroupId());
        info.put("groupName", student.getGroupName());
        info.put("roleType", student.getRoleType());
        info.put("dutyAssignment", student.getDutyAssignment());
        info.put("dutyStatus", student.getDutyStatus());
        // 指导教师姓名与班级截止时间（班级可能尚未分配）
        info.put("teacherName", student.getClassId() == null ? "" : studentMapper.selectTeacherNameByClassId(student.getClassId()));
        info.put("deadline", teClass == null ? null : teClass.getDeadline());
        return info;
    }

    /**
     * 选定组内角色并填报分工：组长唯一校验 + 分工变化重置确认状态
     */
    @Override
    public void updateMyRole(Long userId, Long roleType, String dutyAssignment)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，请联系教师完成分组后再选择角色");
        }
        if (roleType == null || roleType < 1 || roleType > 4)
        {
            throw new ServiceException("组内角色不合法（1组长 2汇报人 3组长兼汇报人 4成员）");
        }
        // 组长唯一：组内已有其他组长（角色1或3）则不允许再选
        if (roleType == 1 || roleType == 3)
        {
            TeStudent query = new TeStudent();
            query.setGroupId(student.getGroupId());
            for (TeStudent mate : studentMapper.selectTeStudentList(query))
            {
                if (!mate.getUserId().equals(userId) && (mate.getRoleType() == 1 || mate.getRoleType() == 3))
                {
                    throw new ServiceException("本组已选出组长（" + mate.getNickName() + "），不可重复担任");
                }
            }
        }
        // 分工文本发生变化 → 确认状态重置为"待组长确认"
        TeStudent update = new TeStudent();
        update.setUserId(userId);
        update.setRoleType(roleType);
        update.setDutyAssignment(dutyAssignment);
        String oldDuty = student.getDutyAssignment() == null ? "" : student.getDutyAssignment();
        String newDuty = dutyAssignment == null ? "" : dutyAssignment;
        update.setDutyStatus(oldDuty.equals(newDuty) && student.getDutyStatus() != null && student.getDutyStatus() == 1 ? 1L : 0L);
        update.setUpdateTime(new java.util.Date());
        studentMapper.updateTeStudent(update);
    }

    // ------------------------------------------------------------------
    // ② 公示板
    // ------------------------------------------------------------------

    /**
     * 公示板聚合：本组任务 + 组员分工一览 + 可见资料
     */
    @Override
    public Map<String, Object> board(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        Long classId = student.getClassId();
        Long groupId = student.getGroupId();
        if (classId == null || groupId == null)
        {
            throw new ServiceException("你尚未完成编班分组，公示板暂不可用，请联系教师");
        }

        // 本组被分配的任务（分配表过滤本组 → 逐个取任务详情）
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TeTaskAssign assign : taskAssignMapper.selectByClassId(classId))
        {
            if (groupId.equals(assign.getGroupId()))
            {
                TeTask task = taskMapper.selectTeTaskById(assign.getTaskId());
                if (task != null)
                {
                    Map<String, Object> t = new HashMap<>();
                    t.put("id", task.getId());
                    t.put("taskCode", task.getTaskCode());
                    t.put("taskName", task.getTaskName());
                    t.put("designGoal", task.getDesignGoal());
                    t.put("requirement", task.getRequirement());
                    t.put("gradingStandard", task.getGradingStandard());
                    t.put("tips", task.getTips());
                    t.put("assignTime", assign.getAssignTime());
                    tasks.add(t);
                }
            }
        }

        // 组员分工一览（含角色/分工/确认状态）
        TeStudent memberQuery = new TeStudent();
        memberQuery.setGroupId(groupId);
        List<TeStudent> members = studentMapper.selectTeStudentList(memberQuery);

        // 班级可见资料（参考答案 visible=0 严格保密，学生端不可见）
        TeMaterial materialQuery = new TeMaterial();
        materialQuery.setClassId(classId);
        materialQuery.setVisible(1L);
        List<TeMaterial> materials = materialMapper.selectTeMaterialList(materialQuery);

        Map<String, Object> board = new HashMap<>();
        board.put("info", myInfo(userId));
        board.put("tasks", tasks);
        board.put("members", members);
        board.put("materials", materials);
        return board;
    }

    // ------------------------------------------------------------------
    // ③ 10大模块协同编辑
    // ------------------------------------------------------------------

    /**
     * 模块看板：设置 × 内容状态 × 赋分（10行）
     */
    @Override
    public List<Map<String, Object>> moduleBoard(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        return contentMapper.selectModuleBoard(student.getClassId(), student.getGroupId());
    }

    /**
     * 模块内容详情；"任务要求"模块（编号2）无内容时由任务要素自动合成
     */
    @Override
    public Map<String, Object> moduleDetail(Long userId, Long moduleCode)
    {
        TeStudent student = getStudentChecked(userId);
        List<Map<String, Object>> board = contentMapper.selectModuleBoard(student.getClassId(), student.getGroupId());
        Map<String, Object> row = null;
        for (Map<String, Object> item : board)
        {
            if (moduleCode.equals(((Number) item.get("moduleCode")).longValue()))
            {
                row = item;
                break;
            }
        }
        if (row == null)
        {
            throw new ServiceException("模块编号不合法（1-10）");
        }

        // 本组已有内容（无记录则按未开展状态返回）
        TeModuleContent content = contentMapper.selectByGroupModule(student.getGroupId(), moduleCode);
        row.put("content", content == null ? "" : content.getContent());
        if (content != null)
        {
            row.put("status", content.getStatus());
            row.put("version", content.getVersion());
        }
        else
        {
            row.put("status", 0L);
            row.put("version", 0L);
        }

        // 任务要求模块：本组从未编辑过 → 用任务要素合成初始只读内容
        if (moduleCode == 2L && (content == null || isBlank(content.getContent())))
        {
            row.put("content", buildTaskRequirementHtml(student.getClassId(), student.getGroupId()));
        }
        return row;
    }

    /**
     * 暂存模块内容（乐观锁 + 上限校验 + 进度折算）
     */
    @Override
    public void saveContent(Long userId, TeModuleContent form)
    {
        saveOrSubmit(userId, form, false);
    }

    /**
     * 提交模块内容（下限校验；提交后锁定）
     */
    @Override
    public void submitContent(Long userId, TeModuleContent form)
    {
        saveOrSubmit(userId, form, true);
    }

    /**
     * 暂存/提交共用逻辑：校验 → 计数 → 新建或乐观锁更新
     *
     * @param isSubmit true=提交（校验下限并锁定），false=暂存（校验上限）
     */
    private void saveOrSubmit(Long userId, TeModuleContent form, boolean isSubmit)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，无法编辑模块内容");
        }
        Long moduleCode = form.getModuleCode();
        if (moduleCode == null || moduleCode < 1 || moduleCode > 10)
        {
            throw new ServiceException("模块编号不合法（1-10）");
        }

        // 模块设置校验：只读模块（封面/任务要求/参数配置）不允许学生编辑
        List<Map<String, Object>> board = contentMapper.selectModuleBoard(student.getClassId(), student.getGroupId());
        Map<String, Object> setRow = null;
        for (Map<String, Object> item : board)
        {
            if (moduleCode.equals(((Number) item.get("moduleCode")).longValue()))
            {
                setRow = item;
                break;
            }
        }
        if (setRow == null || ((Number) setRow.get("editable")).longValue() == 0L)
        {
            throw new ServiceException("该模块为只读模块，由系统/教师生成，无需学生编辑");
        }
        if (isSubmit && ((Number) setRow.get("score")) == null)
        {
            throw new ServiceException("教师尚未完成模块赋分，请联系教师设置后再提交");
        }

        // 统计有效字数/条目数（去HTML标签）
        String html = form.getContent() == null ? "" : form.getContent();
        long wordCount = stripHtml(html).length();
        long itemCount = countLines(stripHtml(html));
        Long minCount = toLong(setRow.get("minCount"));
        Long maxCount = toLong(setRow.get("maxCount"));
        boolean byWord = toLong(setRow.get("countType")) == 1L;

        if (isSubmit)
        {
            // 提交必须达到最低要求
            long actual = byWord ? wordCount : itemCount;
            if (minCount != null && minCount > 0 && actual < minCount)
            {
                throw new ServiceException(byWord
                        ? "字数不足：当前" + actual + "字，要求至少" + minCount + "字，无法提交"
                        : "条目不足：当前" + actual + "条，要求至少" + minCount + "条，无法提交");
            }
        }
        else if (maxCount != null && maxCount > 0)
        {
            // 暂存不允许超过上限
            long actual = byWord ? wordCount : itemCount;
            if (actual > maxCount)
            {
                throw new ServiceException(byWord
                        ? "超出最高字数限制：当前" + actual + "字，上限" + maxCount + "字"
                        : "超出最高条目限制：当前" + actual + "条，上限" + maxCount + "条");
            }
        }

        // 进度折算：实际值 ÷ 基准（上限优先，无上限用下限）× 100 封顶
        long base = byWord ? wordCount : itemCount;
        long denom = (maxCount != null && maxCount > 0) ? maxCount : (minCount == null ? 0 : minCount);
        long progress = denom <= 0 ? (base > 0 ? 100 : 0) : Math.min(100L, base * 100 / denom);

        String operator = student.getStudentNo();
        TeModuleContent exist = contentMapper.selectByGroupModule(student.getGroupId(), moduleCode);
        // 首次编辑：初始化内容记录（并发下唯一键冲突则转为更新已有记录）
        if (exist == null)
        {
            TeModuleContent insert = new TeModuleContent();
            insert.setGroupId(student.getGroupId());
            insert.setModuleCode(moduleCode);
            insert.setContent(html);
            insert.setStatus(isSubmit ? 2L : 1L);
            insert.setProgress(progress);
            insert.setWordCount(wordCount);
            insert.setItemCount(itemCount);
            insert.setCreateBy(operator);
            insert.setUpdateBy(operator);
            try
            {
                contentMapper.insertTeModuleContent(insert);
                return;
            }
            catch (DuplicateKeyException e)
            {
                exist = contentMapper.selectByGroupModule(student.getGroupId(), moduleCode);
            }
        }

        // 已提交模块锁定 + 乐观锁版本校验
        if (exist.getStatus() != null && exist.getStatus() == 2L)
        {
            throw new ServiceException("该模块已提交，锁定后不可再编辑");
        }
        long dbVersion = exist.getVersion() == null ? 0L : exist.getVersion();
        if (form.getVersion() == null || form.getVersion().longValue() != dbVersion)
        {
            throw new ServiceException("内容已被组员更新（版本冲突），请刷新页面获取最新内容后再试");
        }

        exist.setContent(html);
        exist.setStatus(isSubmit ? 2L : 1L);
        exist.setProgress(progress);
        exist.setWordCount(wordCount);
        exist.setItemCount(itemCount);
        exist.setVersion(dbVersion);
        exist.setUpdateBy(operator);
        int rows = contentMapper.updateWithVersion(exist);
        if (rows == 0)
        {
            throw new ServiceException("内容已被组员更新（版本冲突），请刷新页面获取最新内容后再试");
        }
    }

    // ------------------------------------------------------------------
    // ④ 贡献率分配与确认
    // ------------------------------------------------------------------

    /**
     * 贡献率面板：组员 + 本组分配行 + 赋分 + 我是否组长
     */
    @Override
    public Map<String, Object> contributionPanel(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，暂无贡献率数据");
        }
        // 组员列表（分配下拉数据源）
        TeStudent query = new TeStudent();
        query.setGroupId(student.getGroupId());
        List<Map<String, Object>> members = new ArrayList<>();
        for (TeStudent mate : studentMapper.selectTeStudentList(query))
        {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", mate.getUserId());
            m.put("nickName", mate.getNickName());
            m.put("studentNo", mate.getStudentNo());
            members.add(m);
        }
        // 赋分（模块5-10满分展示）
        List<Map<String, Object>> scores = new ArrayList<>();
        for (TeModuleScoreSet score : scoreSetMapper.selectByClassId(student.getClassId()))
        {
            Map<String, Object> s = new HashMap<>();
            s.put("moduleCode", score.getModuleCode());
            s.put("score", score.getScore());
            scores.add(s);
        }
        // 我是否组长（角色1组长 3组长兼汇报人）
        boolean isLeader = student.getRoleType() != null && (student.getRoleType() == 1L || student.getRoleType() == 3L);

        Map<String, Object> panel = new HashMap<>();
        panel.put("isLeader", isLeader);
        panel.put("members", members);
        panel.put("scores", scores);
        panel.put("rows", contributionMapper.selectByGroup(student.getGroupId()));
        return panel;
    }

    /**
     * 组长保存贡献率分配：模块5-10每模块合计必须100%，重分配重置确认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveContribution(Long userId, List<TeContribution> list)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，无法分配贡献率");
        }
        boolean isLeader = student.getRoleType() != null && (student.getRoleType() == 1L || student.getRoleType() == 3L);
        if (!isLeader)
        {
            throw new ServiceException("仅组长可分配贡献率，如需调整请联系组长");
        }

        // 组员用户ID白名单（防止把贡献率分给组外人）
        TeStudent query = new TeStudent();
        query.setGroupId(student.getGroupId());
        Set<Long> memberIds = new HashSet<>();
        for (TeStudent mate : studentMapper.selectTeStudentList(query))
        {
            memberIds.add(mate.getUserId());
        }

        // 按模块分组校验：编号5-10、成员合法、每模块合计=100
        Map<Long, List<TeContribution>> byModule = new LinkedHashMap<>();
        for (TeContribution row : list)
        {
            Long code = row.getModuleCode();
            if (code == null || code < 5 || code > 10)
            {
                throw new ServiceException("贡献率仅针对模块5-10，模块编号" + code + "不合法");
            }
            if (row.getUserId() == null || !memberIds.contains(row.getUserId()))
            {
                throw new ServiceException("贡献率分配对象必须为本组成员");
            }
            BigDecimal ratio = row.getRatio() == null ? BigDecimal.ZERO : row.getRatio();
            if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(new BigDecimal("100")) > 0)
            {
                throw new ServiceException("贡献率须在0-100之间");
            }
            byModule.computeIfAbsent(code, k -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<Long, List<TeContribution>> entry : byModule.entrySet())
        {
            BigDecimal sum = BigDecimal.ZERO;
            for (TeContribution row : entry.getValue())
            {
                sum = sum.add(row.getRatio() == null ? BigDecimal.ZERO : row.getRatio());
            }
            if (sum.compareTo(new BigDecimal("100")) != 0)
            {
                throw new ServiceException("模块" + entry.getKey() + "的组内贡献率合计必须为100%，当前为"
                        + sum.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
            }
        }

        // 整体覆盖：先删该模块旧行（重置确认），再批量插入新行
        for (Map.Entry<Long, List<TeContribution>> entry : byModule.entrySet())
        {
            for (TeContribution row : entry.getValue())
            {
                row.setGroupId(student.getGroupId());
            }
            contributionMapper.deleteByGroupModule(student.getGroupId(), entry.getKey());
            contributionMapper.batchInsert(entry.getValue());
        }
    }

    /**
     * 组员确认贡献率（一键确认本组本人全部行）
     */
    @Override
    public void confirmContribution(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，暂无贡献率可确认");
        }
        int rows = contributionMapper.confirmByGroupUser(student.getGroupId(), userId);
        if (rows == 0)
        {
            throw new ServiceException("组长尚未分配贡献率，暂无可确认的数据");
        }
    }

    // ------------------------------------------------------------------
    // ⑤ 组长分工确认
    // ------------------------------------------------------------------

    /**
     * 组长确认组员分工：校验同组 + 已填报分工 → duty_status 置1
     */
    @Override
    public void confirmDuty(Long leaderUserId, Long memberUserId)
    {
        TeStudent leader = getStudentChecked(leaderUserId);
        boolean isLeader = leader.getRoleType() != null && (leader.getRoleType() == 1L || leader.getRoleType() == 3L);
        if (!isLeader)
        {
            throw new ServiceException("仅组长可确认组员分工");
        }
        TeStudent member = studentMapper.selectTeStudentByUserId(memberUserId);
        if (member == null || !leader.getGroupId().equals(member.getGroupId()))
        {
            throw new ServiceException("该成员不在你的小组内，无法确认");
        }
        if (member.getDutyAssignment() == null || member.getDutyAssignment().trim().isEmpty())
        {
            throw new ServiceException("该组员尚未填报分工说明，无法确认");
        }
        // 仅更新确认状态字段
        TeStudent update = new TeStudent();
        update.setUserId(memberUserId);
        update.setDutyStatus(1L);
        update.setUpdateTime(new java.util.Date());
        studentMapper.updateTeStudent(update);
    }

    // ------------------------------------------------------------------
    // 私有工具方法
    // ------------------------------------------------------------------

    /**
     * 获取当前学生档案（不存在直接抛错）
     */
    private TeStudent getStudentChecked(Long userId)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student == null)
        {
            throw new ServiceException("当前账号没有学生档案，请联系管理员");
        }
        return student;
    }

    /**
     * 去除HTML标签与常见实体，得到纯文本（用于字数/条目统计）
     */
    private String stripHtml(String html)
    {
        return html == null ? ""
                : html.replaceAll("(?s)<[^>]*>", "")
                      .replace("&nbsp;", " ")
                      .replace("&lt;", "<")
                      .replace("&gt;", ">")
                      .replace("&amp;", "&")
                      .trim();
    }

    /**
     * 统计非空行数（条目数校验：每行视为一条）
     */
    private long countLines(String text)
    {
        if (text == null || text.trim().isEmpty())
        {
            return 0;
        }
        long count = 0;
        for (String line : text.split("\\r?\\n"))
        {
            if (!line.trim().isEmpty())
            {
                count++;
            }
        }
        return count;
    }

    /**
     * "任务要求"模块初始内容合成（任务题目/目标/要求/评分标准/提示）
     */
    private String buildTaskRequirementHtml(Long classId, Long groupId)
    {
        // 找到本组被分配的任务
        TeTask task = null;
        for (TeTaskAssign assign : taskAssignMapper.selectByClassId(classId))
        {
            if (groupId.equals(assign.getGroupId()))
            {
                task = taskMapper.selectTeTaskById(assign.getTaskId());
                break;
            }
        }
        if (task == null)
        {
            return "<p style=\"color:#86909c\">教师尚未给本组分配任务，请联系教师。</p>";
        }
        // 用任务要素拼装只读HTML（各要素空值跳过）
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>任务 ").append(escape(task.getTaskCode())).append("：")
          .append(escape(task.getTaskName())).append("</h2>");
        appendSection(sb, "设计目标", task.getDesignGoal());
        appendSection(sb, "具体要求", task.getRequirement());
        appendSection(sb, "评分标准", task.getGradingStandard());
        appendSection(sb, "重要提示", task.getTips());
        return sb.toString();
    }

    /**
     * 追加"标题+正文段落"小节
     */
    private void appendSection(StringBuilder sb, String title, String text)
    {
        if (text != null && !text.trim().isEmpty())
        {
            sb.append("<h3>").append(title).append("</h3><p>")
              .append(escape(text).replace("\r\n", "<br/>").replace("\n", "<br/>"))
              .append("</p>");
        }
    }

    /**
     * HTML转义（防任务文本中的特殊字符破坏富文本结构）
     */
    private String escape(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Object → Long 安全转换（看板Map里的数值可能为Integer/Long/BigDecimal）
     */
    private Long toLong(Object value)
    {
        return value == null ? null : ((Number) value).longValue();
    }

    /**
     * 空白判断
     */
    private boolean isBlank(String text)
    {
        return text == null || text.trim().isEmpty();
    }
}
