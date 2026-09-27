<template>
  <!-- ============================================================================
       【功能】班级公告面板（学生端/教师端共用，阶段6）
       ----------------------------------------------------------------------------
       【说明】教师面向自己指导的班级发布公告，学生查看本班公告：
              · 公告按发布时间倒序卡片展示（标题/发布人/时间/正文）；
              · 教师端右上角"发布公告"按钮，弹出班级/标题/正文表单；
              · 发布成功后服务端向该班在线学生推送 WebSocket 实时提醒，
                学生在线时收到右上角弹窗通知并自动刷新列表。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">班级公告</h2>
      <p class="page-sub">{{ isTeacher ? '面向自己指导的班级发布公告，在线学生将收到实时提醒' : '查看本班公告；教师发布新公告时会在右上角实时提醒' }}</p>
    </div>

    <!-- 工具条 -->
    <div class="toolbar">
      <span></span>
      <!-- 仅教师可发布公告 -->
      <el-button v-if="isTeacher" type="primary" @click="openPublish">发布公告</el-button>
    </div>

    <!-- 公告卡片列表（倒序由后端保证） -->
    <div class="notice-list">
      <el-card v-for="n in notices" :key="n.id" shadow="hover" class="notice-card">
        <div class="notice-head">
          <span class="notice-title">{{ n.title }}</span>
          <el-tag v-if="n.className" size="small" type="info" effect="plain">{{ n.className }}</el-tag>
        </div>
        <div class="notice-meta">{{ n.publishBy }} 发布于 {{ n.publishTime }}</div>
        <div class="notice-content">{{ n.content }}</div>
      </el-card>
      <el-empty v-if="notices.length === 0" description="暂无公告" :image-size="100" />
    </div>

    <!-- 发布公告对话框（教师） -->
    <el-dialog v-model="publishVisible" title="发布公告" width="560px">
      <el-form label-width="70px">
        <el-form-item label="班级">
          <el-select v-model="publishForm.classId" placeholder="选择指导的班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="publishForm.title" maxlength="100" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="publishForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit
                    placeholder="公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPublish">发布并推送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PortalNoticePanel">
// ============================================================================
// 【功能】班级公告逻辑：列表/教师发布/WS实时提醒联动
// ============================================================================
import { listNotices, publishNotice } from '@/api/teach/message'
import { connectWs, onWsMsg } from '@/utils/websocket'
import request from '@/utils/request'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isTeacher = computed(() => userStore.roles.includes('teacher')) // 仅教师显示发布按钮

// ---------------- 状态 ----------------
const notices = ref([])           // 公告列表
const classes = ref([])           // 教师指导的班级（发布下拉用）
const publishVisible = ref(false) // 发布对话框
const saving = ref(false)
const publishForm = reactive({ classId: null, title: '', content: '' })

// ---------------- 数据加载 ----------------
// 拉取公告列表（学生=本班；教师=我指导的全部班级）
async function loadNotices() {
  const res = await listNotices()
  notices.value = res.data || []
}

// 打开发布对话框：同时拉取教师指导的班级下拉
async function openPublish() {
  publishForm.classId = null
  publishForm.title = ''
  publishForm.content = ''
  const res = await request({ url: '/teach/class/list', method: 'get' })
  classes.value = res.data || []
  publishVisible.value = true
}

// 提交发布（后端落库+向该班在线学生推送WS提醒）
async function doPublish() {
  if (!publishForm.classId) return proxy.$modal.msgWarning('请选择班级')
  if (!publishForm.title.trim()) return proxy.$modal.msgWarning('请填写标题')
  if (!publishForm.content.trim()) return proxy.$modal.msgWarning('请填写正文')
  saving.value = true
  try {
    const res = await publishNotice({
      classId: publishForm.classId,
      title: publishForm.title.trim(),
      content: publishForm.content.trim()
    })
    proxy.$modal.msgSuccess(res.msg || '发布成功')
    publishVisible.value = false
    loadNotices()
  } finally {
    saving.value = false
  }
}

// ---------------- WS 实时提醒 ----------------
let offWs = null
function handleWs(data) {
  // 收到公告推送：弹通知并刷新列表（学生在线场景）
  if (data.type === 'notice') {
    proxy.$notify({ title: '新公告：' + data.title, message: (data.publishBy || '') + ' 发布了班级公告', type: 'warning' })
    loadNotices()
  }
}

onMounted(() => {
  connectWs()             // 保证长连接在线（幂等）
  offWs = onWsMsg(handleWs)
  loadNotices()
})
onUnmounted(() => {
  if (offWs) offWs()      // 仅解绑回调，ws 单例保持
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.notice-list { display: flex; flex-direction: column; gap: 10px; }
.notice-head { display: flex; align-items: center; gap: 8px; }
.notice-title { font-size: 16px; font-weight: 600; flex: 1; }
.notice-meta { font-size: 12px; color: #909399; margin: 6px 0; }
.notice-content { font-size: 14px; white-space: pre-wrap; line-height: 1.7; }
</style>
