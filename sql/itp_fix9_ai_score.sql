-- ============================================================================
-- 【功能】阶段9.2 修复脚本：AI参考分口径修正（百分制 → 模块绝对分）
-- ----------------------------------------------------------------------------
-- 【背景】修复前 te_ai_review.ai_score / teacher_score 存的是"百分制参考分（0-100）"，
--         界面上与模块满分并排展示时出现"满分20 | AI参考分85"的观感错乱。
--         修复后代码统一为"模块绝对分"口径：模块满分 × 百分制分 / 100（≤模块满分），
--         成绩汇总直接对生效分求和，全程同口径。
-- 【适用】仅修复前已产生批改/核定数据的存量库需要执行一次；新环境走
--         itp_business.sql 全量初始化则无需执行本脚本。
-- 【注意】教师核定分若已填写（百分制口径）同样按模块满分折算迁移。
-- ============================================================================

-- 1. 存量 AI 参考分迁移：百分制 → 模块绝对分（关联赋分表取各模块满分，钳制≤满分）
UPDATE te_ai_review r
JOIN te_group g             ON g.id = r.group_id
JOIN te_module_score_set s  ON s.class_id = g.class_id AND s.module_code = r.module_code
SET r.ai_score = LEAST(s.score, ROUND(r.ai_score * s.score / 100, 1));

-- 2. 存量教师核定分迁移（与AI参考分同为百分制口径，一并折算；无核定数据时本条影响0行）
UPDATE te_ai_review r
JOIN te_group g             ON g.id = r.group_id
JOIN te_module_score_set s  ON s.class_id = g.class_id AND s.module_code = r.module_code
SET r.teacher_score = LEAST(s.score, ROUND(r.teacher_score * s.score / 100, 1))
WHERE r.teacher_score IS NOT NULL;

-- 3. 重算小组 AI 参考分 = Σ 各模块AI参考分（与批改落库口径一致）
UPDATE te_group g
SET g.ai_ref_score = (
    SELECT SUM(r.ai_score) FROM te_ai_review r WHERE r.group_id = g.id
)
WHERE EXISTS (SELECT 1 FROM te_ai_review r WHERE r.group_id = g.id);

-- 4. 重算小组最终分 = Σ 各模块生效分（教师核定分优先，缺省用AI参考分；与成绩汇总口径一致）
--    （汇总过的班级建议教师在批改页重新执行一次"成绩汇总"，个人分同步刷新）
UPDATE te_group g
SET g.teacher_score = (
    SELECT SUM(COALESCE(r.teacher_score, r.ai_score)) FROM te_ai_review r WHERE r.group_id = g.id
)
WHERE EXISTS (SELECT 1 FROM te_ai_review r WHERE r.group_id = g.id);
