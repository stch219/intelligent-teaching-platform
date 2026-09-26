# 智能教学平台（Intelligent Teaching Platform）

> 基于 RuoYi-Vue3 的《汽车理论》项目制课程设计全过程管理平台，集成本地多模态大模型实现 AI 辅助批改与智能答疑。

## 项目简介

本平台面向高校项目制课程设计场景，覆盖**教师建班分组 → 学生在线协同完成课程设计 → AI 辅助批改 → 教师终审发布成绩**的完整闭环，支持三种角色协同工作。

## 功能特性

### 管理员端
- 教师账号管理（上限 4 名，创建/禁用/删除二次确认）
- 学生账号管理（上限 300 名，注册审批、批量导入导出、层级展示）
- 角色权限配置、数据备份与统计

### 教师端
- 班级与分组管理：创建班级、设置截止日期、Excel/CSV/TXT 名单批量导入
- 自动分组：按学号末 2 位取模规则（4n/5n/6n）智能分组，支持手动调整
- 任务管理：任务分配（ABCD 排序）、模块字数区间设置、资料发布、任务要求发布
- 过程监控：权限穿透查看学生进度、三级进度预警（一般/重要/紧急）
- 成绩管理：模块赋分（总分=100 校验）、贡献率评定、成绩发布
- AI 辅助批改：上传参考答案后由本地大模型预批改，教师核查后终审定分

### 学生端
- 注册审批制：填写班级/学号注册，管理员审批后登录
- 角色选定：组长 / 汇报人 / 组长兼汇报人 / 成员（四选一，同组唯一性校验）
- 10 大课程设计模块在线协同编辑：封面、任务要求、角色与分工、参数配置、知识背景、
  计算步骤、代码实现、计算结果与分析、心得体会、参考资料
- 富文本编辑：支持图片上传/粘贴、公式录入（KaTeX）、代码块、字数实时校验
- 贡献率分配：勾选模块完成人并分配贡献率（合计 100%）
- 说明书总稿：一键生成含目录页的 PDF 终稿，组长合规预检后提交
- 实时通讯：微信群式组内群聊、师生单聊（已读/未读回执）、模块讨论话题
- AI 助教：学生提问，本地大模型结合课程上下文解答

### 系统引擎
- 封面自动生成（学院、姓名、组长、汇报人、指导教师、日期自动汇聚）
- 提交合规性预检（全员有分工、组长/汇报人唯一、贡献率合计 100%、必填完整）
- AI 批改流水线：参考答案 + 评分标准 → 大模型逐模块批改 → 输出批改过程与分数 → 教师核查终审
- 进度预警定时任务：距截止日 X 天完成率不足 Y% 自动触发三级预警

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.1.0 |
| 基座框架 | RuoYi-Vue | 3.9.2 |
| JDK | Java | 17 |
| 前端框架 | Vue | 3.5.x |
| UI 组件库 | Element Plus | 2.13.x |
| 构建工具 | Vite | 5.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 5.0 |
| ORM | MyBatis / PageHelper | - |
| 实时通讯 | WebSocket | - |
| AI 推理 | llama-cpp-python + Qwen2.5-VL-7B（GGUF Q4） | - |
| AI 服务框架 | FastAPI | - |
| 富文本/公式 | wangEditor + KaTeX | - |
| PDF 生成 | openhtmltopdf | - |

## 项目结构

```
intelligent-teaching-platform/
├── ruoyi-admin/        # 后端启动模块（含业务配置）
├── ruoyi-common/       # 通用工具模块
├── ruoyi-framework/    # 框架核心（安全、拦截器等）
├── ruoyi-system/       # 系统管理模块（用户/角色/菜单）
├── ruoyi-generator/    # 代码生成模块
├── ruoyi-quartz/       # 定时任务模块（进度预警）
├── ruoyi-teach/        # ★ 教学业务模块（班级/分组/任务/模块/成绩/预警）
├── ruoyi-ai/           # ★ AI 集成模块（调用 ai-server）
├── ai-server/          # ★ Python AI 微服务（FastAPI + llama.cpp 推理）
├── ruoyi-ui/           # Vue3 前端
├── sql/                # 数据库脚本（若依基线 + 业务表）
├── docs/
│   ├── requirements/   # 需求文档
│   └── issues/         # 开发问题记录
├── pom.xml             # Maven 父工程
├── LICENSE             # MIT License
└── README.md
```

## 快速开始

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 5.0+ |
| Python | 3.10+（仅 AI 模块需要，可关闭） |
| NVIDIA GPU | 8GB+ 显存推荐（AI 识图；无独显可切换 CPU 模式或关闭 AI） |

### 启动步骤

```bash
# 1. 导入数据库（先创建 itp_platform 库）
mysql -u root -p < sql/ry_20260417.sql
mysql -u root -p < sql/quartz.sql
mysql -u root -p < sql/itp_business.sql

# 2. 修改后端配置 ruoyi-admin/src/main/resources/application-druid.yml（数据库密码）
#    及 application.yml（Redis 配置）

# 3. 启动后端
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 4. 启动前端
cd ruoyi-ui
npm install
npm run dev

# 5.（可选）启动 AI 微服务
cd ai-server
pip install -r requirements.txt
python ai_server.py
```

访问 http://localhost:80（前端开发端口见 vite 配置），默认管理员账号 `admin / admin123`。

## 开发计划

- [x] 阶段0：环境准备 + 仓库初始化 + 若依基线跑通
- [ ] 阶段1：数据库业务表设计 + 代码生成
- [ ] 阶段2：管理员端
- [ ] 阶段3：教师端
- [ ] 阶段4：学生端（10 大模块协同编辑）
- [ ] 阶段5：系统引擎（封面/PDF/合规预检）
- [ ] 阶段6：消息系统（WebSocket 群聊/单聊）
- [ ] 阶段7：AI 批改模块 + 成绩判分
- [ ] 阶段8：仪表盘、三级预警、帮助中心
- [ ] 阶段9：测试、文档终稿、发布

## 文档

- [需求文档](docs/requirements/)
- [开发问题记录](docs/issues/开发问题记录.md)

## 许可证

[MIT](LICENSE)
