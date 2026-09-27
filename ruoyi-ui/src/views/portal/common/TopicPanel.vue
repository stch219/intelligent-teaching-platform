<template>
  <!-- ============================================================================
       【功能】模块讨论话题面板（学生端/教师端共用，阶段6）
       ----------------------------------------------------------------------------
       【说明】围绕10个课程模块的讨论区：
              · 学生：可按模块筛选/发起本组话题/回复本组话题；
              · 教师：查看本班全部小组话题并可参与回复；
              · 话题卡片显示模块号/标题/发起人/所属小组/回复数；
              · 点击卡片打开回复抽屉，支持首层回复（楼中楼仅记录指向人）。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">模块讨论</h2>
      <p class="page-sub">围绕课程模块展开小组讨论；学生可发起本组话题，教师可查看本班全部话题并回复</p>
    </div>

    <!-- 工具条：模块筛选 + 新建话题 -->
    <div class="toolbar">
      <el-select v-model="moduleFilter" clearable placeholder="按模块筛选" style="width: 240px" @change="loadTopics">
        <el-option v-for="m in 10" :key="m" :value="m" :label="m + '. ' + moduleNameOf(m)" />
      </el-select>
      <!-- 仅学生可发起话题 -->
      <el-button v-if="isStudent" type="primary" @click="openCreate">发起话题</el-button>
    </div>

    <!-- 话题卡片列表 -->
    <div class="topic-list">
      <el-card v-for="t in topics" :key="t.id" shadow="hover" class="topic-card" @click="openReplies(t)">
        <div class="topic-row">
          <el-tag size="small" type="warning">模块{{ t.moduleCode }}</el-tag>
          <span class="topic-title">{{ t.title }}</span>
          <el-badge :value="t.replyCount" :hidden="!t.replyCount" type="primary" class="topic-badge">
            <el-icon><ChatDotRound /></el-icon>
          </el-badge>
        </div>
        <div class="topic-meta">{{ t.creatorName }} · {{ t.groupName || '' }} · {{ t.createTime }}</div>
      </el-card>
      <el-empty v-if="topics.length === 0" description="暂无话题，发起第一个讨论吧" :image-size="100" />
    </div>

    <!-- 发起话题对话框（学生） -->
    <el-dialog v-model="createVisible" title="发起话题" width="480px">
      <el-form label-width="80px">
        <el-form-item label="模块">
          <el-select v-model="createForm.moduleCode" placeholder="选择模块" style="width: 100%">
            <el-option v-for="m in 10" :key="m" :value="m" :label="m + '. ' + moduleNameOf(m)" />
          </el-select>
        </el-form-item>
        <el-form-item label="话题标题">
          <el-input v-model="createForm.title" maxlength="200" show-word-limit placeholder="一句话说清讨论主题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">发布</el-button>
      </template>
    </el-dialog>

    <!-- 回复抽屉 -->
    <el-drawer v-model="replyVisible" :title="activeTopic ? activeTopic.title : '话题'" size="45%">
      <div class="reply-flow">
        <div v-for="r in replies" :key="r.id" class="reply-item">
          <div class="reply-head">
            <b>{{ r.nickName }}</b>
            <span v-if="r.replyToName" class="reply-to">回复 @{{ r.replyToName }}</span>
            <span class="reply-time">{{ r.replyTime }}</span>
          </div>
          <div class="reply-content">{{ r.content }}</div>
          <!-- 点某人名字即可回复TA -->
          <el-button link type="primary" size="small" @click="replyForm.replyId = r.id">回复TA</el-button>
        </div>
        <el-empty v-if="replies.length === 0" description="还没有回复" :image-size="80" />
      </div>
      <!-- 回复输入 -->
      <div class="reply-input">
        <el-tag v-if="replyForm.replyId" closable size="small" @close="replyForm.replyId = null">回复指定楼层</el-tag>
        <div class="input-row">
          <el-input v-model="replyForm.content" maxlength="1000" placeholder="写下你的观点..." @keydown.enter="doReply" />
          <el-button type="primary" :disabled="!replyForm.content.trim()" @click="doReply">回复</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="PortalTopicPanel">
// ============================================================================
// 【功能】模块讨论逻辑：话题列表/筛选/发起/回复流/回复
// ============================================================================
import { listTopics, createTopic, listReplies, replyTopic } from '@/api/teach/message'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isStudent = computed(() => userStore.roles.includes('student')) // 仅学生显示发起按钮

// 10个模块名称（与模块编辑页一致的固定文案）
const MODULE_NAMES = {
  1: '课题概述', 2: '需求分析', 3: '总体设计', 4: '数据库设计', 5: '详细设计',
  6: '系统实现', 7: '系统测试', 8: '技术难点', 9: '总结与展望', 10: '参考文献'
}
function moduleNameOf(code) {
  return MODULE_NAMES[code] || '模块' + code
}

// ---------------- 状态 ----------------
const topics = ref([])          // 话题列表
const moduleFilter = ref(null)  // 模块筛选
const createVisible = ref(false) // 发起话题对话框
const saving = ref(false)
const createForm = reactive({ moduleCode: null, title: '' })
const replyVisible = ref(false) // 回复抽屉
const activeTopic = ref(null)   // 当前查看的话题
const replies = ref([])         // 当前话题的回复列表
const replyForm = reactive({ replyId: null, content: '' })

// ---------------- 数据加载 ----------------
// 拉取话题列表（学生=本组；教师=本班；后端按身份过滤）
async function loadTopics() {
  const res = await listTopics(moduleFilter.value || undefined)
  topics.value = res.data || []
}

// 打开发起话题对话框
function openCreate() {
  createForm.moduleCode = null
  createForm.title = ''
  createVisible.value = true
}

// 提交新话题（校验在后端：已分组/模块1-10/标题非空）
async function doCreate() {
  if (!createForm.moduleCode) return proxy.$modal.msgWarning('请选择模块')
  if (!createForm.title.trim()) return proxy.$modal.msgWarning('请填写话题标题')
  saving.value = true
  try {
    await createTopic({ moduleCode: createForm.moduleCode, title: createForm.title.trim() })
    proxy.$modal.msgSuccess('话题已发布')
    createVisible.value = false
    moduleFilter.value = null
    loadTopics()
  } finally {
    saving.value = false
  }
}

// 打开话题的回复抽屉
async function openReplies(topic) {
  activeTopic.value = topic
  replyForm.replyId = null
  replyForm.content = ''
  replyVisible.value = true
  await loadReplies()
}

// 拉取回复列表
async function loadReplies() {
  const res = await listReplies(activeTopic.value.id)
  replies.value = res.data || []
}

// 提交回复
async function doReply() {
  const content = replyForm.content.trim()
  if (!content) return
  await replyTopic(activeTopic.value.id, { replyId: replyForm.replyId, content: content })
  replyForm.content = ''
  replyForm.replyId = null
  await loadReplies()
  // 回复数变化同步到话题卡片
  loadTopics()
}

onMounted(loadTopics)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.topic-list { display: flex; flex-direction: column; gap: 10px; }
.topic-card { cursor: pointer; }
.topic-row { display: flex; align-items: center; gap: 8px; }
.topic-title { font-size: 15px; font-weight: 600; flex: 1; }
.topic-badge { color: #909399; }
.topic-meta { font-size: 12px; color: #909399; margin-top: 6px; }

/* 回复抽屉 */
.reply-flow { overflow-y: auto; }
.reply-item { border-bottom: 1px solid #f0f0f0; padding: 10px 4px; }
.reply-head { font-size: 13px; display: flex; gap: 8px; align-items: center; }
.reply-to { color: #409eff; font-size: 12px; }
.reply-time { margin-left: auto; color: #c0c4cc; font-size: 12px; }
.reply-content { font-size: 14px; margin: 6px 0; white-space: pre-wrap; }
.reply-input { margin-top: 12px; }
.input-row { display: flex; gap: 8px; margin-top: 6px; }
</style>
