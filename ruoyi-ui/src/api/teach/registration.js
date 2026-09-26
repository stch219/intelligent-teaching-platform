// ============================================================================
// 【功能】学生注册审批 API
// ----------------------------------------------------------------------------
// 【说明】对接后端 /teach/registration 接口：申请列表、详情、审批通过、
//         审批驳回、删除。学生匿名注册接口在登录页直接调用 /teach/register。
// ============================================================================
import request from '@/utils/request'

// 查询注册申请列表（支持姓名/学号/班级/状态筛选）
export function listRegistration(query) {
  return request({
    url: '/teach/registration/list',
    method: 'get',
    params: query
  })
}

// 查询注册申请详情
export function getRegistration(id) {
  return request({
    url: '/teach/registration/' + id,
    method: 'get'
  })
}

// 审批通过（后端自动创建学生登录账号）
export function approveRegistration(id) {
  return request({
    url: '/teach/registration/approve/' + id,
    method: 'put'
  })
}

// 审批驳回（需携带驳回原因）
export function rejectRegistration(data) {
  return request({
    url: '/teach/registration/reject',
    method: 'put',
    data: data
  })
}

// 删除注册申请（支持批量）
export function delRegistration(ids) {
  return request({
    url: '/teach/registration/' + ids,
    method: 'delete'
  })
}
