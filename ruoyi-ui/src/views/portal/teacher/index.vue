<template>
  <!-- ============================================================================
       【功能】教师工作台首页（阶段8升级为教学仪表盘）
       ----------------------------------------------------------------------------
       【说明】欢迎语 + 班级切换器 + 教学总览统计卡（学生/小组/总稿提交率/
              黄橙红预警计数）+ 各组完成率横向条形图（echarts）+
              成绩分布柱状图（echarts）+ 「立即扫描预警」按钮（触发后
              WebSocket 实时推送在线组员）+ 预警记录表（可处置）+ 快捷入口。
       ============================================================================ -->
  <div class="page">
    <!-- 欢迎标题 + 班级切换 -->
    <div class="page-head head-flex">
      <div>
        <h2 class="page-title">您好，{{ userStore.nickName || userStore.name }} 老师</h2>
        <p class="page-sub">欢迎回到智能教学平台，祝教学顺利</p>
      </div>
      <el-select v-model="currentClassId" style="width: 220px" v-if="classList.length > 0">
        <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
    </div>

    <!-- 教学总览统计卡 -->
    <el-row :gutter="16" v-if="stats.className">
      <el-col :span="4" v-for="s in statCards" :key="s.label">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-num" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：各组完成率 + 成绩分布 -->
    <el-row :gutter="16" style="margin-top: 4px" v-if="stats.className">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="head-flex">
              <span class="card-title">各组完成率（已提交模块数 / 10）</span>
              <!-- 触发扫描：按规则判定并实时推送在线组员 -->
              <el-button type="danger" plain size="small" :loading="scanning" @click="doScan">
                立即扫描预警
              </el-button>
            </div>
          </template>
          <div ref="progressChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span class="card-title">成绩分布（已发布成绩）</span></template>
          <div ref="scoreChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警记录表 -->
    <el-card shadow="never" style="margin-top: 16px" v-if="stats.className">
      <template #header><span class="card-title">预警记录（未处置在前）</span></template>
      <el-table :data="records" size="small" empty-text="暂无预警记录，点击上方「立即扫描预警」开始检查">
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="小组" prop="groupName" width="90" />
        <el-table-column label="任务" prop="taskName" min-width="140" show-overflow-tooltip />
        <el-table-column label="预警内容" prop="content" min-width="300" show-overflow-tooltip />
        <el-table-column label="完成率" width="80">
          <template #default="{ row }">{{ row.progress }}%</template>
        </el-table-column>
        <el-table-column label="触发时间" prop="sendTime" width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.resolved === 1 ? 'info' : 'danger'" size="small">
              {{ row.resolved === 1 ? '已处置' : '未处置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.resolved === 0" size="small" link type="primary" @click="doResolve(row)">
              处置
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="classList.length === 0" description="暂无指导班级，请前往「班级与分组」创建" :image-size="90" />

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

<script setup name="TeacherHome">
// ============================================================================
// 【功能】教师工作台逻辑：班级切换 + 教学统计 + echarts 图表 +
//        预警扫描触发与记录处置 + 快捷入口
// ============================================================================
import * as echarts from 'echarts'
import { listClass } from '@/api/teach/portal'
import { teacherDashboard, scanWarning, listWarningRecords, resolveWarning } from '@/api/teach/dashboard'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 班级列表与当前选中班级
const classList = ref([])
const currentClassId = ref(null)
// 仪表盘统计与预警记录
const stats = ref({})
const records = ref([])
const scanning = ref(false)

// 图表 DOM 与实例
const progressChartRef = ref(null)
const scoreChartRef = ref(null)
let progressChart = null
let scoreChart = null

// 快捷入口配置
const quickLinks = [
  { path: '/teacher/class', title: '班级与分组', sub: '建班 / 名单导入 / 自动分组', icon: 'School', color: '#409EFF' },
  { path: '/teacher/task', title: '任务管理', sub: '发布任务并设置截止时间', icon: 'Document', color: '#67C23A' },
  { path: '/teacher/warning', title: '预警规则', sub: '黄橙红三级规则配置', icon: 'Bell', color: '#E6A23C' },
  { path: '/teacher/ai', title: 'AI批改', sub: '本地大模型批改与成绩发布', icon: 'Star', color: '#F56C6C' }
]

/** 统计卡配置（含预警分布取色） */
const statCards = computed(() => {
  const w = stats.value.warningDist || {}
  return [
    { label: '学生数', value: stats.value.studentCount ?? '-', color: '#2e4a6b' },
    { label: '小组数', value: stats.value.groupCount ?? '-', color: '#2e4a6b' },
    { label: '总稿提交率', value: (stats.value.submitRate ?? '-') + '%', color: '#67c23a' },
    { label: '黄色预警', value: w['1'] ?? 0, color: '#e6a23c' },
    { label: '橙色预警', value: w['2'] ?? 0, color: '#f0830a' },
    { label: '红色预警', value: w['3'] ?? 0, color: '#f56c6c' }
  ]
})

/** 预警级别文本与标签色 */
function levelText(level) {
  return { 1: '一般-黄', 2: '重要-橙', 3: '紧急-红' }[level] || '未知'
}
function levelTag(level) {
  return { 1: 'warning', 2: 'warning', 3: 'danger' }[level] || 'info'
}

/** 加载班级列表并默认选中第一个班 */
async function loadClass() {
  const res = await listClass()
  classList.value = res.data || []
  if (classList.value.length > 0 && !currentClassId.value) {
    currentClassId.value = classList.value[0].id
  }
}

/** 切换班级时刷新仪表盘与记录 */
watch(currentClassId, (v) => {
  if (v) refresh(v)
})

/** 刷新统计 + 预警记录 + 重绘图表 */
async function refresh(classId) {
  const [dashRes, recRes] = await Promise.all([teacherDashboard(classId), listWarningRecords(classId)])
  stats.value = dashRes.data || {}
  records.value = recRes.data || []
  await nextTick()
  renderProgressChart()
  renderScoreChart()
}

/** 各组完成率横向条形图 */
function renderProgressChart() {
  if (!progressChartRef.value) return
  if (!progressChart) progressChart = echarts.init(progressChartRef.value)
  const rows = stats.value.groupProgress || []
  progressChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 70, right: 40, top: 10, bottom: 24 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: rows.map((r) => r.groupName), inverse: true },
    series: [{
      type: 'bar',
      barWidth: 16,
      data: rows.map((r) => ({
        value: Math.min(100, (Number(r.submitted) || 0) * 10),
        itemStyle: { color: r.submitStatus === 1 ? '#67c23a' : '#409eff', borderRadius: [0, 8, 8, 0] }
      })),
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  }, true)
}

/** 成绩分布柱状图（四段） */
function renderScoreChart() {
  if (!scoreChartRef.value) return
  if (!scoreChart) scoreChart = echarts.init(scoreChartRef.value)
  const dist = stats.value.scoreDist || []
  const ranges = ['90-100', '80-89', '60-79', '60以下']
  const cntMap = {}
  for (const d of dist) cntMap[d.range] = Number(d.cnt) || 0
  scoreChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 16, bottom: 28 },
    xAxis: { type: 'category', data: ranges },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      barWidth: 34,
      data: ranges.map((r) => ({
        value: cntMap[r] || 0,
        itemStyle: { color: r === '90-100' ? '#67c23a' : r === '80-89' ? '#409eff' : r === '60-79' ? '#e6a23c' : '#f56c6c', borderRadius: [6, 6, 0, 0] }
      })),
      label: { show: true, position: 'top' }
    }]
  }, true)
}

/** 触发扫描：判定+落库+实时推送在线组员 */
async function doScan() {
  scanning.value = true
  try {
    const res = await scanWarning(currentClassId.value)
    const d = res.data || {}
    const triggered = d.triggered || []
    if (triggered.length === 0) {
      ElMessage.success('扫描完成：全部小组状态正常，无预警')
    } else {
      ElNotification({
        title: '扫描完成',
        message: `触发预警 ${triggered.length} 组（新增 ${d.newCount} / 刷新 ${d.refreshCount}），已实时提醒在线学生 ${d.pushCount} 人`,
        type: 'warning',
        duration: 6000
      })
    }
    refresh(currentClassId.value)
  } finally {
    scanning.value = false
  }
}

/** 处置预警记录 */
async function doResolve(row) {
  await resolveWarning(row.id)
  ElMessage.success('已处置')
  listWarningRecords(currentClassId.value).then((res) => { records.value = res.data || [] })
}

/** 窗口尺寸变化时自适应图表 */
function onResize() {
  progressChart && progressChart.resize()
  scoreChart && scoreChart.resize()
}

onMounted(() => {
  loadClass()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  progressChart && progressChart.dispose()
  scoreChart && scoreChart.dispose()
})
</script>

<style lang="scss" scoped>
/* ==================== 工作台样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.section-title { margin: 0 0 14px; font-size: 16px; color: #1d2b3a; }
.card-title { font-weight: 600; color: #1d2b3a; }
.head-flex { display: flex; justify-content: space-between; align-items: center; }

.stat-card {
  margin-bottom: 16px; text-align: center; border-radius: 10px;
  .stat-num { font-size: 24px; font-weight: 700; }
  .stat-label { font-size: 12px; color: #86909c; margin-top: 4px; }
}

.chart-box { height: 260px; width: 100%; }

.quick-card {
  cursor: pointer; border-radius: 10px;
  .quick-title { font-size: 15px; font-weight: 600; color: #1d2b3a; margin-top: 10px; }
  .quick-sub { font-size: 12px; color: #86909c; margin-top: 4px; }
}
</style>
