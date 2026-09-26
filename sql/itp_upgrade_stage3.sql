-- ============================================================================
-- 智能教学平台 阶段3（教师端）数据库升级脚本
-- 说明：在已导入 itp_business.sql 的库上执行本脚本完成增量升级
-- 变更内容：
--   1. te_class 新增 group_count 字段（每班分组数 4/5/6，按学号末2位对组数取模自动分组）
--   2. te_group.group_name 注释更新为字母组名（A组/B组/…，需求变更）
-- ============================================================================

USE itp_platform;

-- 1. 班级表新增分组数字段
ALTER TABLE te_class
    ADD COLUMN group_count TINYINT DEFAULT 4 COMMENT '分组数（4/5/6，按学号末2位对组数取模自动分组）' AFTER teacher_id;

-- 2. 小组表组名注释更新（组名由"第N组"变更为"A组/B组/…"字母命名）
ALTER TABLE te_group
    MODIFY COLUMN group_name VARCHAR(50) NOT NULL COMMENT '小组名称（A组/B组/…按字母命名）';
