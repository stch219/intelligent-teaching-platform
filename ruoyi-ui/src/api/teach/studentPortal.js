import request from '@/utils/request'

// ============================================================================
// 【功能】学生端门户统一 API（阶段4）
// ----------------------------------------------------------------------------
// 【说明】涵盖：我的信息与角色选定 / 公示板 / 10模块协同编辑 /
//         贡献率分配与确认 / 组长分工确认，均走学生门户后端接口
//         （hasRole student，服务端按当前学生班级、小组过滤数据）
// ============================================================================

// ---------------- 我的信息与角色选定 ----------------
// 我的信息（学籍/班级/小组/角色/分工/指导教师/截止时间）
export function getMyInfo() {
  return request({ url: '/teach/student/portal/myInfo', method: 'get' })
}
// 选定组内角色并填报分工（组长唯一校验；分工变化重置确认状态）
export function updateMyRole(data) {
  return request({ url: '/teach/student/portal/myRole', method: 'put', data: data })
}

// ---------------- 公示板 ----------------
// 公示板聚合（本组任务 + 组员分工一览 + 可见资料）
export function getBoard() {
  return request({ url: '/teach/student/portal/board', method: 'get' })
}

// ---------------- 模块协同编辑 ----------------
// 模块看板（10模块设置+内容状态/进度+赋分）
export function listModules() {
  return request({ url: '/teach/student/portal/modules', method: 'get' })
}
// 模块内容详情（任务要求模块自动合成）
export function getModule(moduleCode) {
  return request({ url: '/teach/student/portal/module/' + moduleCode, method: 'get' })
}
// 暂存模块内容（乐观锁+上限校验+进度折算）
export function saveModule(data) {
  return request({ url: '/teach/student/portal/module/save', method: 'put', data: data })
}
// 提交模块内容（下限校验，提交后锁定）
export function submitModule(data) {
  return request({ url: '/teach/student/portal/module/submit', method: 'put', data: data })
}

// ---------------- 贡献率 ----------------
// 贡献率面板（组员+分配行+赋分+是否组长）
export function getContributionPanel() {
  return request({ url: '/teach/student/portal/contribution', method: 'get' })
}
// 组长保存贡献率分配（模块5-10每模块合计必须100%）
export function saveContribution(data) {
  return request({ url: '/teach/student/portal/contribution', method: 'put', data: data })
}
// 组员确认贡献率（一键确认本人全部行）
export function confirmContribution() {
  return request({ url: '/teach/student/portal/contribution/confirm', method: 'put' })
}

// ---------------- 分工确认 ----------------
// 组长确认组员分工
export function confirmDuty(memberUserId) {
  return request({ url: '/teach/student/portal/duty/confirm/' + memberUserId, method: 'post' })
}

// ---------------- 总稿提交与导出（阶段5） ----------------
// 总稿合规预检（4项检查：组长身份/模块完成/分工确认/贡献率）
export function getSubmitPrecheck() {
  return request({ url: '/teach/student/portal/submitPrecheck', method: 'get' })
}
// 提交总稿（预检全过后封面落库锁定 + 小组置已提交）
export function submitFinal() {
  return request({ url: '/teach/student/portal/submitFinal', method: 'post' })
}
// 导出总稿PDF（封面+目录+10模块正文；responseType blob 接收二进制流）
export function exportPdf() {
  return request({
    url: '/teach/student/portal/exportPdf',
    method: 'get',
    responseType: 'blob'
  })
}

// ---------------- 我的成绩（阶段7） ----------------
// 我的成绩（教师发布后可见：组最终分/个人得分/各模块终分/本人贡献率）
export function getMyReview() {
  return request({ url: '/teach/student/portal/myReview', method: 'get' })
}
