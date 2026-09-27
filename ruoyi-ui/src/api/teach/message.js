import request from '@/utils/request'

// ============================================================================
// 【功能】消息系统统一 API（阶段6）
// ----------------------------------------------------------------------------
// 【说明】三组接口：
//         · 消息中心 /teach/msg/*     会话列表/历史/已读/未读角标/联系人/发起单聊
//         · 模块讨论 /teach/topic/*   话题列表/发起/回复列表/回复
//         · 班级公告 /teach/notice/*  公告列表/教师发布公告
//         学生与教师共用，实时收发由 WebSocket（utils/websocket.js）承担
// ============================================================================

// ---------------- 消息中心 ----------------
// 我的会话列表（含展示名/未读数/最后一条消息）
export function listContacts() {
  return request({ url: '/teach/msg/contacts', method: 'get' })
}
// 会话历史消息（时间正序；进入会话即顺带推进已读回执）
export function getHistory(chatId) {
  return request({ url: '/teach/msg/history/' + chatId, method: 'get' })
}
// 手动标记会话已读
export function markRead(chatId) {
  return request({ url: '/teach/msg/read/' + chatId, method: 'post' })
}
// 我的未读消息总数（导航角标）
export function getUnreadTotal() {
  return request({ url: '/teach/msg/unreadTotal', method: 'get' })
}
// 可联系人列表（学生=指导教师+本组成员；教师=本班学生）
export function listBuddies() {
  return request({ url: '/teach/msg/buddies', method: 'get' })
}
// 发起/进入单聊（无会话则懒创建，返回会话ID）
export function startSingle(targetUserId) {
  return request({ url: '/teach/msg/start/' + targetUserId, method: 'post' })
}

// ---------------- 模块讨论 ----------------
// 话题列表（可选按模块编号过滤）
export function listTopics(moduleCode) {
  return request({ url: '/teach/topic/list', method: 'get', params: { moduleCode: moduleCode } })
}
// 学生发起话题（moduleCode 1-10，title ≤200字）
export function createTopic(data) {
  return request({ url: '/teach/topic', method: 'post', data: data })
}
// 话题回复列表（时间正序）
export function listReplies(topicId) {
  return request({ url: '/teach/topic/' + topicId + '/replies', method: 'get' })
}
// 回复话题（replyId 为空=首层回复）
export function replyTopic(topicId, data) {
  return request({ url: '/teach/topic/' + topicId + '/reply', method: 'post', data: data })
}

// ---------------- 班级公告 ----------------
// 公告列表（学生=本班；教师=我指导的全部班级）
export function listNotices() {
  return request({ url: '/teach/notice/list', method: 'get' })
}
// 教师发布公告（发布后服务端向该班在线学生推送 WS 提醒）
export function publishNotice(data) {
  return request({ url: '/teach/notice', method: 'post', data: data })
}
