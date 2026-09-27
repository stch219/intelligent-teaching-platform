// ============================================================================
// 【功能】WebSocket 连接管理工具（阶段6消息系统）
// ----------------------------------------------------------------------------
// 【说明】封装与后端 /websocket/message 端点的长连接：
//         · connectWs()  建立连接（自动携带登录token），重复调用幂等
//         · onWsMsg(fn)  注册统一消息分发回调，返回取消注册函数
//         · sendWs(obj)  发送 JSON 报文（连接未就绪时静默丢弃）
//         · closeWs()    页面退出时主动断开（停止心跳与重连）
//         内置 25 秒心跳保活与断线 3 秒自动重连。
// ============================================================================
import { getToken } from '@/utils/auth'

// 组件内统一的 ws 单例与状态
let ws = null            // WebSocket 实例
let pingTimer = null     // 心跳定时器
let retryTimer = null    // 重连定时器
let manualClosed = false // 是否主动关闭（主动关闭不再重连）
const handlers = []      // 消息分发回调列表

/**
 * 建立连接（token 通过 URL 参数携带，因为 WS 握手无法自定义请求头）
 */
export function connectWs() {
  // 已连接或连接中则不重复建连
  if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
    return
  }
  const token = getToken()
  if (!token) return
  manualClosed = false
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/websocket/message?token=${token}`)

  // 连接建立：重置重试计数并启动心跳
  ws.onopen = () => {
    startPing()
  }
  // 收到服务端报文：统一 JSON 解析后分发给所有订阅者
  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      handlers.forEach((fn) => fn(data))
    } catch (ignored) {}
  }
  // 连接关闭：非主动关闭则安排重连
  ws.onclose = () => {
    stopPing()
    if (!manualClosed) scheduleReconnect()
  }
  // 连接异常：直接关闭触发 onclose 走统一重连
  ws.onerror = () => {
    try { ws.close() } catch (ignored) {}
  }
}

/**
 * 注册消息回调；返回解绑函数供组件 onUnmounted 时清理
 */
export function onWsMsg(fn) {
  handlers.push(fn)
  return () => {
    const i = handlers.indexOf(fn)
    if (i >= 0) handlers.splice(i, 1)
  }
}

/**
 * 发送 JSON 报文；仅在连接就绪时发送
 */
export function sendWs(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(obj))
    return true
  }
  return false
}

/**
 * 主动断开（组件卸载/登出时调用），不再自动重连
 */
export function closeWs() {
  manualClosed = true
  stopPing()
  if (retryTimer) { clearTimeout(retryTimer); retryTimer = null }
  if (ws) { try { ws.close() } catch (ignored) {} ws = null }
}

// ---------- 内部工具 ----------

// 启动心跳：每25秒发一次 ping，防止代理/防火墙掐断空闲连接
function startPing() {
  stopPing()
  pingTimer = setInterval(() => sendWs({ type: 'ping' }), 25000)
}

// 停止心跳
function stopPing() {
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
}

// 断线3秒后自动重连（课程项目用固定间隔，够用且实现简单）
function scheduleReconnect() {
  if (retryTimer) return
  retryTimer = setTimeout(() => {
    retryTimer = null
    connectWs()
  }, 3000)
}
