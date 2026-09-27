<template>
  <!-- ============================================================================
       【功能】帮助中心（师生共用，阶段8）
       ----------------------------------------------------------------------------
       【说明】左侧为分类目录（快速上手/学生端指南/教师端指南/常见问题），
              右侧为该分类下的条目列表；点击条目展开查看富文本内容。
              数据来自 te_help 已发布条目，管理员可在帮助管理页维护。
       ============================================================================ -->
  <div class="help-page">
    <div class="page-head">
      <h2 class="page-title">帮助中心</h2>
      <p class="page-sub">平台使用指引与常见问题解答</p>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：分类目录 -->
      <el-col :span="6">
        <el-card shadow="never" class="cate-card">
          <div v-for="c in categories" :key="c"
               class="cate-item" :class="{ active: currentCate === c }"
               @click="currentCate = c">
            <span class="cate-name">{{ c }}</span>
            <span class="cate-count">{{ grouped[c].length }} 篇</span>
          </div>
          <el-empty v-if="categories.length === 0" description="暂无帮助内容" :image-size="70" />
        </el-card>
      </el-col>

      <!-- 右侧：条目列表（手风琴展开内容） -->
      <el-col :span="18">
        <el-card shadow="never">
          <template #header><span class="card-title">{{ currentCate }}</span></template>
          <el-collapse v-if="items.length > 0" v-model="opened">
            <el-collapse-item v-for="it in items" :key="it.id" :name="it.id">
              <template #title>
                <span class="item-title">{{ it.title }}</span>
              </template>
              <!-- 富文本内容（后台维护时为受控 HTML） -->
              <div class="help-content" v-html="it.content"></div>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-else description="该分类下暂无条目" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="HelpCenter">
// ============================================================================
// 【功能】帮助中心逻辑：按分类分组展示已发布条目
// ============================================================================
import { listHelp } from '@/api/teach/dashboard'

// 全部条目与当前选中分类
const list = ref([])
const currentCate = ref('')

/** 按分类分组的映射 { 分类名: [条目...] } */
const grouped = computed(() => {
  const map = {}
  for (const it of list.value) {
    const key = it.category || '其他'
    if (!map[key]) map[key] = []
    map[key].push(it)
  }
  return map
})
/** 分类目录（保持接口返回顺序） */
const categories = computed(() => Object.keys(grouped.value))
/** 当前分类下的条目 */
const items = computed(() => grouped.value[currentCate.value] || [])

/** 展开的折叠面板（默认展开当前分类第一篇） */
const opened = ref([])

/** 监听分类切换：自动展开第一篇 */
watch(currentCate, (v) => {
  const arr = grouped.value[v] || []
  opened.value = arr.length > 0 ? [arr[0].id] : []
})

/** 加载已发布条目 */
async function load() {
  const res = await listHelp()
  list.value = res.data || []
  if (categories.value.length > 0) currentCate.value = categories.value[0]
}
load()
</script>

<style lang="scss" scoped>
/* ==================== 帮助中心样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 16px; color: #86909c; font-size: 14px; }
.card-title { font-weight: 600; color: #1d2b3a; }

.cate-card { border-radius: 10px; }
/* 分类目录项（选中高亮） */
.cate-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; border-radius: 8px; cursor: pointer; margin-bottom: 4px;
  transition: all 0.2s;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; .cate-name { color: #409eff; font-weight: 600; } }
  .cate-name { font-size: 14px; color: #1d2b3a; }
  .cate-count { font-size: 12px; color: #86909c; }
}

.item-title { font-size: 14px; color: #1d2b3a; font-weight: 500; }

/* 富文本内容排版 */
.help-content {
  font-size: 14px; color: #3d4a5c; line-height: 1.9; padding: 0 8px;
  :deep(p) { margin: 6px 0; }
  :deep(b) { color: #1d2b3a; }
}
</style>
