<template>
  <!-- ============================================================================
       【功能】AI批改与成绩判分页（教师端，阶段7）
       ----------------------------------------------------------------------------
       【说明】全流程：选班级 → 小组总览 → 触发AI批改（模型不可用自动规则兜底）→
              查看批改详情（AI分+评语）→ 教师终审核定 → 成绩汇总 → 发布成绩。
              小组最终分 = Σ各模块生效分（教师核定分优先，缺省用AI参考分）；
              个人最终得分 = Σ(模块生效分 × 个人贡献率)。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 + 引擎状态提示 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">AI 批改</h2>
        <p class="page-sub">本地多模态大模型批改模块内容并生成评语，教师终审后汇总发布成绩</p>
      </div>
      <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadBoard">
        <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
    </div>

    <!-- 小组总览表 -->
    <el-card shadow="never" v-loading="loading" v-if="classId">
      <el-table :data="rows" row-key="id">
        <el-table-column label="小组" width="110">
          <template #default="s"><span class="group-name">{{ s.row.groupName }}</span></template>
        </el-table-column>
        <el-table-column label="任务" min-width="150">
          <template #default="s">
            <span v-if="s.row.taskCode">{{ s.row.taskCode }}：{{ s.row.taskName }}</span>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="总稿" width="100">
          <template #default="s">
            <el-tag :type="s.row.submitStatus === 1 ? 'success' : 'info'" size="small">
              {{ s.row.submitStatus === 1 ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI参考分" width="100" align="center">
          <template #default="s">{{ s.row.aiRefScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="组最终分" width="100" align="center">
          <template #default="s">{{ s.row.teacherScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="批改/成绩" width="130" align="center">
          <template #default="s">
            <el-tag :type="s.row.reviewCount > 0 ? 'primary' : 'info'" size="small" class="mr4">
              批改{{ s.row.reviewCount > 0 ? '已生成' : '无' }}
            </el-tag>
            <el-tag :type="s.row.published ? 'success' : 'info'" size="small">
              {{ s.row.scoreCount > 0 ? (s.row.published ? '已发布' : '未发布') : '无成绩' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="s">
            <el-button type="primary" size="small" :disabled="s.row.submitStatus !== 1"
              :loading="reviewingId === s.row.id" @click="doReview(s.row)">AI批改</el-button>
            <el-button size="small" :disabled="s.row.reviewCount === 0"
              @click="openDetail(s.row)">批改详情/终审</el-button>
            <el-button size="small" :disabled="s.row.scoreCount === 0"
              @click="openScores(s.row)">成绩单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="tip-bar">
        <span class="tip-dot"></span>
        <span>仅"已提交总稿"的小组可发起批改；AI 引擎（ai_engine/ai_server.py）未启动时自动切换本地规则模拟批改。</span>
      </div>
    </el-card>
    <el-empty v-else description="请先选择班级" :image-size="90" />

    <!-- 批改详情 / 终审对话框 -->
    <el-dialog v-model="detailVisible" :title="`${activeRow.groupName} · 批改详情与终审`" width="880px" top="6vh">
      <div class="engine-line" v-if="detailRows.length">
        批改来源：<el-tag size="small">{{ detailRows[0].modelName || '-' }}</el-tag>
        <span class="muted ml8">生效分 = 教师核定分（填写后），未填写时取 AI 参考分</span>
      </div>
      <el-table :data="detailRows" v-loading="detailLoading">
        <el-table-column label="模块" width="150">
          <template #default="s">{{ s.row.moduleCode }}. {{ s.row.moduleName }}</template>
        </el-table-column>
        <el-table-column label="满分" width="70" align="center">
          <template #default="s">{{ s.row.moduleMax ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="AI参考分" width="90" align="center">
          <template #default="s"><span class="ai-score">{{ s.row.aiScore }}</span></template>
        </el-table-column>
        <el-table-column label="教师核定分" width="150" align="center">
          <template #default="s">
            <el-input-number v-model="s.row._teacherScore" :min="0" :max="100" :step="1" :precision="0"
              controls-position="right" placeholder="不调整留空" style="width: 130px" />
          </template>
        </el-table-column>
        <el-table-column label="评语（优点 / 问题 / 建议）" min-width="300">
          <template #default="s">
            <div class="comment-box" v-if="parseComment(s.row.reviewProcess)">
              <p><b class="c-green">优点：</b>{{ parseComment(s.row.reviewProcess).strengths }}</p>
              <p><b class="c-red">问题：</b>{{ parseComment(s.row.reviewProcess).problems }}</p>
              <p><b class="c-blue">建议：</b>{{ parseComment(s.row.reviewProcess).advice }}</p>
            </div>
            <span v-else class="muted">{{ s.row.reviewProcess }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
        <el-button type="primary" :loading="finalizeLoading" @click="doFinalize">保存终审核定</el-button>
        <el-button type="success" :loading="computeLoading" @click="doCompute">成绩汇总</el-button>
        <el-button type="warning" :disabled="!activeRow.scoreCount" :loading="publishLoading"
          @click="doPublish">{{ activeRow.published ? '重新发布' : '发布成绩' }}</el-button>
      </template>
    </el-dialog>

    <!-- 成绩单对话框 -->
    <el-dialog v-model="scoresVisible" :title="`${activeRow.groupName} · 成绩单`" width="640px">
      <el-table :data="scoreRows" v-loading="scoresLoading">
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="姓名" prop="nickName" width="120" />
        <el-table-column label="小组最终分" prop="groupScore" align="center" />
        <el-table-column label="个人最终得分" prop="finalScore" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="s">
            <el-tag :type="s.row.isPublished === 1 ? 'success' : 'info'" size="small">
              {{ s.row.isPublished === 1 ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="tip-bar">
        <span class="tip-dot"></span>
        <span>个人最终得分 = Σ(模块生效分 × 本人该模块贡献率)；重新汇算会自动撤回发布。</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="TeacherAiReview">
// ============================================================================
// 【功能】AI批改页逻辑：总览加载 / 触发批改 / 批改详情与终审 / 成绩汇总 / 发布
// ============================================================================
import {
  listClass, reviewBoard, runAiReview, listReview, finalizeReview,
  computeScore, publishScore, listScores
} from '@/api/teach/portal'

const { proxy } = getCurrentInstance()

const classList = ref([])
const classId = ref(null)
const rows = ref([])              // 小组总览行
const loading = ref(false)
const reviewingId = ref(null)     // 正在批改的小组ID（按钮loading）

// ----- 批改详情对话框状态 -----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRows = ref([])        // 批改记录行（附 _teacherScore 编辑字段）
const activeRow = ref({})         // 当前操作小组
const finalizeLoading = ref(false)
const computeLoading = ref(false)
const publishLoading = ref(false)

// ----- 成绩单对话框状态 -----
const scoresVisible = ref(false)
const scoresLoading = ref(false)
const scoreRows = ref([])

/** 加载班级下拉（默认选第一个班） */
async function loadClassOptions() {
  const res = await listClass()
  classList.value = res.data || []
  if (!classId.value && classList.value.length > 0) {
    classId.value = classList.value[0].id
  }
  if (classId.value) loadBoard()
}

/** 加载小组批改总览 */
async function loadBoard() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await reviewBoard(classId.value)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 触发 AI 批改（长耗时：后端自动降级规则兜底） */
async function doReview(row) {
  await proxy.$modal.confirm(
    `确认为「${row.groupName}」发起 AI 批改？` +
    (row.reviewCount > 0 ? '已有批改结果将被覆盖。' : '') +
    '（模型推理约需 1~3 分钟，AI 引擎未启动时自动使用本地规则模拟）')
  reviewingId.value = row.id
  try {
    const res = await runAiReview(row.id)
    proxy.$modal.msgSuccess(res.msg || '批改完成')
    loadBoard()
  } finally {
    reviewingId.value = null
  }
}

/** 打开批改详情/终审对话框（加载批改记录并准备核定分编辑） */
async function openDetail(row) {
  activeRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await listReview(row.id)
    detailRows.value = (res.data || []).map(r => ({
      ...r,
      _teacherScore: r.teacherScore == null ? undefined : Number(r.teacherScore)
    }))
  } finally {
    detailLoading.value = false
  }
}

/** 解析批改过程 JSON（评语三段式：优点/问题/建议），解析失败返回 null 走原文展示 */
function parseComment(text) {
  try { return JSON.parse(text) } catch (e) { return null }
}

/** 保存终审核定（提交全部模块的核定分） */
async function doFinalize() {
  finalizeLoading.value = true
  try {
    // 仅提交有核定值的行，其余行不改动（服务端保留 AI 参考分）
    const payload = detailRows.value
      .filter(r => r._teacherScore != null)
      .map(r => ({ id: r.id, moduleCode: r.moduleCode, teacherScore: r._teacherScore }))
    await finalizeReview(activeRow.value.id, payload)
    proxy.$modal.msgSuccess('终审核定已保存')
    openDetail(activeRow.value)   // 刷新详情（回显核查状态）
    loadBoard()
  } finally {
    finalizeLoading.value = false
  }
}

/** 成绩汇总（组分 + 个人分落库，未发布） */
async function doCompute() {
  computeLoading.value = true
  try {
    const res = await computeScore(activeRow.value.id)
    proxy.$modal.msgSuccess(`成绩汇总完成，小组最终分：${res.data}`)
    activeRow.value.scoreCount = 1
    activeRow.value.published = false
    loadBoard()
  } finally {
    computeLoading.value = false
  }
}

/** 发布成绩（发布后组内学生可见） */
async function doPublish() {
  publishLoading.value = true
  try {
    await publishScore(activeRow.value.id)
    proxy.$modal.msgSuccess('成绩已发布，组内学生可见')
    activeRow.value.published = true
    loadBoard()
  } finally {
    publishLoading.value = false
  }
}

/** 打开成绩单对话框 */
async function openScores(row) {
  activeRow.value = row
  scoresVisible.value = true
  scoresLoading.value = true
  try {
    const res = await listScores(row.id)
    scoreRows.value = res.data || []
  } finally {
    scoresLoading.value = false
  }
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== AI批改页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }

.group-name { font-weight: 600; color: #1d2b3a; }
.muted { color: #a0a6ad; font-size: 13px; }
.ml8 { margin-left: 8px; }
.mr4 { margin-right: 4px; }
.ai-score { font-weight: 700; color: #e6a23c; }

/* 评语三段式展示 */
.comment-box {
  font-size: 13px; line-height: 1.7; color: #4e5969;
  p { margin: 0 0 4px; }
  .c-green { color: #67c23a; }
  .c-red { color: #f56c6c; }
  .c-blue { color: #409eff; }
}

/* 提示条 */
.tip-bar {
  display: flex; align-items: center; gap: 6px;
  margin-top: 14px; padding: 10px 14px;
  background: #f5f7fa; border-radius: 8px;
  color: #86909c; font-size: 13px;
  .tip-dot { width: 6px; height: 6px; border-radius: 50%; background: #409eff; flex: none; }
}
.engine-line { margin-bottom: 12px; font-size: 13px; color: #4e5969; }
</style>
