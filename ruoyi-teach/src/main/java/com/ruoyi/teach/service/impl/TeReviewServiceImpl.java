package com.ruoyi.teach.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.teach.domain.TeAiReview;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeContribution;
import com.ruoyi.teach.domain.TeGroup;
import com.ruoyi.teach.domain.TeModuleContent;
import com.ruoyi.teach.domain.TeModuleScoreSet;
import com.ruoyi.teach.domain.TeScore;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.domain.TeTaskAssign;
import com.ruoyi.teach.mapper.TeAiReviewMapper;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeContributionMapper;
import com.ruoyi.teach.mapper.TeGroupMapper;
import com.ruoyi.teach.mapper.TeModuleContentMapper;
import com.ruoyi.teach.mapper.TeModuleScoreSetMapper;
import com.ruoyi.teach.mapper.TeScoreMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.mapper.TeTaskAssignMapper;
import com.ruoyi.teach.service.ITeReviewService;

/**
 * ============================================================================
 * 【功能】AI批改与成绩判分 Service 实现（阶段7）
 * ----------------------------------------------------------------------------
 * 【批改链路】组装模块内容 → 健康检查 Python 推理服务 → 多模态大模型批改 →
 *             服务不可用/解析失败时本地规则模拟兜底 → 落库 te_ai_review →
 *             回写小组AI参考分（Σ 模块满分×AI分/100）
 * 【判分公式】小组最终分 = Σ(各模块生效分)（生效分=教师核定分，缺省用AI参考分）
 *             个人最终得分 = Σ(模块生效分 × 个人该模块贡献率)
 * 【规则模拟】按字数/结构/代码块/图片/条目五维度启发式打分，保证任何机器可用
 * ============================================================================
 */
@Service
public class TeReviewServiceImpl implements ITeReviewService
{
    /** 计贡献率模块范围（5~10），前4模块不批改不赋分 */
    private static final long MODULE_MIN = 5;

    private static final long MODULE_MAX = 10;

    /** 模块编号 → 模块名称（与 TeModuleSetServiceImpl 保持一致） */
    private static final String[] MODULE_NAMES = {
            "", "", "", "", "", "知识背景", "计算步骤", "代码实现", "计算结果与分析", "心得体会", "参考资料"
    };

    /** 富文本中提取 base64 内嵌图片的正则（Quill 编辑器图片为 dataURL） */
    private static final Pattern IMG_PATTERN =
            Pattern.compile("<img[^>]+src=[\"']data:image/[^;]+;base64,([A-Za-z0-9+/=]+)[\"']", Pattern.CASE_INSENSITIVE);

    /** HTML 标签清理正则（去标签得纯文本） */
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    @Autowired
    private TeAiReviewMapper aiReviewMapper;

    @Autowired
    private TeScoreMapper scoreMapper;

    @Autowired
    private TeGroupMapper groupMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeModuleContentMapper moduleContentMapper;

    @Autowired
    private TeModuleScoreSetMapper moduleScoreSetMapper;

    @Autowired
    private TeContributionMapper contributionMapper;

    /** 任务分配 Mapper（总览行联出任务编号/名称） */
    @Autowired
    private TeTaskAssignMapper taskAssignMapper;

    /** Python 推理服务地址（application.yml ai.engine.base-url） */
    @Value("${ai.engine.base-url}")
    private String engineBaseUrl;

    /** 健康检查超时（毫秒） */
    @Value("${ai.engine.health-timeout}")
    private long healthTimeout;

    /** 批改请求超时（毫秒，CPU 推理较慢需放宽） */
    @Value("${ai.engine.grade-timeout}")
    private long gradeTimeout;

    // ------------------------------------------------------------------
    // 一、触发 AI 批改
    // ------------------------------------------------------------------

    /**
     * 触发 AI 批改：校验 → 组装 → 调模型（失败走规则）→ 落库 → 回写参考分
     */
    @Override
    @Transactional
    public String reviewGroup(Long groupId, Long operatorUserId)
    {
        // 1. 权限与业务前置校验（班级归属 + 总稿已提交 + 赋分已配置）
        TeGroup group = checkGroupOwner(groupId, operatorUserId);
        if (group.getSubmitStatus() == null || group.getSubmitStatus() != 1)
        {
            throw new ServiceException("该小组尚未提交总稿，不能发起 AI 批改");
        }
        List<TeModuleScoreSet> scoreSets = moduleScoreSetMapper.selectByClassId(group.getClassId());
        if (scoreSets == null || scoreSets.isEmpty())
        {
            throw new ServiceException("请先完成模块赋分设置（总分=100），再发起 AI 批改");
        }
        Map<Long, BigDecimal> maxMap = new HashMap<>();
        for (TeModuleScoreSet s : scoreSets)
        {
            maxMap.put(s.getModuleCode(), s.getScore());
        }

        // 2. 组装模块5-10批改输入（纯文本 + base64 图片）
        JSONArray items = new JSONArray();
        for (long code = MODULE_MIN; code <= MODULE_MAX; code++)
        {
            BigDecimal max = maxMap.get(code);
            if (max == null)
            {
                throw new ServiceException("模块「" + MODULE_NAMES[(int) code] + "」未设置满分值，请先完成模块赋分");
            }
            TeModuleContent content = moduleContentMapper.selectByGroupModule(groupId, code);
            String html = content != null && content.getContent() != null ? content.getContent() : "";
            JSONObject item = new JSONObject();
            item.put("moduleCode", code);
            item.put("moduleName", MODULE_NAMES[(int) code]);
            item.put("moduleMax", max.doubleValue());
            item.put("contentText", html.length() > 0 ? htmlToText(html) : "（本模块未开展，内容为空）");
            item.put("images", extractImages(html));
            items.add(item);
        }

        // 3. 调用 Python 推理服务；不可用/失败时本地规则模拟兜底
        JSONObject engineResult = callEngine(items);
        boolean byEngine = engineResult != null;
        JSONArray results = byEngine ? engineResult.getJSONArray("results") : null;
        String modelName = byEngine ? engineResult.getString("model") : "rule-sim(本地规则)";
        Date now = new Date();

        // 4. 结果落库（先删后插整体覆盖）+ 计算 AI 参考总分
        aiReviewMapper.deleteByGroup(groupId);
        List<TeAiReview> records = new ArrayList<>();
        BigDecimal aiRefScore = BigDecimal.ZERO;
        for (long code = MODULE_MIN; code <= MODULE_MAX; code++)
        {
            JSONObject r = findResult(results, code);
            Integer score = r != null ? r.getInteger("score") : null;
            String comment = r != null ? r.getString("comment") : "";
            if (score == null)
            {
                // 模型不可用/输出异常：该模块降级为规则模拟（分+三段式评语）
                String text = (String) findItem(items, code).get("contentText");
                score = (int) ruleScore(text);
                comment = ruleComment(text);
            }
            BigDecimal max = maxMap.get(code);
            TeAiReview record = new TeAiReview();
            record.setGroupId(groupId);
            record.setModuleCode(code);
            record.setAiScore(BigDecimal.valueOf(score));
            record.setReviewProcess(comment);
            record.setModelName(byEngine ? modelName + " / " + ENGINE_TAG : "rule-sim(本地规则)");
            record.setTeacherChecked(0L);
            record.setReviewTime(now);
            records.add(record);
            // AI 参考总分 = Σ(模块满分 × AI百分制分 / 100)
            aiRefScore = aiRefScore.add(max.multiply(BigDecimal.valueOf(score))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        aiReviewMapper.batchInsert(records);

        // 5. 回写小组 AI 参考分（保留1位小数）
        TeGroup update = new TeGroup();
        update.setId(groupId);
        update.setAiRefScore(aiRefScore.setScale(1, RoundingMode.HALF_UP).doubleValue());
        update.setUpdateTime(now);
        groupMapper.updateTeGroup(update);

        return byEngine ? "AI模型批改完成（引擎：" + modelName + "）" : "AI 引擎不可用，已自动切换本地规则模拟批改";
    }

    /** 引擎标记（记录进 model_name 便于教师核查批改来源） */
    private static final String ENGINE_TAG = "Qwen2.5-VL";

    // ------------------------------------------------------------------
    // 二、查询批改结果
    // ------------------------------------------------------------------

    /**
     * 教师端批改总览：小组 + 总稿状态 + AI参考分 + 组最终分 + 任务 + 批改数 + 发布状态
     */
    @Override
    public List<Map<String, Object>> reviewBoard(Long classId, Long operatorUserId)
    {
        // 1. 班级归属校验（非管理员只能看自己指导的班级）
        TeClass clazz = classMapper.selectTeClassById(classId);
        if (clazz == null)
        {
            throw new ServiceException("班级不存在");
        }
        if (!SecurityUtils.isAdmin(operatorUserId)
                && (clazz.getTeacherId() == null || !clazz.getTeacherId().equals(operatorUserId)))
        {
            throw new ServiceException("无权查看其它教师班级");
        }
        // 2. 组装每组的批改视角数据
        List<Map<String, Object>> board = new ArrayList<>();
        Map<Long, TeTaskAssign> assignMap = new HashMap<>();
        for (TeTaskAssign a : taskAssignMapper.selectByClassId(classId))
        {
            assignMap.put(a.getGroupId(), a);   // 一组一任务，取首条即可
        }
        for (TeGroup g : groupMapper.selectByClassId(classId))
        {
            Map<String, Object> row = new HashMap<>();
            row.put("id", g.getId());
            row.put("groupName", g.getGroupName());
            row.put("submitStatus", g.getSubmitStatus());
            row.put("aiRefScore", g.getAiRefScore());
            row.put("teacherScore", g.getTeacherScore());
            TeTaskAssign assign = assignMap.get(g.getId());
            row.put("taskCode", assign != null ? assign.getTaskCode() : null);
            row.put("taskName", assign != null ? assign.getTaskName() : null);
            row.put("reviewCount", aiReviewMapper.countByGroup(g.getId()));
            // 成绩发布状态：取本组任一成绩行的 is_published（同组整体发布）
            List<TeScore> scores = scoreMapper.selectByGroup(g.getId());
            row.put("scoreCount", scores.size());
            row.put("published", !scores.isEmpty() && scores.get(0).getIsPublished() != null
                    && scores.get(0).getIsPublished() == 1);
            board.add(row);
        }
        return board;
    }

    /**
     * 查询小组学生成绩列表（教师核对发布结果）
     */
    @Override
    public List<TeScore> listScores(Long groupId, Long operatorUserId)
    {
        checkGroupOwner(groupId, operatorUserId);
        return scoreMapper.selectByGroup(groupId);
    }

    /**
     * 查询小组批改结果（组装模块名称；无记录时返回空列表）
     */
    @Override
    public List<TeAiReview> listReview(Long groupId, Long operatorUserId)
    {
        checkGroupOwner(groupId, operatorUserId);
        List<TeAiReview> list = aiReviewMapper.selectByGroup(groupId);
        for (TeAiReview r : list)
        {
            long code = r.getModuleCode() == null ? 0 : r.getModuleCode();
            if (code >= 1 && code < MODULE_NAMES.length)
            {
                r.setModuleName(MODULE_NAMES[(int) code]);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // 三、教师终审
    // ------------------------------------------------------------------

    /**
     * 教师终审：逐模块核定分数（防越权：仅允许修改本组自己的批改记录）
     */
    @Override
    @Transactional
    public int finalizeReview(Long groupId, List<TeAiReview> list, String checkedBy, Long operatorUserId)
    {
        checkGroupOwner(groupId, operatorUserId);
        if (list == null || list.isEmpty())
        {
            throw new ServiceException("终审数据不能为空");
        }
        // 建立 id → 记录 映射，防止伪造其它组的批改ID越权改分
        Map<Long, TeAiReview> owned = new HashMap<>();
        for (TeAiReview r : aiReviewMapper.selectByGroup(groupId))
        {
            owned.put(r.getId(), r);
        }
        int rows = 0;
        Date now = new Date();
        for (TeAiReview form : list)
        {
            TeAiReview ownedRec = form.getId() == null ? null : owned.get(form.getId());
            if (ownedRec == null)
            {
                throw new ServiceException("批改记录不存在或不属于该小组");
            }
            TeAiReview update = new TeAiReview();
            update.setId(ownedRec.getId());
            // 教师核定分：空表示不调整（沿用AI参考分）；填了则限定在 0~100
            if (form.getTeacherScore() == null)
            {
                update.setTeacherScore(null);
            }
            else
            {
                double ts = form.getTeacherScore().doubleValue();
                update.setTeacherScore(BigDecimal.valueOf(Math.max(0, Math.min(100, ts))));
            }
            update.setTeacherChecked(1L);
            update.setCheckedBy(checkedBy);
            rows += aiReviewMapper.updateTeacherCheck(update);
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 四、成绩汇总与发布
    // ------------------------------------------------------------------

    /**
     * 成绩汇总：组分 = Σ模块生效分；个人分 = Σ(模块生效分 × 个人贡献率)；
     * 写入 te_score（未发布），并回写 te_group.teacher_score
     */
    @Override
    @Transactional
    public BigDecimal computeScores(Long groupId, Long operatorUserId)
    {
        TeGroup group = checkGroupOwner(groupId, operatorUserId);
        // 1. 必须已完成 AI 批改（6条记录）
        List<TeAiReview> reviews = aiReviewMapper.selectByGroup(groupId);
        if (reviews.size() < (int) (MODULE_MAX - MODULE_MIN + 1))
        {
            throw new ServiceException("请先完成 AI 批改（含全部计分模块），再进行成绩汇总");
        }
        // 2. 生效分（百分制）折算为"模块得分" = 模块满分 × 生效分/100；
        //    教师核定分优先，缺省用 AI 参考分；组最终分 = Σ模块得分（≤100）
        Map<Long, BigDecimal> finalMap = new HashMap<>();
        BigDecimal groupScore = BigDecimal.ZERO;
        for (TeAiReview r : reviews)
        {
            if (r.getModuleMax() == null)
            {
                throw new ServiceException("模块「" + MODULE_NAMES[(int) Math.max(0, Math.min(MODULE_NAMES.length - 1, r.getModuleCode()))]
                        + "」当前未设置满分值，请先完成模块赋分再汇总成绩");
            }
            BigDecimal percent = r.getTeacherScore() != null ? r.getTeacherScore() : r.getAiScore();
            // 模块得分 = 满分 × 百分制生效分 / 100（保留2位中间精度）
            BigDecimal moduleScore = r.getModuleMax().multiply(percent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            finalMap.put(r.getModuleCode(), moduleScore);
            groupScore = groupScore.add(moduleScore);
        }
        groupScore = groupScore.setScale(1, RoundingMode.HALF_UP);

        // 3. 组内学生 + 贡献率，逐人折算个人最终得分
        TeStudent query = new TeStudent();
        query.setGroupId(groupId);
        List<TeStudent> members = studentMapper.selectTeStudentList(query);
        if (members == null || members.isEmpty())
        {
            throw new ServiceException("该小组没有组员，无法汇总成绩");
        }
        List<TeContribution> contribs = contributionMapper.selectByGroup(groupId);
        // 贡献率索引：userId -> (moduleCode -> ratio%)
        Map<Long, Map<Long, BigDecimal>> ratioMap = new HashMap<>();
        for (TeContribution c : contribs)
        {
            ratioMap.computeIfAbsent(c.getUserId(), k -> new HashMap<>()).put(c.getModuleCode(), c.getRatio());
        }
        Date now = new Date();
        for (TeStudent m : members)
        {
            Map<Long, BigDecimal> ratios = ratioMap.getOrDefault(m.getUserId(), new HashMap<>());
            BigDecimal personal = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> e : finalMap.entrySet())
            {
                BigDecimal ratio = ratios.get(e.getKey());
                if (ratio != null)
                {
                    // 个人得分累加：模块生效分 × 贡献率%（贡献率满100%时拿满该模块）
                    personal = personal.add(e.getValue().multiply(ratio)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
            }
            TeScore score = new TeScore();
            score.setClassId(group.getClassId());
            score.setGroupId(groupId);
            score.setUserId(m.getUserId());
            score.setGroupScore(groupScore);
            score.setFinalScore(personal.setScale(1, RoundingMode.HALF_UP));
            score.setCreateTime(now);
            score.setUpdateTime(now);
            scoreMapper.upsert(score);   // upsert：重新汇算自动撤回发布，需重新发布
        }

        // 4. 回写小组最终分到 te_group（教师录入口径）
        TeGroup update = new TeGroup();
        update.setId(groupId);
        update.setTeacherScore(groupScore.doubleValue());
        update.setUpdateTime(now);
        groupMapper.updateTeGroup(update);
        return groupScore;
    }

    /**
     * 发布本组成绩（发布后组内学生可见）
     */
    @Override
    public int publishScores(Long groupId, Long operatorUserId)
    {
        checkGroupOwner(groupId, operatorUserId);
        int rows = scoreMapper.publishByGroup(groupId);
        if (rows == 0)
        {
            throw new ServiceException("该小组还没有成绩记录，请先完成成绩汇总");
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 五、学生端"我的成绩"
    // ------------------------------------------------------------------

    /**
     * 学生端查询：仅 is_published=1 时透出分数；批改过程对学生不可见（仅见终分）
     */
    @Override
    public Map<String, Object> myReview(Long userId)
    {
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student == null || student.getGroupId() == null)
        {
            throw new ServiceException("您尚未加入小组，暂无成绩信息");
        }
        Map<String, Object> view = new HashMap<>();
        TeScore score = scoreMapper.selectByUserId(userId);
        boolean published = score != null && score.getIsPublished() != null && score.getIsPublished() == 1;
        view.put("published", published);
        if (!published)
        {
            view.put("groupName", student.getGroupName());
            return view;   // 未发布：不透出任何分数
        }
        // 1. 成绩主体
        view.put("groupScore", score.getGroupScore());
        view.put("finalScore", score.getFinalScore());
        view.put("publishTime", score.getPublishTime());
        view.put("groupName", student.getGroupName());
        // 2. 各模块终分（仅模块名/满分/终分，不含AI批改过程）
        List<Map<String, Object>> modules = new ArrayList<>();
        for (TeAiReview r : aiReviewMapper.selectByGroup(student.getGroupId()))
        {
            Map<String, Object> m = new HashMap<>();
            m.put("moduleCode", r.getModuleCode());
            m.put("moduleName", MODULE_NAMES[(int) Math.max(0, Math.min(MODULE_NAMES.length - 1, r.getModuleCode()))]);
            m.put("moduleMax", r.getModuleMax());
            m.put("finalScore", r.getTeacherScore() != null ? r.getTeacherScore() : r.getAiScore());
            modules.add(m);
        }
        view.put("modules", modules);
        // 3. 本人各模块贡献率（回显折算依据）
        List<Map<String, Object>> contribs = new ArrayList<>();
        for (TeContribution c : contributionMapper.selectByGroup(student.getGroupId()))
        {
            if (userId.equals(c.getUserId()))
            {
                Map<String, Object> m = new HashMap<>();
                m.put("moduleCode", c.getModuleCode());
                m.put("moduleName", MODULE_NAMES[(int) Math.max(0, Math.min(MODULE_NAMES.length - 1, c.getModuleCode()))]);
                m.put("ratio", c.getRatio());
                contribs.add(m);
            }
        }
        view.put("contributions", contribs);
        return view;
    }

    // ------------------------------------------------------------------
    // 六、Python 推理服务调用与降级
    // ------------------------------------------------------------------

    /**
     * 调用 Python 推理服务：健康检查(短超时) → 批改(长超时)；
     * 服务不可用/HTTP异常/模型未加载时返回 null（由调用方切换规则模拟）
     */
    private JSONObject callEngine(JSONArray items)
    {
        try
        {
            RestTemplate healthRt = buildRestTemplate(healthTimeout);
            // 1. 健康检查：服务存活且模型就绪才走模型批改
            String health = healthRt.getForObject(engineBaseUrl + "/health", String.class);
            JSONObject h = JSON.parseObject(health);
            if (h == null || !h.getBooleanValue("ok"))
            {
                return null;
            }
            // 2. 批改请求（读超时放宽，CPU 推理可能每模块 10~30 秒）
            RestTemplate gradeRt = buildRestTemplate(gradeTimeout);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject body = new JSONObject();
            body.put("items", items);
            String resp = gradeRt.postForObject(engineBaseUrl + "/grade",
                    new HttpEntity<>(body.toJSONString(), headers), String.class);
            JSONObject result = JSON.parseObject(resp);
            return result != null && result.getBooleanValue("ok") ? result : null;
        }
        catch (Exception e)
        {
            // 网络不通 / 超时 / 响应解析失败：统一降级规则模拟（保证功能可用）
            return null;
        }
    }

    /**
     * 构建带超时的 RestTemplate（连接/读取超时按用途区分）
     */
    private RestTemplate buildRestTemplate(long readTimeout)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);          // 连接超时固定3秒（本机部署足够）
        factory.setReadTimeout((int) readTimeout);
        return new RestTemplate(factory);
    }

    /** 从批改结果数组中定位某模块的结果 */
    private JSONObject findResult(JSONArray results, long moduleCode)
    {
        if (results == null)
        {
            return null;
        }
        for (int i = 0; i < results.size(); i++)
        {
            JSONObject r = results.getJSONObject(i);
            if (r.getLongValue("moduleCode") == moduleCode)
            {
                return r;
            }
        }
        return null;
    }

    /** 从批改输入数组中定位某模块的输入（规则兜底评分时使用） */
    private JSONObject findItem(JSONArray items, long moduleCode)
    {
        for (int i = 0; i < items.size(); i++)
        {
            JSONObject r = items.getJSONObject(i);
            if (r.getLongValue("moduleCode") == moduleCode)
            {
                return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 七、本地规则模拟批改（零依赖兜底：任何机器都能跑）
    // ------------------------------------------------------------------

    /**
     * 规则模拟评分：字数/结构/代码块/图片/条目 五维度启发式加权（30~95分）
     */
    private double ruleScore(String text)
    {
        if (text == null || text.trim().isEmpty() || text.contains("本模块未开展"))
        {
            return 40;   // 空内容/未开展：给基础怜悯分并附低评
        }
        double score = 55;
        int len = text.length();
        if (len >= 200) score += 10;    // 有一定工作量
        if (len >= 500) score += 15;    // 内容充实
        if (text.contains("<h") || text.contains("##")) score += 10;   // 有标题结构
        if (text.contains("class=\"") && text.contains("hljs")) score += 10;  // 代码高亮块
        if (text.contains("pre>") || text.contains("code>")) score += 5;
        score = Math.min(95, score);
        return Math.max(30, score);
    }

    /**
     * 规则模拟评语：按命中维度生成"优点/问题/建议"三段式 JSON（贴近教师口吻）
     */
    private String ruleComment(String text)
    {
        String strengths, problems, advice;
        if (text == null || text.trim().isEmpty() || text.contains("本模块未开展"))
        {
            strengths = "模块框架有搭建意识，说明小组有整体规划。";
            problems = "本模块内容为空或未开展，没有任何实质性论述，无法体现工作量。";
            advice = "请尽快补充本模块内容：先列提纲，再逐节展开论述，完成后联系组内同学交叉检查。";
        }
        else
        {
            int len = text.length();
            boolean hasHead = text.contains("<h") || text.contains("##");
            boolean hasCode = text.contains("pre>") || text.contains("code>");
            strengths = (len >= 500 ? "内容充实、工作量饱满，" : len >= 200 ? "内容有一定深度，" : "模块已有基础内容，")
                    + (hasHead ? "层次分明、条理清晰，" : "") + (hasCode ? "代码呈现规范，" : "") + "看得出小组用心整理。";
            problems = (len < 200 ? "篇幅偏短，论述不够展开；" : "")
                    + (!hasHead ? "缺少小标题分层，阅读时不易抓住重点；" : "")
                    + (!hasCode ? "如有相关代码/公式建议补充呈现；" : "")
                    + "部分表述可以更严谨，注意与任务要求的对应关系。";
            advice = "建议：1) 按小标题组织内容，每段聚焦一个要点；"
                    + "2) 补充具体实例或推导过程增强说服力；"
                    + "3) 对照任务要求逐条自查，确保要点覆盖完整。";
        }
        JSONObject c = new JSONObject();
        c.put("strengths", strengths);
        c.put("problems", problems);
        c.put("advice", advice);
        return c.toJSONString();
    }

    // ------------------------------------------------------------------
    // 八、工具方法
    // ------------------------------------------------------------------

    /**
     * 班级归属校验：组存在 → 班级存在 → 非管理员时班级必须属于当前教师
     */
    private TeGroup checkGroupOwner(Long groupId, Long operatorUserId)
    {
        TeGroup group = groupMapper.selectById(groupId);
        if (group == null)
        {
            throw new ServiceException("小组不存在");
        }
        TeClass clazz = classMapper.selectTeClassById(group.getClassId());
        if (clazz == null)
        {
            throw new ServiceException("小组所属班级不存在");
        }
        if (!SecurityUtils.isAdmin(operatorUserId)
                && (clazz.getTeacherId() == null || !clazz.getTeacherId().equals(operatorUserId)))
        {
            throw new ServiceException("无权操作其它教师班级的小组");
        }
        return group;
    }

    /**
     * 富文本 → 纯文本：去 script/style/img 等标签，压缩空白（供模型阅读）
     */
    private String htmlToText(String html)
    {
        String text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        text = text.replaceAll("(?is)<br\\s*/?>", "\n").replaceAll("(?is)</(p|div|h[1-6]|li|tr)>", "\n");
        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&").replace("&quot;", "\"");
        // 压缩连续空白为单个空格、连续空行为单个换行
        text = text.replaceAll("[ \\t]+", " ").replaceAll("\\n\\s*\\n+", "\n");
        // 限制长度防止上下文溢出（模型批改按 8000 字截断）
        return text.length() > 8000 ? text.substring(0, 8000) + "\n…（内容过长已截断）" : text.trim();
    }

    /**
     * 提取富文本中的 base64 内嵌图片（最多6张，按出现顺序）
     */
    private List<String> extractImages(String html)
    {
        List<String> images = new ArrayList<>();
        Matcher m = IMG_PATTERN.matcher(html);
        while (m.find() && images.size() < 6)
        {
            images.add(m.group(1));
        }
        return images;
    }
}
