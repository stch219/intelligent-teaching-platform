<template>
  <!-- ============================================================================
       【功能】10大模块协同编辑页（学生端核心页）
       ----------------------------------------------------------------------------
       【说明】左侧：10模块列表（名称/状态/进度/字数条目/赋分，可切换）；
              右侧：富文本协同编辑区——
              · 只读模块（封面/任务要求/参数配置）：仅展示系统合成内容
              · 已提交模块：锁定展示，不可再编辑
              · 可编辑模块：Editor 富文本 + 暂存（上限校验）/提交（下限校验）
                + 乐观锁版本号防组员覆盖（冲突时提示刷新）
       ============================================================================ -->
  <div class="page">
    <el-row :gutter="16">
      <!-- 左侧模块列表 -->
      <el-col :span="7">
        <el-card shadow="never" class="list-card">
          <template #header><span class="card-title">报告模块（{{ modules.length }}）</span></template>
          <div class="module-item" v-for="m in modules" :key="m.moduleCode"
               :class="{ active: current === Number(m.moduleCode) }" @click="switchModule(m)">
            <div class="module-head">
              <span class="module-name">{{ m.moduleCode }}. {{ m.moduleName }}</span>
              <el-tag size="small" :type="statusTagType(m.status)">{{ statusText(m.status) }}</el-tag>
            </div>
            <el-progress :percentage="Number(m.progress) || 0" :stroke-width="6"
                         :status="Number(m.status) === 2 ? 'success' : undefined" />
            <div class="module-foot">
              <span>{{ countText(m) }}</span>
              <span v-if="m.score != null" class="module-score">{{ m.score }}分</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧编辑区 -->
      <el-col :span="17">
        <el-card shadow="never" class="edit-card" v-if="currentRow">
          <template #header>
            <div class="edit-head">
              <span class="card-title">{{ currentRow.moduleCode }}. {{ currentRow.moduleName }}</span>
              <div class="edit-meta">
                <span class="require-text">{{ requireText(currentRow) }}</span>
                <!-- 只读/已提交展示锁定提示 -->
                <el-tag v-if="isReadonly" type="info" size="small">只读模块（系统/教师生成）</el-tag>
                <el-tag v-else-if="isSubmitted" type="success" size="small">已提交·锁定</el-tag>
              </div>
            </div>
          </template>

          <!-- 只读或已提交：纯展示 -->
          <div v-if="isReadonly || isSubmitted" class="preview-box" v-html="content"></div>

          <!-- 可编辑：富文本编辑器 -->
          <template v-else>
            <editor v-model="content" :min-height="320" />
            <div class="edit-toolbar">
              <span class="word-count">当前字数：{{ wordCount }}</span>
              <div>
                <el-button @click="doSave" :loading="saving">暂存</el-button>
                <el-button type="primary" @click="doSubmit" :loading="submitting">提交</el-button>
              </div>
            </div>
          </template>
        </el-card>
        <el-empty v-else description="教师尚未设置模块，请等待教师配置" :image-size="100" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="StudentModules">
// ============================================================================
// 【功能】模块协同编辑逻辑：看板加载/切换 + 富文本读写 + 暂存/提交 +
//        字数统计 + 乐观锁版本冲突处理 + URL参数直达指定模块
// ============================================================================
import { listModules, getModule, saveModule, submitModule } from '@/api/teach/studentPortal'

const { proxy } = getCurrentInstance()
const route = useRoute()

// 看板数据与当前选中模块
const modules = ref([])
const current = ref(1)
// 当前模块详情（内容/版本/设置）
const currentRow = ref(null)
const content = ref('')
// 乐观锁版本号（暂存/提交需携带）
const version = ref(0)
const saving = ref(false)
const submitting = ref(false)

// 当前模块是否只读（editable=0）或已提交（status=2）
const isReadonly = computed(() => currentRow.value && Number(currentRow.value.editable) === 0)
const isSubmitted = computed(() => currentRow.value && Number(currentRow.value.status) === 2)

// 实时字数（与后端同规则：去HTML标签后长度）
const wordCount = computed(() => content.value.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim().length)

/** 加载模块看板，默认选中 URL 参数指定或第一个可编辑模块 */
async function load() {
  const res = await listModules()
  modules.value = res.data || []
  const codeFromUrl = Number(route.query.code)
  const target = modules.value.find(m => Number(m.moduleCode) === codeFromUrl)
    || modules.value.find(m => Number(m.editable) === 1)
    || modules.value[0]
  if (target) {
    current.value = Number(target.moduleCode)
    await loadDetail()
  }
}

/** 加载当前模块内容详情（含任务要求自动合成） */
async function loadDetail() {
  const res = await getModule(current.value)
  currentRow.value = res.data || null
  content.value = currentRow.value ? (currentRow.value.content || '') : ''
  version.value = currentRow.value ? Number(currentRow.value.version || 0) : 0
}

/** 切换模块（未保存内容直接丢弃，协同场景以服务端为准） */
async function switchModule(m) {
  current.value = Number(m.moduleCode)
  await loadDetail()
}

/** 暂存（上限校验由后端完成） */
async function doSave() {
  saving.value = true
  try {
    await saveModule({ moduleCode: current.value, content: content.value, version: version.value })
    proxy.$modal.msgSuccess('暂存成功')
    await Promise.all([load(), loadDetail()])
  } finally {
    saving.value = false
  }
}

/** 提交（下限校验由后端完成，提交后锁定） */
async function doSubmit() {
  await proxy.$modal.confirm('提交后该模块将锁定，不可再编辑，确定提交？')
  submitting.value = true
  try {
    await submitModule({ moduleCode: current.value, content: content.value, version: version.value })
    proxy.$modal.msgSuccess('提交成功，模块已锁定')
    await Promise.all([load(), loadDetail()])
  } finally {
    submitting.value = false
  }
}

/** 校验要求文本（字数/条目区间提示） */
function requireText(row) {
  const unit = Number(row.countType) === 1 ? '字' : '条'
  const max = Number(row.maxCount) > 0 ? row.maxCount : '不限'
  return `要求：${row.minCount} ~ ${max} ${unit}`
}

/** 模块计数展示文本 */
function countText(m) {
  if (Number(m.countType) === 1) return `字数 ${m.wordCount || 0}`
  return `条目 ${m.itemCount || 0}`
}

/** 模块状态文本与标签类型 */
function statusText(status) {
  const map = { 0: '未开展', 1: '暂存', 2: '已提交' }
  return map[Number(status)] || '未开展'
}
function statusTagType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[Number(status)] || 'info'
}

load()
</script>

<style lang="scss" scoped>
/* ==================== 模块编辑页样式 ==================== */
.list-card, .edit-card { border-radius: 10px; }
.card-title { font-weight: 600; color: #1d2b3a; }

/* 左侧模块单元（选中高亮） */
.module-item {
  border: 1px solid #ebeef5; border-radius: 8px; padding: 8px 10px; margin-bottom: 10px;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: #409eff; }
  &.active { border-color: #409eff; background: #ecf5ff; }
  .module-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
  .module-name { font-size: 13px; font-weight: 600; color: #1d2b3a; }
  .module-foot { display: flex; justify-content: space-between; margin-top: 4px; font-size: 12px; color: #86909c; }
  .module-score { color: #e6a23c; font-weight: 600; }
}

/* 右侧编辑区头部 */
.edit-head { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.edit-meta { display: flex; align-items: center; gap: 10px; }
.require-text { font-size: 12px; color: #86909c; }

/* 工具栏：字数统计 + 操作按钮 */
.edit-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 12px; padding-top: 12px; border-top: 1px solid #ebeef5;
  .word-count { font-size: 13px; color: #606266; }
}

/* 只读/已提交内容展示区 */
.preview-box {
  border: 1px solid #ebeef5; border-radius: 8px; padding: 16px;
  min-height: 320px; max-height: 620px; overflow: auto; line-height: 1.8;
  :deep(h2) { color: #1d2b3a; }
  :deep(h3) { color: #2e4a6b; }
  :deep(img) { max-width: 100%; }
  :deep(pre) { background: #f5f7fa; padding: 10px; border-radius: 6px; overflow: auto; }
}
</style>
