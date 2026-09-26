<template>
  <!-- ============================================================================
       【功能】教师工作台首页
       ----------------------------------------------------------------------------
       【说明】展示欢迎语 + 指导班级概览卡片（学生数/组数/截止倒计时，
              不足7天标红）+ 各功能快捷入口。
       ============================================================================ -->
  <div class="page">
    <!-- 欢迎标题 -->
    <div class="page-head">
      <h2 class="page-title">您好，{{ userStore.nickName || userStore.name }} 老师</h2>
      <p class="page-sub">欢迎回到智能教学平台，祝教学顺利</p>
    </div>

    <!-- 班级概览卡片 -->
    <div class="page-head" style="margin-top: 8px">
      <h3 class="section-title">我的指导班级</h3>
    </div>
    <el-row :gutter="16" v-if="classList.length > 0">
      <el-col :span="8" v-for="c in classList" :key="c.id" style="margin-bottom: 16px">
        <el-card class="class-card" shadow="hover" @click="goClass(c)">
          <div class="card-head">
            <span class="class-name">{{ c.className }}</span>
            <!-- 截止倒计时（不足7天标红） -->
            <span class="deadline" :class="{ urgent: daysLeft(c.deadline) < 7 && c.deadline }">
              {{ deadlineText(c.deadline) }}
            </span>
          </div>
          <div class="card-stats">
            <div class="stat"><div class="num">{{ c.studentCount }}</div><div class="label">学生</div></div>
            <div class="stat"><div class="num">{{ c.groupCount }}</div><div class="label">小组</div></div>
            <div class="stat"><div class="num">{{ taskCount(c.id) }}</div><div class="label">任务</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-else description="暂无指导班级，请前往「班级与分组」创建" :image-size="90" />

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
// 【功能】教师工作台逻辑：班级概览 + 截止倒计时 + 快捷入口
// ============================================================================
import { listClass } from '@/api/teach/portal'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 班级列表与任务数（任务数按班级分组统计）
const classList = ref([])
const taskMap = ref({})
const loading = ref(false)

// 快捷入口配置
const quickLinks = [
  { path: '/teacher/class', title: '班级与分组', sub: '建班 / 名单导入 / 自动分组', icon: 'School', color: '#409EFF' },
  { path: '/teacher/task', title: '任务管理', sub: '发布 ABCD 任务并分配到组', icon: 'Document', color: '#67C23A' },
  { path: '/teacher/module', title: '模块设置', sub: '设置各模块字数区间', icon: 'Setting', color: '#E6A23C' },
  { path: '/teacher/score', title: '模块赋分', sub: '六大模块满分合计 100 分', icon: 'TrophyBase', color: '#F56C6C' }
]

/** 加载班级列表（含任务数统计） */
async function loadClass() {
  loading.value = true
  try {
    const res = await listClass()
    classList.value = res.data || []
    // 汇总各班学生数（仅用于工作台展示）
  } finally {
    loading.value = false
  }
}

/** 计算距截止时间剩余天数 */
function daysLeft(deadline) {
  if (!deadline) return 999
  return Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
}

/** 截止时间提示文本 */
function deadlineText(deadline) {
  if (!deadline) return '未设置截止'
  const d = daysLeft(deadline)
  if (d < 0) return '已截止'
  return '剩 ' + d + ' 天截止'
}

/** 该班任务数（占位0，任务统计在任务页展示；工作台简化显示0） */
function taskCount(classId) {
  return taskMap.value[classId] || 0
}

/** 点击班级卡片跳转班级与分组页 */
function goClass(c) {
  router.push({ path: '/teacher/class', query: { classId: c.id } })
}

loadClass()
</script>

<style lang="scss" scoped>
/* ==================== 工作台样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.section-title { margin: 0 0 14px; font-size: 16px; color: #1d2b3a; }

.class-card {
  cursor: pointer;
  border-radius: 10px;
  .card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
  .class-name { font-size: 16px; font-weight: 600; color: #1d2b3a; }
  .deadline { font-size: 12px; color: #86909c; }
  .deadline.urgent { color: #f56c6c; font-weight: 600; }
  .card-stats { display: flex; justify-content: space-around; }
  .stat { text-align: center; }
  .num { font-size: 20px; font-weight: 700; color: #2e4a6b; }
  .label { font-size: 12px; color: #86909c; }
}

.quick-card {
  cursor: pointer;
  border-radius: 10px;
  .quick-title { font-size: 15px; font-weight: 600; color: #1d2b3a; margin-top: 10px; }
  .quick-sub { font-size: 12px; color: #86909c; margin-top: 4px; }
}
</style>
