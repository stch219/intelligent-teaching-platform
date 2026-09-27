<template>
  <!-- ============================================================================
       【功能】学生端公示板
       ----------------------------------------------------------------------------
       【说明】班级/小组信息公开一览：①我的任务卡（题目/目标/要求/评分标准/
              提示 折叠展开）；②组员分工一览表（角色/分工/组长确认状态，
              组长可在此确认组员分工）；③教师发布的可见资料（可下载）。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">公示板</h2>
      <p class="page-sub">{{ info.className || '' }} · {{ info.groupName || '未分组' }} · 指导教师：{{ info.teacherName || '-' }}</p>
    </div>

    <el-row :gutter="16">
      <!-- 我的任务 -->
      <el-col :span="14">
        <el-card shadow="never" class="board-card">
          <template #header><span class="card-title">我的任务（{{ tasks.length }}）</span></template>
          <el-empty v-if="tasks.length === 0" description="教师尚未给本组分配任务" :image-size="90" />
          <el-collapse v-else v-model="activeTasks">
            <el-collapse-item v-for="t in tasks" :key="t.id" :name="t.id">
              <template #title>
                <el-tag type="primary" size="small" style="margin-right: 8px">任务 {{ t.taskCode }}</el-tag>
                <span class="task-name">{{ t.taskName }}</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="设计目标">{{ t.designGoal || '-' }}</el-descriptions-item>
                <el-descriptions-item label="具体要求">{{ t.requirement || '-' }}</el-descriptions-item>
                <el-descriptions-item label="评分标准">{{ t.gradingStandard || '-' }}</el-descriptions-item>
                <el-descriptions-item label="重要提示">
                  <span style="color: #e6a23c">{{ t.tips || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="分配时间">{{ t.assignTime || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>

      <!-- 组员分工一览 -->
      <el-col :span="10">
        <el-card shadow="never" class="board-card">
          <template #header>
            <span class="card-title">组员分工一览（{{ members.length }}人）</span>
          </template>
          <el-table :data="members" size="small" border>
            <el-table-column label="学号" prop="studentNo" width="90" />
            <el-table-column label="姓名" prop="nickName" width="80" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">
                <el-tag :type="roleTagType(row.roleType)" size="small">{{ roleText(row.roleType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分工说明" prop="dutyAssignment" show-overflow-tooltip />
            <el-table-column label="确认状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.dutyStatus === 1" type="success" size="small">已确认</el-tag>
                <template v-else>
                  <!-- 组长可以确认组员分工 -->
                  <el-button v-if="isLeader && row.userId !== myUserId && row.dutyAssignment"
                             type="primary" link size="small" @click="doConfirmDuty(row)">确认</el-button>
                  <el-tag v-else type="warning" size="small">待确认</el-tag>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 学习资料 -->
        <el-card shadow="never" class="board-card" style="margin-top: 16px">
          <template #header><span class="card-title">学习资料（{{ materials.length }}）</span></template>
          <el-empty v-if="materials.length === 0" description="暂无可见资料" :image-size="80" />
          <div v-else>
            <div class="material-item" v-for="m in materials" :key="m.id">
              <el-tag size="small" :type="materialTagType(m.materialType)">{{ materialText(m.materialType) }}</el-tag>
              <span class="material-name">{{ m.fileName }}</span>
              <el-button type="primary" link size="small"
                         @click="downloadMaterial(m)">下载</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="StudentBoard">
// ============================================================================
// 【功能】公示板逻辑：任务/组员/资料加载 + 组长分工确认 + 资料下载
// ============================================================================
import { getBoard, confirmDuty } from '@/api/teach/studentPortal'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const route = useRoute()
const userStore = useUserStore()

// 公示板聚合数据
const info = ref({})
const tasks = ref([])
const members = ref([])
const materials = ref([])
// 默认展开第一个任务
const activeTasks = ref([])

// 我的用户ID与是否组长
const myUserId = computed(() => userStore.userId)
const isLeader = computed(() => {
  const r = Number(info.value.roleType)
  return r === 1 || r === 3
})

/** 加载公示板数据 */
async function load() {
  const res = await getBoard()
  info.value = res.data.info || {}
  tasks.value = res.data.tasks || []
  members.value = res.data.members || []
  materials.value = res.data.materials || []
  if (tasks.value.length > 0) activeTasks.value = [tasks.value[0].id]
}

/** 组长确认组员分工 */
async function doConfirmDuty(row) {
  await proxy.$modal.confirm(`确认「${row.nickName}」填报的分工？`)
  await confirmDuty(row.userId)
  proxy.$modal.msgSuccess('已确认')
  load()
}

/** 资料下载（走通用文件流接口） */
function downloadMaterial(m) {
  proxy.download(m.filePath, {}, m.fileName)
}

/** 角色文本与标签类型 */
function roleText(roleType) {
  const map = { 1: '组长', 2: '汇报人', 3: '组长兼汇报人', 4: '成员' }
  return map[roleType] || '未选择'
}
function roleTagType(roleType) {
  const map = { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[roleType] || 'info'
}
/** 资料类型文本与标签 */
function materialText(type) {
  const map = { 1: '任务指导书', 2: '说明书模板', 3: '参考答案', 4: '辅助资料' }
  return map[type] || '资料'
}
function materialTagType(type) {
  const map = { 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' }
  return map[type] || 'info'
}

load()
</script>

<style lang="scss" scoped>
/* ==================== 公示板样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.board-card { border-radius: 10px; margin-bottom: 2px; }
.card-title { font-weight: 600; color: #1d2b3a; }
.task-name { font-weight: 600; color: #1d2b3a; }

.material-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 4px; border-bottom: 1px dashed #ebeef5;
  &:last-child { border-bottom: none; }
  .material-name { flex: 1; font-size: 13px; color: #1d2b3a; }
}
</style>
