<template>
  <!-- ============================================================================
       【功能】模块设置页（10大模块编辑规则，同步学生端）
       ----------------------------------------------------------------------------
       【说明】按班级设置各模块：校验类型（字数/条目数）、最低/最高要求、
              公式/图片/代码块能力开关、是否需要学生编辑。
              封面/任务要求/参数配置默认只读；参考资料默认按条目3~20。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 + 保存 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">模块设置</h2>
        <p class="page-sub">设置各模块的字数/条目区间与编辑能力，保存后同步到学生端编辑器</p>
      </div>
      <el-space>
        <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadList">
          <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!classId" :loading="submitLoading" @click="save">保存设置</el-button>
      </el-space>
    </div>

    <!-- 模块设置表格（10行行内编辑） -->
    <el-card shadow="never" v-loading="loading">
      <el-table :data="rows" v-if="classId">
        <el-table-column label="模块" width="150">
          <template #default="scope">
            <span class="module-name">{{ scope.row.moduleCode }}. {{ scope.row.moduleName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="校验类型" width="130" align="center">
          <template #default="scope">
            <el-select v-model="scope.row.countType" size="small" :disabled="scope.row.moduleCode == 1 || scope.row.moduleCode == 2 || scope.row.moduleCode == 4">
              <el-option :value="1" label="按字数" />
              <el-option :value="2" label="按条目数" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="最低要求" width="140" align="center">
          <template #default="scope">
            <el-input-number v-model="scope.row.minCount" :min="0" :max="99999" size="small" controls-position="right" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="最高要求" width="140" align="center">
          <template #default="scope">
            <el-input-number v-model="scope.row.maxCount" :min="0" :max="99999" size="small" controls-position="right" style="width: 110px" />
            <div class="tip">0 为不限</div>
          </template>
        </el-table-column>
        <el-table-column label="公式" width="70" align="center">
          <template #default="scope"><el-switch v-model="scope.row.needFormula" :active-value="1" :inactive-value="0" /></template>
        </el-table-column>
        <el-table-column label="图片" width="70" align="center">
          <template #default="scope"><el-switch v-model="scope.row.needImage" :active-value="1" :inactive-value="0" /></template>
        </el-table-column>
        <el-table-column label="代码块" width="70" align="center">
          <template #default="scope"><el-switch v-model="scope.row.needCode" :active-value="1" :inactive-value="0" /></template>
        </el-table-column>
        <el-table-column label="需学生编辑" width="100" align="center">
          <template #default="scope"><el-switch v-model="scope.row.editable" :active-value="1" :inactive-value="0" /></template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="请先选择班级" :image-size="90" />
    </el-card>
  </div>
</template>

<script setup name="TeacherModule">
// ============================================================================
// 【功能】模块设置逻辑：10模块行内编辑 + 整体保存（upsert）
// ============================================================================
import { listClass, listModuleSet, saveModuleSet } from '@/api/teach/portal'

const { proxy } = getCurrentInstance()

const classList = ref([])
const classId = ref(null)
const rows = ref([])
const loading = ref(false)
const submitLoading = ref(false)

/** 加载班级下拉 */
async function loadClassOptions() {
  const res = await listClass()
  classList.value = res.data || []
  if (!classId.value && classList.value.length > 0) {
    classId.value = classList.value[0].id
  }
  if (classId.value) loadList()
}

/** 加载模块设置（无记录时后端返回默认配置） */
async function loadList() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await listModuleSet(classId.value)
    rows.value = (res.data || []).map(r => ({
      ...r,
      minCount: r.minCount || 0,
      maxCount: r.maxCount || 0
    }))
  } finally {
    loading.value = false
  }
}

/** 保存设置（整体提交10条） */
async function save() {
  if (rows.value.length !== 10) {
    proxy.$modal.msgWarning('模块设置不完整')
    return
  }
  submitLoading.value = true
  try {
    await saveModuleSet(classId.value, rows.value)
    proxy.$modal.msgSuccess('保存成功，已同步学生端')
  } finally {
    submitLoading.value = false
  }
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== 模块设置页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }

.module-name { font-weight: 600; color: #1d2b3a; }
.tip { font-size: 11px; color: #c0c4cc; line-height: 1; }
</style>
