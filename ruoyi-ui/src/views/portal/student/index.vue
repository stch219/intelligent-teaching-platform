<template>
  <!-- ============================================================================
       【功能】学生工作台首页
       ----------------------------------------------------------------------------
       【说明】展示欢迎语 + 我的课程设计信息卡（班级/小组/角色/指导教师/
              截止倒计时）+ 10大模块进度概览（进度条，可点击跳转编辑） +
              快捷入口。
       ============================================================================ -->
  <div class="page">
    <!-- 欢迎标题 -->
    <div class="page-head">
      <h2 class="page-title">您好，{{ userStore.nickName || userStore.name }} 同学</h2>
      <p class="page-sub">欢迎回到智能教学平台，开始你的课程设计之旅</p>
    </div>

    <!-- 学习状态统计卡（阶段8仪表盘：模块完成度/贡献率确认/未读/预警/成绩） -->
    <div class="stat-row" v-if="stats.inGroup">
      <div class="stat-cell" v-for="s in statCards" :key="s.label">
        <div class="stat-num" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 未处置预警横幅（阶段8三级预警：黄/橙/红染色展示） -->
    <div v-for="w in openWarnings" :key="w.id" class="warn-banner" :class="'level-' + w.level">
      <el-tag :type="levelTag(w.level)" size="small" effect="dark">{{ levelText(w.level) }}预警</el-tag>
      <span class="warn-text">{{ w.content }}</span>
      <span class="warn-time">{{ w.sendTime }}</span>
    </div>

    <!-- 我的信息卡 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="info-card">
          <template #header><span class="card-title">我的课程设计</span></template>
          <el-descriptions :column="2" border v-if="info.studentNo">
            <el-descriptions-item label="学号">{{ info.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ info.nickName }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ info.className || '未编班' }}</el-descriptions-item>
            <el-descriptions-item label="小组">{{ info.groupName || '未分组' }}</el-descriptions-item>
            <el-descriptions-item label="组内角色">
              <el-tag :type="roleTagType(info.roleType)" size="small">{{ roleText(info.roleType) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="指导教师">{{ info.teacherName || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="学生档案加载中或未编班分组" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="info-card deadline-card">
          <template #header><span class="card-title">任务截止</span></template>
          <div class="deadline-box">
            <div class="deadline-days" :class="{ urgent: daysLeft(info.deadline) < 7 && info.deadline }">
              {{ deadlineText(info.deadline) }}
            </div>
            <div class="deadline-time">{{ info.deadline || '教师尚未设置截止时间' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模块进度概览 -->
    <div class="page-head" style="margin-top: 8px">
      <h3 class="section-title">模块进度概览（点击模块直接编辑）</h3>
    </div>
    <el-card shadow="never" v-if="modules.length > 0">
      <el-row :gutter="12">
        <el-col :span="8" v-for="m in modules" :key="m.moduleCode" style="margin-bottom: 14px">
          <div class="module-item" @click="router.push('/student/modules?code=' + m.moduleCode)">
            <div class="module-head">
              <span class="module-name">{{ m.moduleCode }}. {{ m.moduleName }}</span>
              <span class="module-status">
                <el-tag size="small" :type="statusTagType(m.status)">{{ statusText(m.status) }}</el-tag>
              </span>
            </div>
            <el-progress :percentage="Number(m.progress) || 0" :stroke-width="8"
                         :status="Number(m.status) === 2 ? 'success' : undefined" />
            <div class="module-foot">
              <span v-if="Number(m.countType) === 1">字数 {{ m.wordCount || 0 }}/{{ m.minCount }}~{{ m.maxCount || '不限' }}</span>
              <span v-else>条目 {{ m.itemCount || 0 }}/{{ m.minCount }}~{{ m.maxCount || '不限' }}</span>
              <span v-if="m.score != null" class="module-score">{{ m.score }}分</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
    <el-empty v-else description="教师尚未设置模块，暂无进度数据" :image-size="90" />

    <!-- 快捷入口 -->
    <div class="page-head" style="margin-top: 8px">
      <h3 class="section-title">快捷入口</h3>
    </div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="q in quickLinks" :key="q.path" style="margin-bottom: 16px">
        <el-card class="quick-card" shadow="hover" @click="router.push(q.path)">
          <el-icon :size="26" :color="q.color"><component :is="q.icon" /></el-icon>
          <div class="quick-title">{{ q.title }}</div>
          <div class="quick-sub">{{ q.sub }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="StudentHome">
// ============================================================================
// 【功能】学生工作台逻辑：学习状态统计 + 预警横幅 + 我的信息 + 截止倒计时 +
//        模块进度概览 + 快捷入口 + WebSocket 实时预警提醒（阶段8升级）
// ============================================================================
import { getMyInfo, listModules } from '@/api/teach/studentPortal'
import { studentDashboard } from '@/api/teach/dashboard'
import { connectWs, onWsMsg, closeWs } from '@/utils/websocket'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 我的信息与模块看板数据
const info = ref({})
const modules = ref([])
// 学习状态统计（阶段8仪表盘：含本组预警列表）
const stats = ref({})

// 快捷入口配置
const quickLinks = [
  { path: '/student/profile', title: '个人中心', sub: '选角色 / 填分工 / 改密码', icon: 'User', color: '#409EFF' },
  { path: '/student/board', title: '公示板', sub: '任务 / 组员 / 资料一览', icon: 'Postcard', color: '#67C23A' },
  { path: '/student/modules', title: '模块编辑', sub: '10大模块协同编辑提交', icon: 'EditPen', color: '#E6A23C' },
  { path: '/student/contribution', title: '贡献率', sub: '组内分配与确认（和为100%）', icon: 'DataAnalysis', color: '#F56C6C' }
]

/** 统计卡配置（inGroup 为 true 时展示） */
const statCards = computed(() => [
  { label: '模块完成', value: (stats.value.moduleSubmitted ?? '-') + '/' + (stats.value.moduleTotal ?? 10), color: '#409eff' },
  { label: '贡献率确认', value: stats.value.contributionConfirmed === 1 ? '已确认' : '未确认', color: stats.value.contributionConfirmed === 1 ? '#67c23a' : '#e6a23c' },
  { label: '未读消息', value: stats.value.unreadCount ?? '-', color: '#909399' },
  { label: '未处置预警', value: stats.value.warningCount ?? '-', color: (stats.value.warningCount || 0) > 0 ? '#f56c6c' : '#67c23a' },
  { label: '最终成绩', value: stats.value.finalScore != null ? stats.value.finalScore : '未发布', color: '#e6a23c' }
])

/** 未处置预警列表（横幅数据源） */
const openWarnings = computed(() => (stats.value.warnings || []).filter(w => w.resolved === 0))

/** 预警级别文本（1黄 2橙 3红） */
function levelText(level) {
  return { 1: '一般-黄', 2: '重要-橙', 3: '紧急-红' }[level] || '未知'
}
/** 预警级别标签颜色 */
function levelTag(level) {
  return { 1: 'warning', 2: 'warning', 3: 'danger' }[level] || 'info'
}

/** 加载我的信息与模块看板 */
async function load() {
  const [infoRes, moduleRes] = await Promise.all([getMyInfo(), listModules()])
  info.value = infoRes.data || {}
  modules.value = moduleRes.data || []
}

/** 加载学习状态统计（模块完成度/确认状态/未读/预警/成绩） */
async function loadStats() {
  try {
    const res = await studentDashboard()
    stats.value = res.data || {}
  } catch (ignored) {}
}

/** 角色文本 */
function roleText(roleType) {
  const map = { 1: '组长', 2: '汇报人', 3: '组长兼汇报人', 4: '成员' }
  return map[roleType] || '未选择'
}
/** 角色标签类型 */
function roleTagType(roleType) {
  const map = { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[roleType] || 'info'
}
/** 模块状态文本（0未开展 1暂存 2已提交） */
function statusText(status) {
  const map = { 0: '未开展', 1: '暂存', 2: '已提交' }
  return map[Number(status)] || '未开展'
}
/** 模块状态标签类型 */
function statusTagType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[Number(status)] || 'info'
}
/** 距截止剩余天数 */
function daysLeft(deadline) {
  if (!deadline) return 999
  return Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
}
/** 截止提示文本 */
function deadlineText(deadline) {
  if (!deadline) return '--'
  const d = daysLeft(deadline)
  if (d < 0) return '已截止'
  return '剩 ' + d + ' 天'
}

// 建立长连接并订阅预警推送：教师端扫描后本组命中会实时收到 warning 报文
let offWs = null
onMounted(() => {
  connectWs()
  offWs = onWsMsg((msg) => {
    if (msg.type === 'warning') {
      // 弹出实时预警通知（红色预警用 error 样式，不自动关闭）
      ElNotification({
        title: '收到' + levelText(msg.level) + '预警',
        message: msg.content,
        type: Number(msg.level) === 3 ? 'error' : 'warning',
        duration: 0
      })
      loadStats() // 刷新统计卡与预警横幅
    }
  })
})
onUnmounted(() => {
  if (offWs) offWs() // 解绑消息回调
  closeWs()          // 离开工作台主动断开长连接
})

load()
loadStats()
</script>

<style lang="scss" scoped>
/* ==================== 学生工作台样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.section-title { margin: 0 0 14px; font-size: 16px; color: #1d2b3a; }

/* 学习状态统计行：5 等分弹性卡片 */
.stat-row {
  display: flex; gap: 16px; margin-bottom: 16px;

  .stat-cell {
    flex: 1; text-align: center; padding: 14px 0;
    background: #fff; border: 1px solid #ebeef5; border-radius: 10px;

    .stat-num { font-size: 22px; font-weight: 700; }
    .stat-label { font-size: 12px; color: #86909c; margin-top: 4px; }
  }
}

/* 未处置预警横幅：按级别染左侧色条 */
.warn-banner {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; margin-bottom: 10px;
  border-radius: 8px; border: 1px solid #ebeef5; background: #fff;

  &.level-1 { border-left: 4px solid #e6a23c; background: #fdf6ec; }
  &.level-2 { border-left: 4px solid #f56c6c; background: #fef0f0; }
  &.level-3 { border-left: 4px solid #f56c6c; background: #fef0f0; }
  .warn-text { flex: 1; font-size: 13px; color: #303133; }
  .warn-time { font-size: 12px; color: #86909c; }
}


.info-card { border-radius: 10px; }
.card-title { font-weight: 600; color: #1d2b3a; }

.deadline-card {
  border-radius: 10px;
  .deadline-box { text-align: center; padding: 10px 0; }
  .deadline-days { font-size: 34px; font-weight: 700; color: #2e4a6b; }
  .deadline-days.urgent { color: #f56c6c; }
  .deadline-time { margin-top: 8px; font-size: 13px; color: #86909c; }
}

/* 模块进度单元（可点击） */
.module-item {
  border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15); }
  .module-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
  .module-name { font-size: 13px; font-weight: 600; color: #1d2b3a; }
  .module-foot { display: flex; justify-content: space-between; margin-top: 6px; font-size: 12px; color: #86909c; }
  .module-score { color: #e6a23c; font-weight: 600; }
}

.quick-card {
  cursor: pointer; border-radius: 10px;
  .quick-title { font-size: 15px; font-weight: 600; color: #1d2b3a; margin-top: 10px; }
  .quick-sub { font-size: 12px; color: #86909c; margin-top: 4px; }
}
</style>
