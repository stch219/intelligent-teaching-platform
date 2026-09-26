// ============================================================================
// 【功能】学生管理 API
// ----------------------------------------------------------------------------
// 【说明】对接后端 /teach/student 接口：学生列表（班级/小组筛选）、
//         班级→小组层级树、新增、修改、状态切换、重置密码、删除、
//         Excel 导入/导出/模板下载。
// ============================================================================
import request from '@/utils/request'

// 查询学生列表
export function listStudent(query) {
  return request({
    url: '/teach/student/list',
    method: 'get',
    params: query
  })
}

// 查询班级→小组层级树
export function classGroupTree() {
  return request({
    url: '/teach/student/classGroupTree',
    method: 'get'
  })
}

// 查询学生详情
export function getStudent(userId) {
  return request({
    url: '/teach/student/' + userId,
    method: 'get'
  })
}

// 新增学生
export function addStudent(data) {
  return request({
    url: '/teach/student',
    method: 'post',
    data: data
  })
}

// 修改学生
export function updateStudent(data) {
  return request({
    url: '/teach/student',
    method: 'put',
    data: data
  })
}

// 修改学生账号状态（启用/禁用）
export function changeStudentStatus(userId, status) {
  return request({
    url: '/teach/student/changeStatus',
    method: 'put',
    data: { userId, status }
  })
}

// 重置学生登录密码
export function resetStudentPwd(userId, password) {
  return request({
    url: '/teach/student/resetPwd',
    method: 'put',
    data: { userId, password }
  })
}

// 删除学生（支持批量）
export function delStudent(userIds) {
  return request({
    url: '/teach/student/' + userIds,
    method: 'delete'
  })
}
