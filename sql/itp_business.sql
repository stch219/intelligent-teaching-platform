-- ============================================================================
-- 智能教学平台业务表结构脚本（itp_platform 库）
-- 说明：本脚本在若依基线表基础上创建教学业务表
-- 命名规范：前缀 te_（teaching），字段全部带注释
-- ============================================================================

USE itp_platform;

-- ----------------------------------------------------------------------------
-- 1. 教师扩展表（教师登录账号复用 sys_user，此处扩展业务属性）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_teacher;
CREATE TABLE te_teacher (
    user_id      BIGINT       NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
    title        VARCHAR(50)  DEFAULT '' COMMENT '职称',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB COMMENT='教师扩展信息表';

-- ----------------------------------------------------------------------------
-- 2. 学生扩展表（学生登录账号复用 sys_user，此处扩展学籍与分组属性）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_student;
CREATE TABLE te_student (
    user_id         BIGINT      NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
    student_no      VARCHAR(20) NOT NULL COMMENT '学号',
    class_id        BIGINT      DEFAULT NULL COMMENT '所属班级ID（关联te_class.id）',
    group_id        BIGINT      DEFAULT NULL COMMENT '所属小组ID（关联te_group.id）',
    role_type       TINYINT     DEFAULT 4 COMMENT '角色（1组长 2汇报人 3组长兼汇报人 4成员）',
    duty_assignment VARCHAR(500) DEFAULT '' COMMENT '个人分工说明（提交组长确认）',
    duty_status     TINYINT     DEFAULT 0 COMMENT '分工确认状态（0待组长确认 1已确认）',
    create_time     DATETIME    DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (user_id),
    KEY idx_student_no (student_no),
    KEY idx_class_id (class_id),
    KEY idx_group_id (group_id)
) ENGINE=InnoDB COMMENT='学生扩展信息表';

-- ----------------------------------------------------------------------------
-- 3. 班级表（教师创建，班级级任务截止日期随班设置）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_class;
CREATE TABLE te_class (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    class_name   VARCHAR(100) NOT NULL COMMENT '班级名称',
    teacher_id   BIGINT       NOT NULL COMMENT '指导教师用户ID（关联sys_user.user_id）',
    group_count  TINYINT      DEFAULT 4 COMMENT '分组数（4/5/6，按学号末2位对组数取模自动分组）',
    deadline     DATETIME     DEFAULT NULL COMMENT '课程设计任务截止提交时间',
    status       CHAR(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by    VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB COMMENT='班级表';

-- ----------------------------------------------------------------------------
-- 4. 小组表（组名字母命名：A组/B组/…，展示使用专属色条底纹）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_group;
CREATE TABLE te_group (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '小组ID',
    group_name    VARCHAR(50)  NOT NULL COMMENT '小组名称（A组/B组/…按字母命名）',
    group_order   INT          NOT NULL DEFAULT 1 COMMENT '组序号（用于排序与自动分组规则）',
    class_id      BIGINT       NOT NULL COMMENT '所属班级ID',
    color_code    VARCHAR(20)  DEFAULT '#409EFF' COMMENT '专属色条底纹颜色值',
    submit_status TINYINT      DEFAULT 0 COMMENT '总稿提交状态（0未提交 1已提交）',
    submit_time   DATETIME     DEFAULT NULL COMMENT '总稿提交时间',
    final_pdf     VARCHAR(500) DEFAULT '' COMMENT '总稿PDF文件路径',
    ai_ref_score  DECIMAL(5,1) DEFAULT NULL COMMENT 'AI辅助参考分（仅教师可见）',
    teacher_score DECIMAL(5,1) DEFAULT NULL COMMENT '教师录入的组最终分（发布前学生不可见）',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='小组表';

-- ----------------------------------------------------------------------------
-- 5. 注册审批表（学生注册填班级/学号，管理员审批通过后正式建号）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_registration;
CREATE TABLE te_registration (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    user_id      BIGINT       DEFAULT NULL COMMENT '用户ID（审批通过回填）',
    real_name    VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    student_no   VARCHAR(20)  NOT NULL COMMENT '学号',
    class_name   VARCHAR(100) NOT NULL COMMENT '填报班级名称',
    phone        VARCHAR(20)  DEFAULT '' COMMENT '联系电话',
    status       TINYINT      DEFAULT 0 COMMENT '审批状态（0待审批 1通过 2驳回）',
    approve_by   VARCHAR(64)  DEFAULT '' COMMENT '审批人',
    approve_time DATETIME     DEFAULT NULL COMMENT '审批时间',
    reject_reason VARCHAR(200) DEFAULT '' COMMENT '驳回原因',
    create_time  DATETIME     DEFAULT NULL COMMENT '申请时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='学生注册审批表';

-- ----------------------------------------------------------------------------
-- 6. 任务表（教师按班级发布任务，ABCD排序；含任务要求各要素）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_task;
CREATE TABLE te_task (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    task_code        VARCHAR(10)  NOT NULL COMMENT '任务编号（A/B/C/D/…）',
    task_name        VARCHAR(200) NOT NULL COMMENT '任务题目',
    class_id         BIGINT       NOT NULL COMMENT '所属班级ID',
    design_goal      TEXT         COMMENT '设计目标',
    requirement      TEXT         COMMENT '具体要求',
    grading_standard TEXT         COMMENT '评分标准',
    tips             TEXT         COMMENT '重要提示',
    teacher_id       BIGINT       NOT NULL COMMENT '发布教师用户ID',
    create_time      DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='课程设计任务表';

-- ----------------------------------------------------------------------------
-- 7. 任务分配表（哪个组领取哪个任务；题目由教师分配，取消组长选题）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_task_assign;
CREATE TABLE te_task_assign (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '分配ID',
    task_id     BIGINT   NOT NULL COMMENT '任务ID（关联te_task.id）',
    group_id    BIGINT   NOT NULL COMMENT '小组ID（关联te_group.id）',
    assign_time DATETIME DEFAULT NULL COMMENT '分配时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_group (task_id, group_id)
) ENGINE=InnoDB COMMENT='任务分配表';

-- ----------------------------------------------------------------------------
-- 8. 模块设置表（教师按班级设置各模块字数/条目区间，同步学生端）
--    模块顺序全平台统一：1封面 2任务要求 3角色与分工 4参数配置
--    5知识背景 6计算步骤 7代码实现 8计算结果与分析 9心得体会 10参考资料
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_module_set;
CREATE TABLE te_module_set (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '设置ID',
    class_id     BIGINT      NOT NULL COMMENT '班级ID',
    module_code  TINYINT     NOT NULL COMMENT '模块编号（1-10）',
    module_name  VARCHAR(50) NOT NULL COMMENT '模块名称',
    count_type   TINYINT     DEFAULT 1 COMMENT '校验类型（1字数 2条目数）',
    min_count    INT         DEFAULT 0 COMMENT '最低要求（字数或条目数）',
    max_count    INT         DEFAULT 0 COMMENT '最高要求（字数或条目数，0为不限）',
    need_formula TINYINT     DEFAULT 0 COMMENT '是否支持公式录入（0否 1是）',
    need_image   TINYINT     DEFAULT 0 COMMENT '是否支持图片上传/粘贴（0否 1是）',
    need_code    TINYINT     DEFAULT 0 COMMENT '是否支持代码块（0否 1是）',
    editable     TINYINT     DEFAULT 1 COMMENT '是否需要学生编辑（0否如封面/参数 1是）',
    create_time  DATETIME    DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_module (class_id, module_code)
) ENGINE=InnoDB COMMENT='模块设置表';

-- ----------------------------------------------------------------------------
-- 9. 模块内容表（小组协同编辑核心表；状态三态：未开展/暂存XX%/已提交）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_module_content;
CREATE TABLE te_module_content (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '内容ID',
    group_id       BIGINT       NOT NULL COMMENT '小组ID',
    module_code    TINYINT      NOT NULL COMMENT '模块编号（1-10）',
    content        LONGTEXT     COMMENT '富文本内容（HTML，支持图片/公式/代码块）',
    status         TINYINT      DEFAULT 0 COMMENT '状态（0未开展 1暂存 2已提交）',
    progress       INT          DEFAULT 0 COMMENT '暂存进度百分比（0-100，按字数/条目折算）',
    version        INT          DEFAULT 0 COMMENT '乐观锁版本号（协同编辑防覆盖）',
    word_count     INT          DEFAULT 0 COMMENT '当前有效字数（去HTML标签统计）',
    item_count     INT          DEFAULT 0 COMMENT '当前条目数（参考资料模块使用）',
    submit_time    DATETIME     DEFAULT NULL COMMENT '提交时间',
    create_by      VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time    DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by      VARCHAR(64)  DEFAULT '' COMMENT '最后编辑者',
    update_time    DATETIME     DEFAULT NULL COMMENT '最后编辑时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_module (group_id, module_code)
) ENGINE=InnoDB COMMENT='模块内容表';

-- ----------------------------------------------------------------------------
-- 10. 贡献率表（模块5-10需分配贡献率且合计100%；前4模块不计算贡献率）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_contribution;
CREATE TABLE te_contribution (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '贡献率ID',
    group_id     BIGINT        NOT NULL COMMENT '小组ID',
    module_code  TINYINT       NOT NULL COMMENT '模块编号（5-10）',
    user_id      BIGINT        NOT NULL COMMENT '组员用户ID',
    ratio        DECIMAL(5,2)  DEFAULT 0 COMMENT '贡献率百分比（0-100）',
    confirmed    TINYINT       DEFAULT 0 COMMENT '组员是否已确认（0未确认 1已确认）',
    confirm_time DATETIME      DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_module_user (group_id, module_code, user_id)
) ENGINE=InnoDB COMMENT='模块贡献率表';

-- ----------------------------------------------------------------------------
-- 11. 参数配置数据表（教师上传参数文件解析入库，学生端按组匹配只读展示）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_param_data;
CREATE TABLE te_param_data (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '参数ID',
    task_id     BIGINT        NOT NULL COMMENT '所属任务ID',
    param_name  VARCHAR(100)  NOT NULL COMMENT '参数名称（如整车质量）',
    param_key   VARCHAR(50)   DEFAULT '' COMMENT '参数键（英文标识，用于计算）',
    param_value VARCHAR(200)  DEFAULT '' COMMENT '参数值',
    param_unit  VARCHAR(20)   DEFAULT '' COMMENT '参数单位（如 kg、m/s²）',
    sort_order  INT           DEFAULT 0 COMMENT '显示顺序',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB COMMENT='参数配置数据表';

-- ----------------------------------------------------------------------------
-- 12. 参考文献表（参考资料模块专用；规范著录，必填项小红星标注）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_reference;
CREATE TABLE te_reference (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文献ID',
    content_id   BIGINT       NOT NULL COMMENT '关联模块内容ID（te_module_content.id）',
    ref_type     TINYINT      DEFAULT 1 COMMENT '文献类型（1图书 2期刊 3标准 4网络资源）',
    author       VARCHAR(200) NOT NULL COMMENT '主要责任者（作者，必填）',
    title        VARCHAR(300) NOT NULL COMMENT '文献题名（必填）',
    press        VARCHAR(100) DEFAULT '' COMMENT '出版社（图书必填）',
    journal      VARCHAR(100) DEFAULT '' COMMENT '期刊名称（期刊必填）',
    pub_year     VARCHAR(10)  DEFAULT '' COMMENT '出版年份/标准号年份',
    update_date  VARCHAR(20)  DEFAULT '' COMMENT '更新/修改日期（网络资源必填）',
    url          VARCHAR(500) DEFAULT '' COMMENT '网址（网络资源必填，置最后）',
    sort_order   INT          DEFAULT 0 COMMENT '著录顺序',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_content_id (content_id)
) ENGINE=InnoDB COMMENT='参考文献表';

-- ----------------------------------------------------------------------------
-- 13. 学习资料表（教师发布，供对应班级学生查看/下载）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_material;
CREATE TABLE te_material (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    class_id     BIGINT       NOT NULL COMMENT '所属班级ID',
    material_type TINYINT     DEFAULT 3 COMMENT '类型（1任务指导书 2说明书模板 3参考答案 4辅助资料）',
    file_name    VARCHAR(200) NOT NULL COMMENT '资料显示名称',
    file_path    VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size    BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    visible      TINYINT      DEFAULT 1 COMMENT '学生是否可见（参考答案=0严格保密）',
    upload_by    VARCHAR(64)  DEFAULT '' COMMENT '上传教师',
    upload_time  DATETIME     DEFAULT NULL COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='学习资料表';

-- ----------------------------------------------------------------------------
-- 14. 参考答案解析表（AI批改依据；解析文本供大模型阅读，学生不可见）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_answer_parse;
CREATE TABLE te_answer_parse (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '解析ID',
    material_id  BIGINT       NOT NULL COMMENT '关联资料ID（te_material.id，类型3）',
    task_id      BIGINT       DEFAULT NULL COMMENT '关联任务ID',
    module_code  TINYINT      DEFAULT NULL COMMENT '对应模块编号（NULL为整份文档）',
    parsed_text  LONGTEXT     COMMENT '解析出的纯文本内容',
    parse_status TINYINT      DEFAULT 0 COMMENT '解析状态（0待解析 1完成 2失败）',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_material_id (material_id)
) ENGINE=InnoDB COMMENT='参考答案解析表';

-- ----------------------------------------------------------------------------
-- 15. AI批改结果表（大模型输出：批改过程+分数；教师可见可核查，学生仅见终分）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_ai_review;
CREATE TABLE te_ai_review (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '批改ID',
    group_id        BIGINT        NOT NULL COMMENT '小组ID',
    module_code     TINYINT       NOT NULL COMMENT '模块编号',
    review_process  LONGTEXT      COMMENT '批改过程（JSON：逐条点评/问题定位/修改建议）',
    ai_score        DECIMAL(5,1)  DEFAULT NULL COMMENT 'AI参考分',
    similarity      DECIMAL(5,2)  DEFAULT NULL COMMENT '与参考答案相似度（0-100）',
    model_name      VARCHAR(100)  DEFAULT '' COMMENT '使用的模型名称',
    teacher_checked TINYINT       DEFAULT 0 COMMENT '教师是否已核查（0未核查 1已核查）',
    checked_by      VARCHAR(64)   DEFAULT '' COMMENT '核查教师',
    review_time     DATETIME      DEFAULT NULL COMMENT '批改时间',
    PRIMARY KEY (id),
    KEY idx_group_module (group_id, module_code)
) ENGINE=InnoDB COMMENT='AI批改结果表';

-- ----------------------------------------------------------------------------
-- 16. 模块赋分表（教师对模块赋分，总分必须=100，否则设置失败）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_module_score_set;
CREATE TABLE te_module_score_set (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '赋分ID',
    class_id    BIGINT       NOT NULL COMMENT '班级ID',
    module_code TINYINT      NOT NULL COMMENT '模块编号（5-10，前4模块不计贡献率不赋分）',
    score       DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '模块满分值',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_module (class_id, module_code)
) ENGINE=InnoDB COMMENT='模块赋分表';

-- ----------------------------------------------------------------------------
-- 17. 成绩表（个人最终得分 = Σ(模块满分 × 个人该模块贡献率)，发布后学生可见）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_score;
CREATE TABLE te_score (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
    class_id     BIGINT        NOT NULL COMMENT '班级ID',
    group_id     BIGINT        NOT NULL COMMENT '小组ID',
    user_id      BIGINT        NOT NULL COMMENT '学生用户ID',
    group_score  DECIMAL(5,1)  DEFAULT NULL COMMENT '小组最终分（教师录入）',
    final_score  DECIMAL(5,1)  DEFAULT NULL COMMENT '个人最终得分（公式折算，教师可调整）',
    is_published TINYINT       DEFAULT 0 COMMENT '是否发布（0未发布 1已发布，发布后学生可见）',
    publish_time DATETIME      DEFAULT NULL COMMENT '发布时间',
    create_time  DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='成绩表';

-- ----------------------------------------------------------------------------
-- 18. 预警规则表（教师设定：距截止日X天完成率不足Y%触发；最多3条/班级）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_warning_rule;
CREATE TABLE te_warning_rule (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    class_id          BIGINT      NOT NULL COMMENT '班级ID',
    level             TINYINT     NOT NULL COMMENT '预警级别（1一般-黄 2重要-橙 3紧急-红）',
    days_before       INT         NOT NULL COMMENT '距截止提交时间天数',
    progress_threshold INT        NOT NULL COMMENT '完成率阈值百分比（低于则触发）',
    enabled           TINYINT     DEFAULT 1 COMMENT '是否启用（0停用 1启用）',
    create_time       DATETIME    DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='进度预警规则表';

-- ----------------------------------------------------------------------------
-- 19. 预警记录表（触发后推送仪表盘置顶+消息通知，三级颜色标识）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_warning_record;
CREATE TABLE te_warning_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预警ID',
    class_id    BIGINT       NOT NULL COMMENT '班级ID',
    group_id    BIGINT       NOT NULL COMMENT '小组ID',
    level       TINYINT      NOT NULL COMMENT '预警级别（1一般 2重要 3紧急）',
    content     VARCHAR(500) NOT NULL COMMENT '预警内容描述',
    progress    INT          DEFAULT 0 COMMENT '触发时小组完成率',
    send_time   DATETIME     DEFAULT NULL COMMENT '推送时间',
    PRIMARY KEY (id),
    KEY idx_group_id (group_id)
) ENGINE=InnoDB COMMENT='进度预警记录表';

-- ----------------------------------------------------------------------------
-- 20. 群聊会话表（组内群聊自动创建含组员+指导教师）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_chat;
CREATE TABLE te_chat (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    chat_type   TINYINT      NOT NULL DEFAULT 2 COMMENT '会话类型（1单聊 2群聊）',
    chat_name   VARCHAR(100) DEFAULT '' COMMENT '会话名称（群聊名，单聊为空）',
    group_id    BIGINT       DEFAULT NULL COMMENT '关联小组ID（群聊时）',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='聊天会话表';

-- ----------------------------------------------------------------------------
-- 21. 会话成员表（含最后已读消息ID，用于已读/未读回执）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_chat_member;
CREATE TABLE te_chat_member (
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    chat_id          BIGINT   NOT NULL COMMENT '会话ID',
    user_id          BIGINT   NOT NULL COMMENT '成员用户ID',
    last_read_msg_id BIGINT   DEFAULT 0 COMMENT '最后已读消息ID（已读回执依据）',
    join_time        DATETIME DEFAULT NULL COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_user (chat_id, user_id)
) ENGINE=InnoDB COMMENT='会话成员表';

-- ----------------------------------------------------------------------------
-- 22. 聊天消息表（单聊+群聊统一存储）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_message;
CREATE TABLE te_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    chat_id     BIGINT       NOT NULL COMMENT '会话ID',
    sender_id   BIGINT       NOT NULL COMMENT '发送者用户ID',
    msg_type    TINYINT      DEFAULT 1 COMMENT '消息类型（1文本 2图片 3文件）',
    content     TEXT         COMMENT '消息内容（文本或文件路径）',
    send_time   DATETIME     DEFAULT NULL COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_chat_id (chat_id)
) ENGINE=InnoDB COMMENT='聊天消息表';

-- ----------------------------------------------------------------------------
-- 23. 模块讨论话题表（学生发起，模块讨论标识用小写 abcd）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_topic;
CREATE TABLE te_topic (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '话题ID',
    group_id    BIGINT       NOT NULL COMMENT '小组ID',
    module_code TINYINT      NOT NULL COMMENT '关联模块编号（讨论标识小写abcd…按任务序）',
    title       VARCHAR(200) NOT NULL COMMENT '话题标题',
    creator_id  BIGINT       NOT NULL COMMENT '发起人用户ID',
    create_time DATETIME     DEFAULT NULL COMMENT '发起时间',
    update_time DATETIME     DEFAULT NULL COMMENT '最后回复时间',
    PRIMARY KEY (id),
    KEY idx_group_id (group_id)
) ENGINE=InnoDB COMMENT='模块讨论话题表';

-- ----------------------------------------------------------------------------
-- 24. 话题回复表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_topic_reply;
CREATE TABLE te_topic_reply (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    topic_id    BIGINT   NOT NULL COMMENT '话题ID（关联te_topic.id）',
    reply_id    BIGINT   DEFAULT NULL COMMENT '父回复ID（NULL为首层回复）',
    user_id     BIGINT   NOT NULL COMMENT '回复人用户ID',
    content     TEXT     NOT NULL COMMENT '回复内容',
    reply_time  DATETIME DEFAULT NULL COMMENT '回复时间',
    PRIMARY KEY (id),
    KEY idx_topic_id (topic_id)
) ENGINE=InnoDB COMMENT='话题回复表';

-- ----------------------------------------------------------------------------
-- 25. 系统公告表（教师编辑并指定班级发布，推送对应班级学生端）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_announcement;
CREATE TABLE te_announcement (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    title        VARCHAR(200) NOT NULL COMMENT '公告标题',
    content      TEXT         NOT NULL COMMENT '公告内容',
    class_id     BIGINT       NOT NULL COMMENT '发布目标班级ID',
    publish_by   VARCHAR(64)  DEFAULT '' COMMENT '发布教师',
    publish_time DATETIME     DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='系统公告表';

-- ----------------------------------------------------------------------------
-- 26. 帮助中心表（操作指南/功能说明/常见问题）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_help;
CREATE TABLE te_help (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帮助ID',
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    category    VARCHAR(50)  DEFAULT '常见问题' COMMENT '分类（操作指南/功能说明/常见问题）',
    content     LONGTEXT     COMMENT '内容（富文本）',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      CHAR(1)      DEFAULT '0' COMMENT '状态（0发布 1下架）',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='帮助中心表';

-- ----------------------------------------------------------------------------
-- 27. 成员确认记录表（提交预检要求：组内每名成员确认分工与贡献率）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS te_confirm_record;
CREATE TABLE te_confirm_record (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '确认ID',
    group_id     BIGINT   NOT NULL COMMENT '小组ID',
    user_id      BIGINT   NOT NULL COMMENT '确认人用户ID',
    confirm_type TINYINT  DEFAULT 1 COMMENT '确认类型（1分工确认 2贡献率确认 3总稿知悉）',
    confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_user_type (group_id, user_id, confirm_type)
) ENGINE=InnoDB COMMENT='成员确认记录表';
