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
import com.ruoyi.teach.domain.TeGroup;
import com.ruoyi.teach.domain.TeMaterial;
import com.ruoyi.teach.domain.TeModuleContent;
import com.ruoyi.teach.domain.TeModuleScoreSet;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.domain.TeTask;
import com.ruoyi.teach.domain.TeTaskAssign;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeContributionMapper;
import com.ruoyi.teach.mapper.TeGroupMapper;
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
 *         - 总稿引擎（阶段5）：封面（模块1）系统合成；合规预检4项
 *           （组长身份/模块全提交/分工全确认/贡献率分配并全员确认）；
 *           提交总稿 = 封面落库锁定 + 小组置已提交 + 组长分工自认
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

    @Autowired
    private TeGroupMapper groupMapper;

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
        // 封面模块（编号1，阶段5）：本组无内容时由系统实时合成封面（提交总稿时正式落库）
        if (moduleCode == 1L && (content == null || isBlank(content.getContent())))
        {
            row.put("content", buildCoverHtml(student));
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
    // ⑥ 总稿合规预检 + 提交总稿（阶段5 系统引擎）
    // ------------------------------------------------------------------

    /**
     * 总稿合规预检：共4项检查（顺序即前端展示顺序）
     * ①组长身份：仅组长可发起预检与提交
     * ②模块完成：学生可编辑模块（editable=1）必须全部提交锁定
     *   （封面/任务要求/参数配置为只读合成模块，不要求学生提交）
     * ③分工确认：全部组员已填报分工且经组长确认（组长本人提交时自动确认）
     * ④贡献率：模块5-10每模块覆盖全组成员分配，且全员均已确认
     */
    @Override
    public List<Map<String, Object>> submitPrecheck(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，无法进行总稿预检");
        }
        boolean isLeader = student.getRoleType() != null && (student.getRoleType() == 1L || student.getRoleType() == 3L);

        List<Map<String, Object>> result = new ArrayList<>();

        // ① 组长身份检查
        Map<String, Object> check1 = new HashMap<>();
        check1.put("code", "leader");
        check1.put("item", "组长身份");
        check1.put("passed", isLeader);
        check1.put("detail", isLeader ? "你是本组组长，可以发起总稿提交" : "仅组长可提交总稿，请联系组长操作");
        result.add(check1);

        // ② 模块完成检查：可编辑模块全部提交（status=2）
        List<Map<String, Object>> board = contentMapper.selectModuleBoard(student.getClassId(), student.getGroupId());
        List<String> unfinished = new ArrayList<>();
        int editableCount = 0;
        for (Map<String, Object> item : board)
        {
            // 只读合成模块（封面/任务要求/参数配置）无需学生提交，跳过
            if (toLong(item.get("editable")) == null || toLong(item.get("editable")) != 1L)
            {
                continue;
            }
            editableCount++;
            Long status = toLong(item.get("status"));
            if (status == null || status != 2L)
            {
                unfinished.add(String.valueOf(item.get("moduleName")));
            }
        }
        boolean modulesPassed = editableCount > 0 && unfinished.isEmpty();
        Map<String, Object> check2 = new HashMap<>();
        check2.put("code", "modules");
        check2.put("item", "模块完成度");
        check2.put("passed", modulesPassed);
        check2.put("detail", modulesPassed
                ? editableCount + "个可编辑模块已全部提交锁定"
                : "还有未提交的模块：" + String.join("、", unfinished));
        result.add(check2);

        // ③ 分工确认检查：组员全部填报分工并经组长确认
        TeStudent memberQuery = new TeStudent();
        memberQuery.setGroupId(student.getGroupId());
        List<TeStudent> members = studentMapper.selectTeStudentList(memberQuery);
        List<String> dutyTodo = new ArrayList<>();
        for (TeStudent mate : members)
        {
            boolean mateIsLeader = mate.getRoleType() != null && (mate.getRoleType() == 1L || mate.getRoleType() == 3L);
            if (mateIsLeader)
            {
                continue; // 组长本人提交时自动确认分工，不在此拦截
            }
            if (isBlank(mate.getDutyAssignment()) || mate.getDutyStatus() == null || mate.getDutyStatus() != 1L)
            {
                dutyTodo.add(mate.getNickName());
            }
        }
        Map<String, Object> check3 = new HashMap<>();
        check3.put("code", "duty");
        check3.put("item", "组员分工确认");
        check3.put("passed", dutyTodo.isEmpty());
        check3.put("detail", dutyTodo.isEmpty()
                ? "全部组员分工已确认"
                : "以下组员未填报或未确认分工：" + String.join("、", dutyTodo));
        result.add(check3);

        // ④ 贡献率检查：模块5-10每模块覆盖全组成员且全员已确认
        List<TeContribution> rows = contributionMapper.selectByGroup(student.getGroupId());
        // 按模块归集行数，并记录未确认行的成员
        Map<Long, List<TeContribution>> byModule = new LinkedHashMap<>();
        Set<String> unconfirmedNames = new java.util.TreeSet<>();
        for (TeContribution row : rows)
        {
            byModule.computeIfAbsent(row.getModuleCode(), k -> new ArrayList<>()).add(row);
            if (row.getConfirmed() == null || row.getConfirmed() != 1L)
            {
                unconfirmedNames.add(row.getNickName());
            }
        }
        List<String> missingModules = new ArrayList<>();
        for (long code = 5; code <= 10; code++)
        {
            List<TeContribution> moduleRows = byModule.get(code);
            if (moduleRows == null || moduleRows.size() < members.size())
            {
                missingModules.add(String.valueOf(code));
            }
        }
        boolean contribPassed = missingModules.isEmpty() && unconfirmedNames.isEmpty();
        Map<String, Object> check4 = new HashMap<>();
        check4.put("code", "contribution");
        check4.put("item", "贡献率分配与确认");
        check4.put("passed", contribPassed);
        check4.put("detail", contribPassed
                ? "模块5-10已按成员分配贡献率且全员确认"
                : (missingModules.isEmpty() ? "" : "模块" + String.join("、", missingModules) + "未完成全员分配；")
                  + (unconfirmedNames.isEmpty() ? "" : "以下成员未确认贡献率：" + String.join("、", unconfirmedNames)));
        result.add(check4);

        return result;
    }

    /**
     * 提交总稿（事务）：预检全过 → 封面落库锁定 → 小组置已提交 → 组长分工自认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFinal(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，无法提交总稿");
        }
        TeGroup group = groupMapper.selectById(student.getGroupId());
        if (group == null)
        {
            throw new ServiceException("小组数据异常，请联系教师");
        }
        // 幂等拦截：已提交的小组不允许重复提交
        if (group.getSubmitStatus() != null && group.getSubmitStatus() == 1L)
        {
            throw new ServiceException("本组总稿已于 "
                    + (group.getSubmitTime() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(group.getSubmitTime()))
                    + " 提交，不可重复提交");
        }

        // 预检未全过则汇总所有未通过项一次性提示
        StringBuilder failMsg = new StringBuilder();
        for (Map<String, Object> check : submitPrecheck(userId))
        {
            if (!Boolean.TRUE.equals(check.get("passed")))
            {
                if (failMsg.length() > 0)
                {
                    failMsg.append("；");
                }
                failMsg.append(check.get("item")).append("：").append(check.get("detail"));
            }
        }
        if (failMsg.length() > 0)
        {
            throw new ServiceException("总稿提交未通过合规预检——" + failMsg);
        }

        // 封面HTML正式落库写入模块1并提交锁定（无记录则新建，有草稿则乐观锁覆盖）
        String coverHtml = buildCoverHtml(student);
        long words = stripHtml(coverHtml).length();
        String operator = student.getStudentNo();
        TeModuleContent cover = contentMapper.selectByGroupModule(student.getGroupId(), 1L);
        if (cover == null)
        {
            TeModuleContent insert = new TeModuleContent();
            insert.setGroupId(student.getGroupId());
            insert.setModuleCode(1L);
            insert.setContent(coverHtml);
            insert.setStatus(2L);           // 直接提交态
            insert.setProgress(100L);       // 封面系统生成视为完成
            insert.setWordCount(words);
            insert.setItemCount(countLines(stripHtml(coverHtml)));
            insert.setCreateBy(operator);
            insert.setUpdateBy(operator);
            try
            {
                contentMapper.insertTeModuleContent(insert);
            }
            catch (DuplicateKeyException e)
            {
                // 并发下另一组长同时落库 → 转为更新，版本取库内最新
                cover = contentMapper.selectByGroupModule(student.getGroupId(), 1L);
            }
        }
        if (cover != null)
        {
            cover.setContent(coverHtml);
            cover.setStatus(2L);
            cover.setProgress(100L);
            cover.setWordCount(words);
            cover.setItemCount(countLines(stripHtml(coverHtml)));
            cover.setVersion(cover.getVersion() == null ? 0L : cover.getVersion());
            cover.setUpdateBy(operator);
            if (contentMapper.updateWithVersion(cover) == 0)
            {
                throw new ServiceException("封面内容正被其他组员更新，请稍后重试");
            }
        }

        // 小组置为已提交总稿并记录提交时间
        TeGroup update = new TeGroup();
        update.setId(student.getGroupId());
        update.setSubmitStatus(1L);
        update.setSubmitTime(new java.util.Date());
        update.setUpdateTime(new java.util.Date());
        groupMapper.updateTeGroup(update);

        // 组长本人分工自动确认（预检只查组员，组长在此闭环）
        TeStudent leaderUpdate = new TeStudent();
        leaderUpdate.setUserId(userId);
        leaderUpdate.setDutyStatus(1L);
        leaderUpdate.setUpdateTime(new java.util.Date());
        studentMapper.updateTeStudent(leaderUpdate);
    }

    /**
     * ============================================================================
     * 【功能】总稿PDF导出（阶段5 系统引擎）
     * ----------------------------------------------------------------------------
     * 【说明】仅已提交总稿的小组可下载。文档结构：封面页 → 目录页（10模块）→
     *         各模块正文（分页起始）。采用 openhtmltopdf 渲染HTML生成PDF，
     *         运行时从 Windows 字体目录探测中文字体（等线/黑体/仿宋/楷体），
     *         @page 页脚自动输出"第 x 页 / 共 y 页"。
     * ============================================================================
     */
    @Override
    public byte[] exportFinalPdf(Long userId)
    {
        TeStudent student = getStudentChecked(userId);
        if (student.getGroupId() == null)
        {
            throw new ServiceException("你尚未分组，暂无总稿可导出");
        }
        TeGroup group = groupMapper.selectById(student.getGroupId());
        if (group == null || group.getSubmitStatus() == null || group.getSubmitStatus() != 1L)
        {
            throw new ServiceException("本组尚未提交总稿，提交成功后才能导出PDF");
        }

        // 组装10模块正文：优先取落库内容；"任务要求"（模块2）无落库时自动合成
        List<Map<String, Object>> board = contentMapper.selectModuleBoard(student.getClassId(), student.getGroupId());
        StringBuilder bodySb = new StringBuilder();
        int seq = 0;
        for (Map<String, Object> item : board)
        {
            Long code = toLong(item.get("moduleCode"));
            TeModuleContent content = contentMapper.selectByGroupModule(student.getGroupId(), code);
            String html = content == null ? "" : (content.getContent() == null ? "" : content.getContent());
            if (isBlank(html) && code == 2L)
            {
                html = buildTaskRequirementHtml(student.getClassId(), student.getGroupId());
            }
            if (isBlank(html))
            {
                html = "<p style=\"color:#86909c\">（本模块内容暂缺）</p>";
            }
            seq++;
            bodySb.append("<div class=\"module\">")
                  .append("<h1 class=\"module-title\">").append(seq).append(". ")
                  .append(escape(String.valueOf(item.get("moduleName")))).append("</h1>")
                  .append(html)
                  .append("</div>");
        }

        // 目录页：模块序号 + 模块名（页码简化为模块序号展示）
        StringBuilder tocSb = new StringBuilder();
        int tocSeq = 0;
        for (Map<String, Object> item : board)
        {
            tocSeq++;
            tocSb.append("<li><span class=\"toc-name\">").append(escape(String.valueOf(item.get("moduleName"))))
                 .append("</span><span class=\"toc-num\">").append(tocSeq + 2).append("</span></li>"); // 封面+目录占2页
        }

        // 探测中文字体（单文件TTF，避免TTC兼容问题）：等线→黑体→仿宋→楷体
        java.io.File fontFile = findChineseFont();
        if (fontFile == null)
        {
            throw new ServiceException("服务器未找到可用中文字体（Deng/simhei/simfang/simkai.ttf），无法生成PDF");
        }

        // 完整HTML文档（封面复用 buildCoverHtml，保证页面展示与PDF一致）
        StringBuilder doc = new StringBuilder();
        doc.append("<html><head><meta charset=\"UTF-8\"/><style>")
           .append("@page { size: A4; margin: 2.2cm 2cm;")
           .append(" @bottom-center { content: \"第 \" counter(page) \" 页 / 共 \" counter(pages) \" 页\";")
           .append("   font-size: 9px; color: #888; font-family: \"zh-font\"; } }")
           .append("body { font-family: \"zh-font\"; font-size: 12px; color: #333; line-height: 1.7; }")
           .append(".cover-page { page-break-after: always; padding-top: 150px; }")
           .append(".toc-page { page-break-after: always; }")
           .append(".toc-page li { list-style: none; font-size: 13px; padding: 7px 4px; border-bottom: 1px dashed #ddd; overflow: hidden; }")
           .append(".toc-name { float: left; }")
           .append(".toc-num { float: right; }")
           .append(".module { page-break-before: always; }")
           .append(".module-title { font-size: 20px; border-bottom: 2px solid #333; padding-bottom: 8px; margin-bottom: 14px; }")
           .append("table { border-collapse: collapse; } td, th { border: 1px solid #999; padding: 4px 8px; }")
           .append("img { max-width: 100%; }")
           .append("</style></head><body>")
           // 封面页（系统生成）
           .append("<div class=\"cover-page\">").append(buildCoverHtml(student)).append("</div>")
           // 目录页
           .append("<div class=\"toc-page\"><h2 style=\"text-align:center;\">目　录</h2><ul style=\"padding:0;margin:0;\">")
           .append(tocSb)
           .append("</ul></div>")
           // 各模块正文
           .append(bodySb)
           .append("</body></html>");

        // 渲染PDF返回字节流（响应头与写出由控制器处理）
        try
        {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                    new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(fontFile, "zh-font");
            builder.withHtmlContent(doc.toString(), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new ServiceException("总稿PDF生成失败：" + e.getMessage());
        }
    }

    /**
     * 探测Windows中文字体文件（返回第一个存在的单文件TTF，避免TTC集合兼容问题）
     * 探测顺序：等线 Deng.ttf → 黑体 simhei.ttf → 仿宋 simfang.ttf → 楷体 simkai.ttf
     */
    private java.io.File findChineseFont()
    {
        String[] candidates = { "Deng.ttf", "simhei.ttf", "simfang.ttf", "simkai.ttf" };
        for (String name : candidates)
        {
            java.io.File f = new java.io.File("C:\\Windows\\Fonts\\" + name);
            if (f.exists() && f.isFile())
            {
                return f;
            }
        }
        return null;
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
        TeTask task = findGroupTask(classId, groupId);
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
     * 查找本组被分配的任务（任务分配表过滤本组 → 取任务详情）
     */
    private TeTask findGroupTask(Long classId, Long groupId)
    {
        for (TeTaskAssign assign : taskAssignMapper.selectByClassId(classId))
        {
            if (groupId.equals(assign.getGroupId()))
            {
                return taskMapper.selectTeTaskById(assign.getTaskId());
            }
        }
        return null;
    }

    /**
     * ============================================================================
     * 【功能】封面自动合成（阶段5 系统引擎）
     * ----------------------------------------------------------------------------
     * 【说明】按"系统生成封面"规范拼装只读HTML：平台名 + 课程名 + 任务题目 +
     *         班级/小组 + 组成员表（姓名/学号/角色）+ 指导教师 + 日期。
     *         使用内联样式，学生端富文本展示与总稿PDF渲染共用同一份内容。
     * ============================================================================
     */
    private String buildCoverHtml(TeStudent student)
    {
        // 组员列表（封面成员表数据源）
        TeStudent query = new TeStudent();
        query.setGroupId(student.getGroupId());
        List<TeStudent> members = studentMapper.selectTeStudentList(query);

        // 本组任务题目（未分配任务时留空占位）
        TeTask task = student.getClassId() == null ? null : findGroupTask(student.getClassId(), student.getGroupId());
        String taskLine = task == null ? "（教师尚未分配任务）"
                : "任务 " + escape(task.getTaskCode()) + "：" + escape(task.getTaskName());

        // 组成员表行（姓名/学号/组内角色）
        StringBuilder memberRows = new StringBuilder();
        for (TeStudent mate : members)
        {
            memberRows.append("<tr>")
                    .append("<td>").append(escape(mate.getNickName())).append("</td>")
                    .append("<td>").append(escape(mate.getStudentNo())).append("</td>")
                    .append("<td>").append(roleText(mate.getRoleType())).append("</td>")
                    .append("</tr>");
        }

        // 指导教师（班级未分配时留空）
        String teacherName = student.getClassId() == null ? ""
                : studentMapper.selectTeacherNameByClassId(student.getClassId());

        // 拼装封面（内联样式，居中排版）
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"text-align:center;padding:24px 8px;\">")
          .append("<h1 style=\"font-size:30px;letter-spacing:8px;margin:16px 0 8px;\">智能教学平台</h1>")
          .append("<h2 style=\"font-size:22px;margin:8px 0 24px;\">《汽车理论》课程设计报告</h2>")
          .append("<p style=\"font-size:16px;margin:8px 0;\"><b>").append(taskLine).append("</b></p>")
          .append("<p style=\"font-size:14px;color:#4e5969;margin:4px 0;\">班级：")
          .append(escape(student.getClassName() == null ? "" : student.getClassName()))
          .append("　　小组：").append(escape(student.getGroupName() == null ? "" : student.getGroupName()))
          .append("</p>")
          .append("<table style=\"border-collapse:collapse;margin:20px auto;min-width:360px;\">")
          .append("<tr>")
          .append("<th style=\"border:1px solid #dcdfe6;padding:6px 18px;background:#f5f7fa;\">姓名</th>")
          .append("<th style=\"border:1px solid #dcdfe6;padding:6px 18px;background:#f5f7fa;\">学号</th>")
          .append("<th style=\"border:1px solid #dcdfe6;padding:6px 18px;background:#f5f7fa;\">组内角色</th>")
          .append("</tr>")
          .append(memberRows)
          .append("</table>")
          .append("<p style=\"font-size:14px;margin:6px 0;\">指导教师：")
          .append(escape(teacherName == null || teacherName.isEmpty() ? "　" : teacherName))
          .append("</p>")
          .append("<p style=\"font-size:14px;margin:6px 0;\">日期：")
          .append(new java.text.SimpleDateFormat("yyyy年MM月dd日").format(new java.util.Date()))
          .append("</p>")
          .append("</div>");
        return sb.toString();
    }

    /**
     * 组内角色编码转文本（封面成员表展示用）
     */
    private String roleText(Long roleType)
    {
        if (roleType == null)
        {
            return "成员";
        }
        switch (roleType.intValue())
        {
            case 1: return "组长";
            case 2: return "汇报人";
            case 3: return "组长兼汇报人";
            default: return "成员";
        }
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
