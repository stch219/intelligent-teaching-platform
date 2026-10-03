import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import useTagsViewStore from '@/store/modules/tagsView'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

/**
 * 【端访问隔离】按角色返回各自门户首页：
 * 教师→教师工作台；学生→学生工作台；管理员→管理员首页（若依 /index）
 */
const portalHome = (roles) => {
  if (roles.includes('teacher')) return '/teacher/index'
  if (roles.includes('student')) return '/student/index'
  return '/index'
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册；
        // 教师/学生的门户分流在下方统一执行（login.vue 已提前拉取过 roles 时
        // 本分支不会进入，分流逻辑不能只写在这里，否则首页登录会被带到 /index）
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err)
        return { path: '/' }
      }
    }
    // 【门户分流】按角色门户分流：教师进入教师门户，学生进入学生门户
    // （不进入管理员首页 /index）；管理员无需分流，/index 即管理员首页。
    // 每次导航都判断，兼容 roles 已提前加载（登录页校验时拉取过）的场景
    const currentRoles = useUserStore().roles
    if (to.path === '/' || to.path === '/index') {
      if (currentRoles.includes('teacher') || currentRoles.includes('student')) {
        NProgress.done()
        return { path: portalHome(currentRoles), replace: true }
      }
    }
    // 【端访问隔离】教师/学生门户按角色准入，越端访问一律弹回各自门户首页：
    // meta.roles 只能过滤侧边栏菜单、拦不住导航，因此手输 URL 或登录后残留的
    // redirect 参数（如 /login?redirect=/student/index）会把账号带进他端页面
    //（曾致管理员落入学生工作台报"当前账号没有学生档案"），此处做统一拦截
    if (to.path.startsWith('/student') && !currentRoles.includes('student')) {
      NProgress.done()
      return { path: portalHome(currentRoles), replace: true }
    }
    // 教师门户允许管理员进入：管理员首页「课程设计管理」快捷入口按设计指向教师门户
    if (to.path.startsWith('/teacher') && !currentRoles.includes('teacher') && !currentRoles.includes('admin')) {
      NProgress.done()
      return { path: portalHome(currentRoles), replace: true }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    // 【跨账号缓存隔离】token 静默过期时这里是 Vue Router 软跳转（页面不刷新），
    // pinia 与 KeepAlive 缓存都会原样保留；必须清空标签页与缓存组件实例，
    // 防止换账号登录后复用上一账号的页面数据（如消息中心显示他人会话）
    useTagsViewStore().delAllViews()
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
