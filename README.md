# 智能教学平台（Intelligent Teaching Platform）

> 基于 RuoYi-Vue3 的《汽车理论》项目制课程设计全过程管理平台，集成本地多模态大模型实现 AI 辅助批改与智能答疑。

## 项目简介

本平台面向高校项目制课程设计场景，覆盖**教师建班分组 → 学生在线协同完成课程设计 → AI 辅助批改 → 教师终审发布成绩**的完整闭环，支持三种角色协同工作。

> **界面风格**：三端（管理员/教师/学生）统一品牌化侧边栏布局（深蓝主题 + 学士帽 Logo），登录后按角色自动跳转对应工作台；管理员可见全部菜单，教师仅见「课程设计管理」目录。

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
- AI 辅助批改（阶段7）：本地多模态大模型逐模块批改说明书 → 教师核查三段式评语并核定终分 → 成绩汇总发布
- 成绩判分：模块得分 = 模块满分 × 生效分%（AI分/教师核定分），个人得分 = 模块得分 × 本人贡献率，一键发布后学生端可见

### 学生端
- 注册审批制：填写班级/学号注册，管理员审批后登录
- 角色选定：组长 / 汇报人 / 组长兼汇报人 / 成员（四选一，同组唯一性校验）
- 10 大课程设计模块在线协同编辑：封面、任务要求、角色与分工、参数配置、知识背景、
  计算步骤、代码实现、计算结果与分析、心得体会、参考资料
- 富文本编辑：支持图片上传/粘贴、公式录入（KaTeX）、代码块、字数实时校验
- 贡献率分配：勾选模块完成人并分配贡献率（合计 100%）
- 说明书总稿：一键生成含目录页的 PDF 终稿，组长合规预检后提交
- 实时通讯：微信群式组内群聊、师生单聊（已读/未读回执）、模块讨论话题
- 我的成绩（阶段7）：教师发布后查看小组/个人最终得分与模块得分明细（含生效分、贡献率、本模块个人得分折算）

### 系统引擎
- 封面自动生成（学院、姓名、组长、汇报人、指导教师、日期自动汇聚）
- 提交合规性预检（全员有分工、组长/汇报人唯一、贡献率合计 100%、必填完整）
- AI 批改流水线（阶段7）：组装模块内容（富文本转纯文本 + 提取内嵌图片）→ 本地多模态大模型批改输出三段式评语JSON（亮点/问题/建议+百分制分数）→ 引擎不可用时自动降级本地规则批改兜底 → 教师核查终审 → 成绩汇总发布
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
| AI 推理 | transformers + Qwen2.5-VL-3B-Instruct（多模态，GPU/CPU 自适应降级） | - |
| AI 服务 | Python 标准库 http.server（零框架依赖，独立进程端口 8300） | - |
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
mysql -u root -p < sql/itp_menu.sql   # 角色（教师/学生）与管理员端菜单
# （仅老库升级时执行 sql/itp_phase7.sql 增量脚本：te_ai_review 补 teacher_score 列，新库已含可跳过）

# 2. 修改后端配置 ruoyi-admin/src/main/resources/application-druid.yml（数据库密码）
#    及 application.yml（Redis 配置）

# 3. 启动后端
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 4. 启动前端
cd ruoyi-ui
npm install
npm run dev

# 5.（可选）启动 AI 批改引擎（不开也能用：Java 端自动降级本地规则批改）
cd ai_engine
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
# 首次使用：用魔搭（国内直连免代理）预先下载模型权重到本地目录（约7GB，仅一次）
modelscope download --model Qwen/Qwen2.5-VL-3B-Instruct --local_dir models/qwen2.5-vl-3b
python ai_server.py    # 端口 8300；检测到本地模型目录即离线加载，无需联网
```

> **AI 引擎硬件自适应降级链**（`ai_engine/ai_server.py` 自动探测，无需配置）：
> ① 显存 ≥ 8GB → bf16 全精度 GPU 推理（推荐 RTX 5070 Ti 12GB，批改一个模块约 10~30 秒）
> ② 有 GPU 但显存不足 → 4bit 量化加载（需 `pip install bitsandbytes`）
> ③ 无 GPU → CPU 推理（慢，约 2~5 分钟/模块，任何机器可跑）
> ④ Python 服务完全不可用 → Java 端本地规则批改兜底（零依赖，按字数/结构/代码块估算分数并生成评语），保证批改流程永不中断。
>
> **⚠️ NVIDIA 显卡用户必读**：`pip install torch` 默认装的是 **CPU 版**（`+cpu` 后缀），
> 装了它即使有显卡也只会走 CPU 推理。有显卡请先换装 CUDA 版（Blackwell 新卡如 5070 Ti 用 cu128，老卡用 cu121）。
> **注意必须锁定版本号**：官方 cu128 源最高只有 `torch 2.11.0+cu128`，不带版本号时 pip 会解析到更新的 2.14 版、
> 在 CUDA 源上找不到而绕回清华源的 CPU 版（实测踩坑）：
>
> ```bash
> # 5070 Ti / 5080 / 5090 等新一代显卡（CUDA 12.8，走阿里镜像国内直连，无需代理）
> pip install torch==2.11.0+cu128 torchvision==0.26.0+cu128 -f https://mirrors.aliyun.com/pytorch-wheels/cu128/ -i https://pypi.tuna.tsinghua.edu.cn/simple
> # GTX/RTX 20~40 系显卡（CUDA 12.1，同样锁定版本号）
> pip install torch==2.4.1+cu121 torchvision==0.19.1+cu121 -f https://mirrors.aliyun.com/pytorch-wheels/cu121/ -i https://pypi.tuna.tsinghua.edu.cn/simple
> # 验证：版本带 +cu128 且输出 True 才会用 GPU
> python -c "import torch; print(torch.__version__, torch.cuda.is_available())"
> ```
>
> 若 pip 下载中途断连挂起（大文件易发生），可用 BITS 断点续传手动下载 wheel 后本地安装：
>
> ```powershell
> # BITS 支持断点续传，中断后重跑同一命令会自动续传
> Start-BitsTransfer -Source 'https://mirrors.aliyun.com/pytorch-wheels/cu128/torch-2.11.0%2Bcu128-cp313-cp313-win_amd64.whl' -Destination '.\torch-2.11.0+cu128-cp313-cp313-win_amd64.whl'
> pip install --force-reinstall '.\torch-2.11.0+cu128-cp313-cp313-win_amd64.whl' -i https://pypi.tuna.tsinghua.edu.cn/simple
> ```

访问 http://localhost:80（前端开发端口见 vite 配置），默认管理员账号 `admin / admin123`。

## 开发计划

- [x] 阶段0：环境准备 + 仓库初始化 + 若依基线跑通
- [x] 阶段1：数据库业务表设计 + 代码生成
- [x] 阶段2：管理员端
- [x] 阶段3：教师端（独立门户 /teacher：班级分组 / 任务分配 / 模块设置 / 资料发布 / 预警规则 / 模块赋分）
- [x] 阶段4：学生端（独立门户 /student：工作台 / 个人中心角色分工 / 公示板 / 10大模块协同编辑（字数条目校验+乐观锁防覆盖） / 贡献率分配与确认 / 组长分工确认）
- [x] 阶段5：系统引擎（封面自动生成（模块1系统合成只读展示） / 总稿合规预检4项（组长身份·模块全提交·分工全确认·贡献率分配并全员确认） / 一键提交总稿（封面落库锁定·小组置已提交·组长分工自动确认） / 总稿PDF导出（封面页+目录页+10模块正文·中文字体嵌入·页脚页码））
- [x] 阶段6：消息系统（WebSocket 实时单聊/组内群聊（token 握手认证·心跳保活·断线重连·同账号顶替） / 已读未读回执（进入会话即读·导航未读角标） / 可联系人发起单聊（学生=指导教师+组员·教师=本班学生） / 模块讨论话题（学生发起本组话题·教师查看本班并回复·楼中楼指向） / 班级公告（教师发布·归属校验·在线学生 WebSocket 实时提醒） / 师生共用消息中心/讨论/公告三大页面）
- [x] 阶段7：AI 批改 + 成绩判分（本地多模态大模型批改（transformers + Qwen2.5-VL-3B·GPU/CPU 硬件自适应降级·引擎不可用规则兜底） / 三段式评语（亮点-问题-建议） / 教师核查核定终分 / 成绩判分汇总（模块得分=满分×生效分%·个人得分=模块得分×贡献率%） / 一键发布（重复发布自动撤回旧成绩） / 教师端 AI 批改工作台 / 学生端「我的成绩」分数卡+模块明细）——实测 RTX 5070 Ti 12GB 命中降级链第①级 bf16：4.3 秒/模块（CPU 约 2~5 分钟），6 模块 26 秒完成批改+落库+成绩折算全链路
- [ ] 阶段8：仪表盘、三级预警、帮助中心
- [ ] 阶段9：测试、文档终稿、发布

## 文档

- [需求文档](docs/requirements/)
- [开发问题记录](docs/issues/开发问题记录.md)

## 许可证

[MIT](LICENSE)
