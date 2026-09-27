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
// 【功能】学生工作台逻辑：我的信息 + 截止倒计时 + 模块进度概览 + 快捷入口
// ============================================================================
import { getMyInfo, listModules } from '@/api/teach/studentPortal'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 我的信息与模块看板数据
const info = ref({})
const modules = ref([])

// 快捷入口配置
const quickLinks = [
  { path: '/student/profile', title: '个人中心', sub: '选角色 / 填分工 / 改密码', icon: 'User', color: '#409EFF' },
  { path: '/student/board', title: '公示板', sub: '任务 / 组员 / 资料一览', icon: 'Postcard', color: '#67C23A' },
  { path: '/student/modules', title: '模块编辑', sub: '10大模块协同编辑提交', icon: 'EditPen', color: '#E6A23C' },
  { path: '/student/contribution', title: '贡献率', sub: '组内分配与确认（和为100%）', icon: 'DataAnalysis', color: '#F56C6C' }
]

/** 加载我的信息与模块看板 */
async function load() {
  const [infoRes, moduleRes] = await Promise.all([getMyInfo(), listModules()])
  info.value = infoRes.data || {}
  modules.value = moduleRes.data || []
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

load()
</script>

<style lang="scss" scoped>
/* ==================== 学生工作台样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.section-title { margin: 0 0 14px; font-size: 16px; color: #1d2b3a; }

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
