import request from '@/utils/request'

// ============================================================================
// 【功能】教师端门户统一 API（阶段3）
// ----------------------------------------------------------------------------
// 【说明】涵盖：班级与分组 / 任务管理与分配 / 模块设置 / 资料发布 /
//         预警规则 / 模块赋分 六大模块，均走教师门户后端接口（hasRole teacher）
// ============================================================================

// ---------------- 班级管理 ----------------
// 查询班级列表（教师仅自己的班级）
export function listClass(query) {
  return request({ url: '/teach/class/list', method: 'get', params: query })
}
// 查询班级详情
export function getClass(classId) {
  return request({ url: '/teach/class/' + classId, method: 'get' })
}
// 新建班级
export function addClass(data) {
  return request({ url: '/teach/class', method: 'post', data: data })
}
// 修改班级
export function updateClass(data) {
  return request({ url: '/teach/class', method: 'put', data: data })
}
// 删除班级
export function delClass(classIds) {
  return request({ url: '/teach/class/' + classIds, method: 'delete' })
}
// 分组看板（小组列表+组内成员）
export function groupBoard(classId) {
  return request({ url: '/teach/class/' + classId + '/groupBoard', method: 'get' })
}
// Excel 名单导入（FormData：file + updateSupport）
export function importStudents(classId, data) {
  return request({
    url: '/teach/class/importStudents/' + classId,
    method: 'post',
    data: data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
// 下载名单导入模板
export function importTemplate() {
  return request({ url: '/teach/class/importTemplate', method: 'post', responseType: 'blob' })
}
// 自动重新分组
export function autoGrouping(classId) {
  return request({ url: '/teach/class/autoGroup/' + classId, method: 'post' })
}
// 手动调整学生分组
export function assignStudent(data) {
  return request({ url: '/teach/class/assignGroup', method: 'put', data: data })
}

// ---------------- 任务管理 ----------------
// 查询任务列表
export function listTask(query) {
  return request({ url: '/teach/task/list', method: 'get', params: query })
}
// 任务详情
export function getTask(id) {
  return request({ url: '/teach/task/' + id, method: 'get' })
}
// 新增任务（编号自动生成）
export function addTask(data) {
  return request({ url: '/teach/task', method: 'post', data: data })
}
// 修改任务
export function updateTask(data) {
  return request({ url: '/teach/task', method: 'put', data: data })
}
// 删除任务
export function delTask(ids) {
  return request({ url: '/teach/task/' + ids, method: 'delete' })
}
// 分配任务到组（整体覆盖）
export function assignTask(data) {
  return request({ url: '/teach/task/assign', method: 'put', data: data })
}
// 查询班级下分配视图（组→任务）
export function assignList(classId) {
  return request({ url: '/teach/task/assignList/' + classId, method: 'get' })
}

// ---------------- 模块设置 ----------------
// 查询班级模块设置（10条，无记录返回默认）
export function listModuleSet(classId) {
  return request({ url: '/teach/moduleset/' + classId, method: 'get' })
}
// 批量保存模块设置
export function saveModuleSet(classId, data) {
  return request({ url: '/teach/moduleset/' + classId, method: 'put', data: data })
}

// ---------------- 资料发布 ----------------
// 查询资料列表
export function listMaterial(query) {
  return request({ url: '/teach/material/list', method: 'get', params: query })
}
// 新增资料记录
export function addMaterial(data) {
  return request({ url: '/teach/material', method: 'post', data: data })
}
// 修改资料（名称/可见性）
export function updateMaterial(data) {
  return request({ url: '/teach/material', method: 'put', data: data })
}
// 删除资料
export function delMaterial(ids) {
  return request({ url: '/teach/material/' + ids, method: 'delete' })
}

// ---------------- 预警规则 ----------------
// 查询班级下预警规则
export function listWarningRule(classId) {
  return request({ url: '/teach/warningrule/list/' + classId, method: 'get' })
}
// 新增规则
export function addWarningRule(data) {
  return request({ url: '/teach/warningrule', method: 'post', data: data })
}
// 修改规则
export function updateWarningRule(data) {
  return request({ url: '/teach/warningrule', method: 'put', data: data })
}
// 删除规则
export function delWarningRule(ids) {
  return request({ url: '/teach/warningrule/' + ids, method: 'delete' })
}

// ---------------- 模块赋分 ----------------
// 查询班级赋分设置（模块5-10）
export function listScoreSet(classId) {
  return request({ url: '/teach/scoreset/' + classId, method: 'get' })
}
// 批量保存赋分（总分必须=100）
export function saveScoreSet(classId, data) {
  return request({ url: '/teach/scoreset/' + classId, method: 'put', data: data })
}
