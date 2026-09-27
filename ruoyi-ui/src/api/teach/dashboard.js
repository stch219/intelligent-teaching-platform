// ============================================================================
// 【功能】阶段8 仪表盘/三级预警/帮助中心 API
// ----------------------------------------------------------------------------
// 【说明】三端仪表盘统计、教师预警扫描与记录处置、学生本组预警、
//         帮助中心查询与管理员维护。
// ============================================================================
import request from '@/utils/request'

// ==================== 仪表盘统计 ====================

// 管理员端全平台统计
export function adminDashboard() {
  return request({ url: '/teach/dashboard/admin', method: 'get' })
}

// 教师端本班教学总览
export function teacherDashboard(classId) {
  return request({ url: '/teach/dashboard/teacher/' + classId, method: 'get' })
}

// 学生端个人统计
export function studentDashboard() {
  return request({ url: '/teach/dashboard/student', method: 'get' })
}

// ==================== 三级预警 ====================

// 教师触发班级扫描（判定+落库+实时推送，返回触发明细）
export function scanWarning(classId) {
  return request({ url: '/teach/warning/scan/' + classId, method: 'post' })
}

// 教师查询班级预警记录列表
export function listWarningRecords(classId) {
  return request({ url: '/teach/warning/records/' + classId, method: 'get' })
}

// 教师标记预警记录已处置/忽略
export function resolveWarning(id) {
  return request({ url: '/teach/warning/resolve/' + id, method: 'put' })
}

// 学生查询本组预警记录
export function myWarnings() {
  return request({ url: '/teach/warning/my', method: 'get' })
}

// ==================== 帮助中心 ====================

// 帮助条目列表（师生仅已发布；管理端传 all=true 查全量）
export function listHelp(query) {
  return request({ url: '/teach/help/list', method: 'get', params: query })
}

// 新增帮助条目（管理员）
export function addHelp(data) {
  return request({ url: '/teach/help', method: 'post', data: data })
}

// 修改帮助条目（管理员）
export function updateHelp(data) {
  return request({ url: '/teach/help', method: 'put', data: data })
}

// 删除帮助条目（管理员）
export function delHelp(ids) {
  return request({ url: '/teach/help/' + ids, method: 'delete' })
}
