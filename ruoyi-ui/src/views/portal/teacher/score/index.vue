<template>
  <!-- ============================================================================
       【功能】模块赋分页（计贡献率模块 5-10 设置满分）
       ----------------------------------------------------------------------------
       【说明】六大模块满分值总和必须恰好等于 100 才能保存（后端强校验）；
              个人最终得分 = Σ(模块满分 × 个人该模块贡献率)。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">模块赋分</h2>
        <p class="page-sub">对计贡献率的 6 个模块设置满分值，总和必须等于 100 分</p>
      </div>
      <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadList">
        <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
    </div>

    <!-- 赋分设置卡片 -->
    <el-card shadow="never" v-loading="loading" v-if="classId">
      <el-table :data="rows">
        <el-table-column label="模块" width="220">
          <template #default="scope">
            <span class="module-name">{{ scope.row.moduleCode }}. {{ scope.row.moduleName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模块满分值" width="200">
          <template #default="scope">
            <el-input-number v-model="scope.row.score" :min="0" :max="100" :step="0.5" :precision="1" controls-position="right" style="width: 150px" />
          </template>
        </el-table-column>
        <el-table-column label="说明">
          <template #default="scope">
            <span class="desc">该模块得分 = 满分 × 个人贡献率（组内贡献率合计 100%）</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 合计提示条：等于100可保存，否则红色提示 -->
      <div class="sum-bar">
        <span>当前合计：</span>
        <span class="sum-num" :class="{ ok: totalEq100, bad: !totalEq100 }">{{ totalScore }} 分</span>
        <el-tag :type="totalEq100 ? 'success' : 'danger'" size="small">
          {{ totalEq100 ? '符合要求（=100）' : '必须恰好等于 100 分' }}
        </el-tag>
        <el-button type="primary" class="save-btn" :disabled="!totalEq100" :loading="submitLoading" @click="save">保存赋分</el-button>
      </div>
    </el-card>
    <el-empty v-else description="请先选择班级" :image-size="90" />
  </div>
</template>

<script setup name="TeacherScore">
// ============================================================================
// 【功能】模块赋分逻辑：6模块满分编辑 + 总分100实时校验 + 保存
// ============================================================================
import { listClass, listScoreSet, saveScoreSet } from '@/api/teach/portal'

const { proxy } = getCurrentInstance()

const classList = ref([])
const classId = ref(null)
const rows = ref([])
const loading = ref(false)
const submitLoading = ref(false)

/** 合计分（四舍五入1位小数） */
const totalScore = computed(() => {
  return rows.value.reduce((s, r) => s + (Number(r.score) || 0), 0).toFixed(1)
})
/** 是否恰好等于100（保存按钮开关依据） */
const totalEq100 = computed(() => Number(totalScore.value) === 100)

/** 加载班级下拉 */
async function loadClassOptions() {
  const res = await listClass()
  classList.value = res.data || []
  if (!classId.value && classList.value.length > 0) {
    classId.value = classList.value[0].id
  }
  if (classId.value) loadList()
}

/** 加载赋分设置（未设置时分数为空，前端默认0） */
async function loadList() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await listScoreSet(classId.value)
    rows.value = (res.data || []).map(r => ({ ...r, score: r.score == null ? 0 : Number(r.score) }))
  } finally {
    loading.value = false
  }
}

/** 保存赋分（后端强校验总分=100） */
async function save() {
  submitLoading.value = true
  try {
    await saveScoreSet(classId.value, rows.value)
    proxy.$modal.msgSuccess('赋分保存成功')
  } finally {
    submitLoading.value = false
  }
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== 模块赋分页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }

.module-name { font-weight: 600; color: #1d2b3a; }
.desc { color: #86909c; font-size: 13px; }

/* 合计提示条 */
.sum-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  .sum-num { font-size: 20px; font-weight: 700; }
  .sum-num.ok { color: #67c23a; }
  .sum-num.bad { color: #f56c6c; }
  .save-btn { margin-left: auto; }
}
</style>
