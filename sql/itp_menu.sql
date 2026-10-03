-- ============================================================================
-- 智能教学平台 菜单与角色初始化脚本（itp_platform 库）
-- ----------------------------------------------------------------------------
-- 【功能】
--   1. 预置业务角色：教师（role_key=teacher）、学生（role_key=student）
--   2. 管理员端菜单：教学管理目录 + 注册审批 / 教师管理 / 学生管理 及各操作按钮
-- 【说明】
--   - 菜单 ID 从 2000 起（避开若依基线菜单），角色 ID 从 100 起（避开基线角色）
--   - admin 用户为超级管理员，自动拥有全部菜单权限，无需分配
--   - 重复执行安全：先按 ID 删除再插入
--   - 本脚本只配置管理员端若依菜单；教师/学生端为前端独立门户
--     （/teacher、/student 静态路由 + 角色守卫分流），无需在若依中配置菜单，
--     教师端接口鉴权走 @ss.hasRole('teacher')，与菜单权限无关
-- ============================================================================

USE itp_platform;

-- ----------------------------------------------------------------------------
-- 1. 预置业务角色（教师/学生；代码中按 role_key 查找，勿改）
-- ----------------------------------------------------------------------------
DELETE FROM sys_user_role WHERE role_id >= 100;
DELETE FROM sys_role_menu WHERE role_id >= 100;
DELETE FROM sys_role WHERE role_id >= 100;

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES (100, '教师', 'teacher', 3, '1', 1, 1, '0', '0', 'admin', sysdate(), '教师角色（课程设计指导教师）');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES (101, '学生', 'student', 4, '5', 1, 1, '0', '0', 'admin', sysdate(), '学生角色（课程设计参与学生）');

-- ----------------------------------------------------------------------------
-- 2. 管理员端菜单（教学管理目录：注册审批 → 教师管理 → 学生管理）
--    教师管理 order_num=2 位于学生管理 order_num=3 左侧（需求要求）
-- ----------------------------------------------------------------------------
DELETE FROM sys_menu WHERE menu_id >= 2000;

-- 2.1 一级目录：教学管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2000, '教学管理', 0, 5, 'teach', NULL, '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '教学管理目录');

-- 2.2 二级菜单：注册审批
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2001, '注册审批', 2000, 1, 'registration', 'teach/registration/index', '', '', 1, 0, 'C', '0', '0', 'teach:registration:list', 'form', 'admin', sysdate(), '学生注册申请审批');

-- 2.3 二级菜单：教师管理（位于学生管理左侧）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2010, '教师管理', 2000, 2, 'teacher', 'teach/teacher/index', '', '', 1, 0, 'C', '0', '0', 'teach:teacher:list', 'peoples', 'admin', sysdate(), '教师账号管理（上限4名）');

-- 2.4 二级菜单：学生管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2020, '学生管理', 2000, 3, 'student', 'teach/student/index', '', '', 1, 0, 'C', '0', '0', 'teach:student:list', 'user', 'admin', sysdate(), '学生账号管理（层级展示/导入导出）');

-- ----------------------------------------------------------------------------
-- 3. 操作按钮权限（F 类型）
-- ----------------------------------------------------------------------------

-- 3.1 注册审批按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2002, '注册查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:registration:query',   '#', 'admin', sysdate(), ''),
(2003, '审批通过', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:registration:approve',  '#', 'admin', sysdate(), ''),
(2004, '审批驳回', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:registration:approve',  '#', 'admin', sysdate(), ''),
(2005, '申请删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:registration:remove',   '#', 'admin', sysdate(), '');

-- 3.2 教师管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2011, '教师查询',   2010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:query',    '#', 'admin', sysdate(), ''),
(2012, '教师新增',   2010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:add',      '#', 'admin', sysdate(), ''),
(2013, '教师修改',   2010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:edit',     '#', 'admin', sysdate(), ''),
(2014, '教师重置密码', 2010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:resetPwd', '#', 'admin', sysdate(), ''),
(2015, '教师删除',   2010, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:remove',   '#', 'admin', sysdate(), ''),
(2016, '教师导出',   2010, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:teacher:export',   '#', 'admin', sysdate(), '');

-- 3.3 学生管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2021, '学生查询',     2020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:query',    '#', 'admin', sysdate(), ''),
(2022, '学生新增',     2020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:add',      '#', 'admin', sysdate(), ''),
(2023, '学生修改',     2020, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:edit',     '#', 'admin', sysdate(), ''),
(2024, '学生重置密码', 2020, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:resetPwd', '#', 'admin', sysdate(), ''),
(2025, '学生删除',     2020, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:remove',   '#', 'admin', sysdate(), ''),
(2026, '学生导入',     2020, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:import',   '#', 'admin', sysdate(), ''),
(2027, '学生导出',     2020, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'teach:student:export',   '#', 'admin', sysdate(), '');

-- ----------------------------------------------------------------------------
-- 【端侧菜单隔离】隐藏若依自带的系统级菜单（系统管理/系统监控/系统工具/若依官网）：
-- 教学平台三端各司其职——管理员只见「教学管理」，教师/学生角色本就未绑定系统菜单，
-- 若依底层菜单对业务无入口价值，隐藏后管理员侧边栏不再混入框架自带操作。
-- 恢复方法：UPDATE sys_menu SET visible='0' WHERE menu_id IN (1,2,3,4);
-- ----------------------------------------------------------------------------
UPDATE sys_menu SET visible = '1' WHERE menu_id IN (1, 2, 3, 4);
