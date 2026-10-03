<template>
  <!-- ============================================================================
       【功能】我的成绩页（学生端，阶段7）
       ----------------------------------------------------------------------------
       【说明】教师发布成绩后可见：小组最终分 / 个人最终得分 /
              各模块终分（不含AI批改过程，学生仅见终分）/ 本人各模块贡献率；
              未发布时仅显示提示，不透出任何分数。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的成绩</h2>
        <p class="page-sub" v-if="view.published">{{ view.groupName }} · 成绩已于 {{ view.publishTime }} 发布</p>
        <p class="page-sub" v-else>成绩由教师批改后统一发布</p>
      </div>
    </div>

    <!-- 未发布：占位提示 -->
    <el-card shadow="never" v-if="!published">
      <el-empty description="教师尚未发布本组成绩，发布后可在此查看" :image-size="110" />
    </el-card>

    <!-- 已发布：成绩全景 -->
    <template v-else>
      <!-- 分数统计卡 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never" class="score-card">
            <p class="card-label">小组最终分</p>
            <p class="card-num group">{{ view.groupScore }}<span class="card-unit">分</span></p>
            <p class="card-desc">小组最终分 = 各计分模块生效分（模块得分）之和（满分 100）</p>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="score-card">
            <p class="card-label">我的最终得分</p>
            <p class="card-num mine">{{ view.finalScore }}<span class="card-unit">分</span></p>
            <p class="card-desc">个人最终得分 = Σ(模块生效分 × 本人该模块贡献率)</p>
          </el-card>
        </el-col>
      </el-row>

      <!-- 各模块终分 + 本人贡献率 -->
      <el-card shadow="never" class="mt16">
        <template #header><span class="card-title">模块得分明细</span></template>
        <el-table :data="mergedRows">
          <el-table-column label="模块" width="180">
            <template #default="s">{{ s.row.moduleCode }}. {{ s.row.moduleName }}</template>
          </el-table-column>
          <el-table-column label="模块满分" width="100" align="center">
            <template #default="s">{{ s.row.moduleMax ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="模块生效分" width="110" align="center">
            <template #default="s"><span class="final-score">{{ s.row.finalScore ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="本人贡献率" width="110" align="center">
            <template #default="s">{{ s.row.ratio != null ? s.row.ratio + '%' : '—' }}</template>
          </el-table-column>
          <el-table-column label="本模块我的得分" align="center">
            <template #default="s">{{ s.row.myEarned ?? '—' }}</template>
          </el-table-column>
        </el-table>
        <div class="tip-bar">
          <span class="tip-dot"></span>
          <span>本模块我的得分 = 模块生效分（模块得分）× 本人贡献率%；各模块合计即个人最终得分。</span>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup name="StudentReview">
// ============================================================================
// 【功能】我的成绩逻辑：加载已发布成绩 + 模块明细与贡献率合并展示
// ============================================================================
import { getMyReview } from '@/api/teach/studentPortal'

const view = ref({})          // 后端成绩视图（published/groupScore/finalScore/modules/contributions）

const published = computed(() => !!view.value.published)

/**
 * 合并"模块终分"与"本人贡献率"为一张明细表：
 * 以模块终分为主表，左联本人的贡献率行并折算"本模块我的得分"
 */
const mergedRows = computed(() => {
  const mods = view.value.modules || []
  const contribMap = new Map((view.value.contributions || []).map(c => [c.moduleCode, c.ratio]))
  return mods.map(m => {
    const ratio = contribMap.get(m.moduleCode)
    // 折算本模块个人得分 = 模块生效分（已是模块得分绝对分）× 本人贡献率%（保留1位；与后端汇总口径一致）
    const earned = (ratio != null && m.finalScore != null)
      ? (Number(m.finalScore) * Number(ratio) / 100).toFixed(1) : null
    return { ...m, ratio, myEarned: earned }
  })
})

/** 加载我的成绩（未发布时后端只返回 published=false） */
async function loadReview() {
  const res = await getMyReview()
  view.value = res.data || {}
}

loadReview()
</script>

<style lang="scss" scoped>
/* ==================== 我的成绩页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { margin-bottom: 0; }
.mt16 { margin-top: 16px; }

/* 分数统计卡 */
.score-card {
  text-align: center; padding: 8px 0;
  .card-label { margin: 0 0 8px; color: #86909c; font-size: 14px; }
  .card-num { margin: 0; font-size: 40px; font-weight: 700; }
  .card-num.group { color: #409eff; }
  .card-num.mine { color: #67c23a; }
  .card-unit { font-size: 14px; font-weight: 400; color: #86909c; margin-left: 4px; }
  .card-desc { margin: 10px 0 0; color: #a0a6ad; font-size: 12px; }
}
.card-title { font-weight: 600; color: #1d2b3a; }
.final-score { font-weight: 700; color: #e6a23c; }

/* 提示条 */
.tip-bar {
  display: flex; align-items: center; gap: 6px;
  margin-top: 14px; padding: 10px 14px;
  background: #f5f7fa; border-radius: 8px;
  color: #86909c; font-size: 13px;
  .tip-dot { width: 6px; height: 6px; border-radius: 50%; background: #67c23a; flex: none; }
}
</style>
