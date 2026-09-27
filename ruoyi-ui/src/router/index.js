import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    // 【阶段8帮助中心管理】管理员维护帮助条目（隐藏路由，从管理员首页「帮助管理」快捷入口进入）
    path: '/help-admin',
    component: () => import('@/views/portal/common/HelpAdmin'),
    hidden: true
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    // 教师端门户：与管理员端共用统一侧边栏布局（Layout），
    // meta.roles 控制侧边栏菜单仅教师可见（admin 超管全可见），
    // 页面级接口鉴权由后端 @ss.hasRole('teacher') 保护
    path: '/teacher',
    component: Layout,
    redirect: '/teacher/index',
    meta: { title: '课程设计管理', icon: 'education', roles: ['teacher'] },
    children: [
      { path: 'index', component: () => import('@/views/portal/teacher/index'), name: 'TeacherHome', meta: { title: '教师工作台', icon: 'dashboard' } },
      { path: 'class', component: () => import('@/views/portal/teacher/class/index'), name: 'TeacherClass', meta: { title: '班级与分组', icon: 'peoples' } },
      { path: 'task', component: () => import('@/views/portal/teacher/task/index'), name: 'TeacherTask', meta: { title: '任务管理', icon: 'list' } },
      { path: 'module', component: () => import('@/views/portal/teacher/module/index'), name: 'TeacherModule', meta: { title: '模块设置', icon: 'form' } },
      { path: 'material', component: () => import('@/views/portal/teacher/material/index'), name: 'TeacherMaterial', meta: { title: '资料发布', icon: 'documentation' } },
      { path: 'warning', component: () => import('@/views/portal/teacher/warning/index'), name: 'TeacherWarning', meta: { title: '预警规则', icon: 'bell' } },
      { path: 'score', component: () => import('@/views/portal/teacher/score/index'), name: 'TeacherScore', meta: { title: '模块赋分', icon: 'money' } },
      // 【阶段7 AI批改】本地多模态大模型批改 + 教师终审 + 成绩判分发布
      { path: 'ai', component: () => import('@/views/portal/teacher/ai/index'), name: 'TeacherAiReview', meta: { title: 'AI批改', icon: 'star' } },
      // 【阶段6消息系统】师生共用面板：消息中心/模块讨论/班级公告
      { path: 'message', component: () => import('@/views/portal/common/ChatPanel'), name: 'TeacherMessage', meta: { title: '消息中心', icon: 'message' } },
      { path: 'topic', component: () => import('@/views/portal/common/TopicPanel'), name: 'TeacherTopic', meta: { title: '模块讨论', icon: 'chat' } },
      { path: 'notice', component: () => import('@/views/portal/common/NoticePanel'), name: 'TeacherNotice', meta: { title: '班级公告', icon: 'documentation' } },
      // 【阶段8帮助中心】师生共用静态指引（内容由管理员在 /help-admin 维护）
      { path: 'help', component: () => import('@/views/portal/common/HelpCenter'), name: 'TeacherHelp', meta: { title: '帮助中心', icon: 'question' } }
    ]
  },
  {
    // 学生端门户：与管理员/教师端共用统一侧边栏布局（Layout），
    // meta.roles 控制侧边栏菜单仅学生可见（admin 超管全可见），
    // 页面级接口鉴权由后端 @ss.hasRole('student') 保护
    path: '/student',
    component: Layout,
    redirect: '/student/index',
    meta: { title: '课程设计中心', icon: 'peoples', roles: ['student'] },
    children: [
      { path: 'index', component: () => import('@/views/portal/student/index'), name: 'StudentHome', meta: { title: '学生工作台', icon: 'dashboard' } },
      { path: 'profile', component: () => import('@/views/portal/student/profile/index'), name: 'StudentProfile', meta: { title: '个人中心', icon: 'user' } },
      { path: 'board', component: () => import('@/views/portal/student/board/index'), name: 'StudentBoard', meta: { title: '公示板', icon: 'clipboard' } },
      { path: 'modules', component: () => import('@/views/portal/student/modules/index'), name: 'StudentModules', meta: { title: '模块编辑', icon: 'form' } },
      { path: 'contribution', component: () => import('@/views/portal/student/contribution/index'), name: 'StudentContribution', meta: { title: '贡献率', icon: 'chart' } },
      // 【阶段7成绩查看】教师发布后可查看小组/个人成绩与模块明细
      { path: 'review', component: () => import('@/views/portal/student/review/index'), name: 'StudentReview', meta: { title: '我的成绩', icon: 'money' } },
      // 【阶段6消息系统】师生共用面板：消息中心/模块讨论/班级公告
      { path: 'message', component: () => import('@/views/portal/common/ChatPanel'), name: 'StudentMessage', meta: { title: '消息中心', icon: 'message' } },
      { path: 'topic', component: () => import('@/views/portal/common/TopicPanel'), name: 'StudentTopic', meta: { title: '模块讨论', icon: 'chat' } },
      { path: 'notice', component: () => import('@/views/portal/common/NoticePanel'), name: 'StudentNotice', meta: { title: '班级公告', icon: 'documentation' } },
      // 【阶段8帮助中心】师生共用静态指引（内容由管理员在 /help-admin 维护）
      { path: 'help', component: () => import('@/views/portal/common/HelpCenter'), name: 'StudentHelp', meta: { title: '帮助中心', icon: 'question' } }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
