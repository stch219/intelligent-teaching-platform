<template>
  <!-- ============================================================================
       【功能】组内贡献率分配与确认页
       ----------------------------------------------------------------------------
       【说明】模块5-10的组内贡献率矩阵（成员×模块）：
              · 组长：编辑各成员各模块的贡献率，每模块合计必须=100%，
                保存后组员确认状态重置为未确认；
              · 组员：查看分配结果并一键确认；
              · 表格底部实时显示每模块合计与是否达标（绿色=100%）。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">贡献率分配</h2>
      <p class="page-sub">模块5-10需分配组内贡献率，每模块合计必须为100%；提交预检要求全体组员确认</p>
    </div>

    <el-card shadow="never" class="panel-card" v-if="members.length > 0">
      <template #header>
        <div class="panel-head">
          <span class="card-title">贡献率矩阵（%）</span>
          <div>
            <!-- 组员一键确认 -->
            <el-button v-if="!isLeader" type="success" :disabled="myConfirmed || rows.length === 0"
                       @click="doConfirm">
              {{ myConfirmed ? '已确认全部模块' : '我已知晓并确认贡献率' }}
            </el-button>
            <!-- 组长保存分配 -->
            <el-button v-else type="primary" :loading="saving" @click="doSave">保存分配</el-button>
          </div>
        </div>
      </template>

      <el-table :data="matrixRows" border size="small">
        <!-- 首列：模块名+赋分 -->
        <el-table-column label="模块" width="220" fixed>
          <template #default="{ row }">
            <span class="module-cell">{{ row.moduleCode }}. {{ row.moduleName }}</span>
            <el-tag v-if="row.score != null" type="warning" size="small" style="margin-left: 6px">{{ row.score }}分</el-tag>
          </template>
        </el-table-column>
        <!-- 成员列：贡献率输入（仅组长可编辑） -->
        <el-table-column v-for="mem in members" :key="mem.userId" :label="mem.nickName" min-width="120" align="center">
          <template #header>
            {{ mem.nickName }}
            <div class="mem-no">{{ mem.studentNo }}</div>
          </template>
          <template #default="{ row }">
            <el-input-number v-if="isLeader" v-model="form[row.moduleCode][mem.userId]" :min="0" :max="100"
                             :precision="2" :controls="false" size="small" style="width: 90px" />
            <span v-else>{{ form[row.moduleCode][mem.userId] || 0 }}%</span>
          </template>
        </el-table-column>
        <!-- 合计列 -->
        <el-table-column label="合计" width="110" align="center">
          <template #default="{ row }">
            <span :class="sumOf(row.moduleCode) === 100 ? 'sum-ok' : 'sum-bad'">{{ sumOf(row.moduleCode) }}%</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 组员确认状态一览 -->
      <div class="confirm-bar">
        <span class="card-title" style="font-size: 13px">确认状态：</span>
        <el-tag v-for="mem in members" :key="mem.userId" size="small"
                :type="memberConfirmed(mem.userId) ? 'success' : 'warning'" style="margin-right: 8px">
          {{ mem.nickName }} {{ memberConfirmed(mem.userId) ? '已确认' : '未确认' }}
        </el-tag>
        <span class="confirm-tip" v-if="isLeader">重新保存分配后，组员需重新确认</span>
      </div>
    </el-card>
    <el-empty v-else description="你尚未分组或组长尚未分配贡献率" :image-size="100" />
  </div>
</template>

<script setup name="StudentContribution">
// ============================================================================
// 【功能】贡献率逻辑：面板加载 → 矩阵初始化 → 合计校验 → 组长保存/组员确认
// ============================================================================
import { getContributionPanel, saveContribution, confirmContribution } from '@/api/teach/studentPortal'

const { proxy } = getCurrentInstance()

// 面板数据：组员/分配行/赋分/是否组长
const members = ref([])
const rows = ref([])
const scores = ref({})
const isLeader = ref(false)
const saving = ref(false)

// 表单矩阵：{ 模块编号: { 用户ID: 贡献率 } }（模块5-10）
const form = reactive({})

// 矩阵行（模块5-10，含模块名与赋分）
const matrixRows = computed(() => {
  return [5, 6, 7, 8, 9, 10].map(code => ({
    moduleCode: code,
    moduleName: moduleNameOf(code),
    score: scores.value[code] ?? null
  }))
})

// 我是否已确认全部模块（本组存在我的行且全部 confirmed=1）
const myConfirmed = computed(() => {
  const mine = rows.value.filter(r => r.userId === currentUserId())
  return mine.length > 0 && mine.every(r => Number(r.confirmed) === 1)
})

// 当前登录用户ID
function currentUserId() {
  return Number(useUserStore().userId)
}

/** 模块名（全平台统一顺序） */
function moduleNameOf(code) {
  const names = { 1: '封面', 2: '任务要求', 3: '角色与分工', 4: '参数配置', 5: '知识背景', 6: '计算步骤', 7: '代码实现', 8: '计算结果与分析', 9: '心得体会', 10: '参考资料' }
  return names[code] || ('模块' + code)
}

/** 加载贡献率面板并初始化矩阵 */
async function load() {
  const res = await getContributionPanel()
  members.value = res.data.members || []
  rows.value = res.data.rows || []
  isLeader.value = !!res.data.isLeader
  // 赋分映射（模块编号→满分）
  scores.value = {}
  for (const s of (res.data.scores || [])) {
    scores.value[Number(s.moduleCode)] = Number(s.score)
  }
  // 初始化矩阵：已有分配行取现值，否则置0
  for (const row of matrixRows.value) {
    if (!form[row.moduleCode]) form[row.moduleCode] = {}
    for (const mem of members.value) {
      const exist = rows.value.find(r => Number(r.moduleCode) === row.moduleCode && Number(r.userId) === Number(mem.userId))
      form[row.moduleCode][mem.userId] = exist ? Number(exist.ratio) : 0
    }
  }
}

/** 某模块合计（保留2位避免浮点误差） */
function sumOf(moduleCode) {
  let sum = 0
  for (const mem of members.value) {
    sum += Number(form[moduleCode]?.[mem.userId] || 0)
  }
  return Math.round(sum * 100) / 100
}

/** 组长保存分配（前端先校验每模块合计=100，后端兜底） */
async function doSave() {
  const list = []
  for (const row of matrixRows.value) {
    const sum = sumOf(row.moduleCode)
    if (sum !== 100) {
      proxy.$modal.msgError(`模块${row.moduleCode}（${row.moduleName}）合计为${sum}%，必须为100%`)
      return
    }
    for (const mem of members.value) {
      list.push({ moduleCode: row.moduleCode, userId: mem.userId, ratio: form[row.moduleCode][mem.userId] })
    }
  }
  saving.value = true
  try {
    await saveContribution(list)
    proxy.$modal.msgSuccess('保存成功，请通知组员重新确认')
    load()
  } finally {
    saving.value = false
  }
}

/** 组员一键确认 */
async function doConfirm() {
  await proxy.$modal.confirm('确认你知晓并认可本组各模块贡献率分配？')
  await confirmContribution()
  proxy.$modal.msgSuccess('确认成功')
  load()
}

/** 某组员是否已确认（该成员全部分配行 confirmed=1） */
function memberConfirmed(userId) {
  const mine = rows.value.filter(r => Number(r.userId) === Number(userId))
  return mine.length > 0 && mine.every(r => Number(r.confirmed) === 1)
}

load()
</script>

<style lang="scss" scoped>
/* ==================== 贡献率页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.panel-card { border-radius: 10px; }
.card-title { font-weight: 600; color: #1d2b3a; }
.panel-head { display: flex; justify-content: space-between; align-items: center; }

.module-cell { font-size: 13px; font-weight: 600; color: #1d2b3a; }
.mem-no { font-size: 11px; color: #86909c; font-weight: 400; }

/* 合计达标绿/未达标红 */
.sum-ok { color: #67c23a; font-weight: 700; }
.sum-bad { color: #f56c6c; font-weight: 700; }

.confirm-bar {
  margin-top: 14px; padding-top: 12px; border-top: 1px solid #ebeef5;
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
  .confirm-tip { font-size: 12px; color: #86909c; }
}
</style>
