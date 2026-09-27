<template>
  <!-- ============================================================================
       【功能】消息中心聊天面板（学生端/教师端共用，阶段6）
       ----------------------------------------------------------------------------
       【说明】经典左右布局 IM：
              · 左侧会话列表：单聊+群聊，含未读角标与最后一条消息；
                顶部下拉可向"可联系人"（学生=指导教师/组员，教师=本班学生）发起单聊；
              · 右侧聊天窗：时间正序消息流，自己靠右蓝色，对方靠左白色；
              · 实时收发走 WebSocket（utils/websocket.js），REST 仅拉取历史与回执；
              · 进入会话即顺带推进已读回执（后端 history 内置 markRead）。
       ============================================================================ -->
  <div class="chat-page">
    <!-- 左侧：会话列表 -->
    <el-card shadow="never" class="chat-left">
      <template #header>
        <div class="panel-head">
          <span class="card-title">会话</span>
          <!-- 发起聊天：从可联系人中选择 -->
          <el-dropdown trigger="click" @command="doStartSingle">
            <el-button type="primary" size="small">发起聊天</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="buddies.length === 0" disabled>暂无可联系人</el-dropdown-item>
                <el-dropdown-item v-for="b in buddies" :key="b.userId" :command="b.userId">
                  {{ buddyLabel(b) }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
      <div class="chat-list">
        <div v-for="c in chats" :key="c.chatId" class="chat-item" :class="{ active: c.chatId === activeChatId }"
             @click="openChat(c)">
          <el-badge :value="c.unreadCount" :hidden="!c.unreadCount" :max="99" class="chat-badge">
            <el-avatar :size="36" :style="{ background: avatarColor(c.chatType) }">
              {{ c.displayName ? c.displayName.slice(0, 1) : '?' }}
            </el-avatar>
          </el-badge>
          <div class="chat-meta">
            <div class="chat-name">
              {{ c.displayName }}
              <el-tag v-if="c.chatType === 2" size="small" type="info" effect="plain">群</el-tag>
            </div>
            <div class="chat-last">{{ c.lastContent || '暂无消息' }}</div>
          </div>
        </div>
        <el-empty v-if="chats.length === 0" description="暂无会话，试试右上角发起聊天" :image-size="80" />
      </div>
    </el-card>

    <!-- 右侧：聊天窗 -->
    <el-card shadow="never" class="chat-right">
      <template #header>
        <div class="panel-head">
          <span class="card-title">{{ activeChat ? activeChat.displayName : '消息中心' }}</span>
        </div>
      </template>
      <template v-if="activeChatId">
        <!-- 消息流：新消息自动滚到底部 -->
        <div class="msg-flow" ref="flowRef">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="{ mine: m.senderId === myUserId }">
            <div class="msg-bubble">
              <div class="msg-info">
                {{ m.senderId === myUserId ? '我' : (m.senderName || '对方') }} · {{ m.sendTime }}
              </div>
              <div class="msg-content">{{ m.content }}</div>
            </div>
          </div>
          <el-empty v-if="messages.length === 0" description="还没有消息，发一句打个招呼吧" :image-size="80" />
        </div>
        <!-- 输入区 -->
        <div class="msg-input">
          <el-input v-model="draft" type="textarea" :rows="2" maxlength="2000" show-word-limit
                    placeholder="输入消息，回车发送（Shift+回车换行）" @keydown.enter.exact.prevent="doSend" />
          <el-button type="primary" :disabled="!draft.trim()" @click="doSend">发送</el-button>
        </div>
      </template>
      <el-empty v-else description="从左侧选择一个会话开始聊天" :image-size="110" />
    </el-card>
  </div>
</template>

<script setup name="PortalChatPanel">
// ============================================================================
// 【功能】消息中心逻辑：会话列表/历史/实时收发/已读回执/发起单聊
// ============================================================================
import { listContacts, getHistory, listBuddies, startSingle, markRead } from '@/api/teach/message'
import { connectWs, onWsMsg, sendWs } from '@/utils/websocket'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const myUserId = computed(() => Number(userStore.id)) // 当前登录用户ID（区分自己/他人气泡）

// ---------------- 状态 ----------------
const chats = ref([])          // 左侧会话列表
const buddies = ref([])        // 可联系人下拉
const activeChatId = ref(null) // 当前打开的会话
const activeChat = ref(null)   // 当前会话对象（取展示名）
const messages = ref([])       // 当前会话历史消息
const draft = ref('')          // 输入框草稿
const flowRef = ref(null)      // 消息流容器（滚动到底用）

// 头像底色：群聊橙、单聊蓝绿交替感（简单用两色区分）
function avatarColor(chatType) {
  return chatType === 2 ? '#e6a23c' : '#409eff'
}

// 联系人下拉文案：姓名 + 分组/班级补充说明
function buddyLabel(b) {
  const extra = b.groupName || b.className || ''
  const tag = b.type === 1 ? '（指导教师）' : (extra ? `（${extra}）` : '')
  return b.nickName + tag
}

// ---------------- 数据加载 ----------------
// 拉取左侧会话列表（未读数/最后消息/排序均由后端给出）
async function loadContacts() {
  const res = await listContacts()
  chats.value = res.data || []
  // 同步当前会话对象的展示名（避免列表刷新后标题丢失）
  const cur = chats.value.find(c => c.chatId === activeChatId.value)
  if (cur) activeChat.value = cur
}

// 拉取可联系人下拉
async function loadBuddies() {
  const res = await listBuddies()
  buddies.value = res.data || []
}

// 打开会话：拉历史（后端顺带 markRead），并清零本地未读
async function openChat(chat) {
  activeChatId.value = chat.chatId
  activeChat.value = chat
  const res = await getHistory(chat.chatId)
  messages.value = (res.data && res.data.messages) || []
  chat.unreadCount = 0
  nextTick(scrollToBottom)
}

// 发起单聊：懒创建会话 → 刷新列表 → 直接打开
async function doStartSingle(targetUserId) {
  const res = await startSingle(targetUserId)
  await loadContacts()
  const chat = chats.value.find(c => c.chatId === res.data)
  if (chat) await openChat(chat)
}

// ---------------- 实时收发 ----------------
// 发送：仅走 WebSocket（服务端广播给会话全部成员，含自己）
function doSend() {
  const content = draft.value.trim()
  if (!content || !activeChatId.value) return
  const ok = sendWs({ type: 'chat', chatId: activeChatId.value, content: content })
  if (ok) {
    draft.value = ''
  } else {
    proxy.$modal.msgWarning('连接未就绪，请稍候重试')
  }
}

// 统一处理服务端推送
function handleWs(data) {
  if (data.type === 'chat') {
    // 命中当前会话：追加气泡；是自己发的也追加（服务端广播含发送者）
    if (data.chatId === activeChatId.value) {
      messages.value.push(data)
      // 我不在发送场景下收到新消息=已读，顺带推进回执
      if (data.senderId !== myUserId.value) markReadSilently(data.chatId)
      nextTick(scrollToBottom)
    }
    // 无论哪个会话：刷新左侧列表（最后一条消息与未读角标）
    loadContacts()
  } else if (data.type === 'notice') {
    // 公告实时提醒（教师发布时在线即弹）
    proxy.$notify({ title: '新公告：' + data.title, message: data.publishBy + ' 发布了班级公告', type: 'warning' })
  } else if (data.type === 'error') {
    proxy.$modal.msgError(data.msg || '发送失败')
  }
}

// 静默推进已读（失败不打扰用户）
function markReadSilently(chatId) {
  markRead(chatId).catch(() => {})
}

// 消息流滚动到底部
function scrollToBottom() {
  if (flowRef.value) flowRef.value.scrollTop = flowRef.value.scrollHeight
}

// ---------------- 生命周期 ----------------
let offWs = null // ws 回调解绑函数
onMounted(() => {
  connectWs()              // 幂等建连（含心跳与重连）
  offWs = onWsMsg(handleWs)
  loadContacts()
  loadBuddies()
})
onUnmounted(() => {
  // 仅解绑回调；ws 为全局单例，页面切换保持连接（公告页等也依赖推送）
  if (offWs) offWs()
})
</script>

<style scoped>
.chat-page { display: flex; gap: 12px; height: calc(100vh - 140px); }
.chat-left { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; }
.chat-right { flex: 1; display: flex; flex-direction: column; }
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; }

/* 左侧会话项 */
.chat-list { overflow-y: auto; flex: 1; }
.chat-item { display: flex; align-items: center; gap: 10px; padding: 10px 8px; border-radius: 6px; cursor: pointer; }
.chat-item:hover { background: #f5f7fa; }
.chat-item.active { background: #ecf5ff; }
.chat-meta { flex: 1; min-width: 0; }
.chat-name { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.chat-last { font-size: 12px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 右侧消息流 */
.msg-flow { flex: 1; overflow-y: auto; padding: 8px; background: #f7f8fa; border-radius: 6px; }
.msg-row { display: flex; margin-bottom: 12px; }
.msg-row.mine { justify-content: flex-end; }
.msg-bubble { max-width: 65%; }
.msg-row.mine .msg-bubble { text-align: right; }
.msg-info { font-size: 11px; color: #909399; margin-bottom: 3px; }
.msg-content { display: inline-block; padding: 8px 12px; border-radius: 8px; background: #fff; border: 1px solid #e4e7ed; font-size: 14px; white-space: pre-wrap; text-align: left; }
.msg-row.mine .msg-content { background: #409eff; border-color: #409eff; color: #fff; }

/* 输入区 */
.msg-input { display: flex; gap: 8px; margin-top: 10px; align-items: flex-end; }
</style>
