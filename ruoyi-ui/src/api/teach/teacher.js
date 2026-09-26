// ============================================================================
// 【功能】教师管理 API
// ----------------------------------------------------------------------------
// 【说明】对接后端 /teach/teacher 接口：教师列表（含指导班级数）、详情、
//         新增（上限4名校验）、修改、状态切换、重置密码、删除、导出。
// ============================================================================
import request from '@/utils/request'

// 查询教师列表
export function listTeacher(query) {
  return request({
    url: '/teach/teacher/list',
    method: 'get',
    params: query
  })
}

// 查询教师详情
export function getTeacher(userId) {
  return request({
    url: '/teach/teacher/' + userId,
    method: 'get'
  })
}

// 新增教师
export function addTeacher(data) {
  return request({
    url: '/teach/teacher',
    method: 'post',
    data: data
  })
}

// 修改教师
export function updateTeacher(data) {
  return request({
    url: '/teach/teacher',
    method: 'put',
    data: data
  })
}

// 修改教师账号状态（启用/禁用）
export function changeTeacherStatus(userId, status) {
  return request({
    url: '/teach/teacher/changeStatus',
    method: 'put',
    data: { userId, status }
  })
}

// 重置教师登录密码
export function resetTeacherPwd(userId, password) {
  return request({
    url: '/teach/teacher/resetPwd',
    method: 'put',
    data: { userId, password }
  })
}

// 删除教师（支持批量）
export function delTeacher(userIds) {
  return request({
    url: '/teach/teacher/' + userIds,
    method: 'delete'
  })
}
