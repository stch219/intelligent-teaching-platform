<template>
  <!-- ============================================================================
       【功能】教师端独立门户布局（顶栏式，非若依后台样式）
       ----------------------------------------------------------------------------
       【说明】与登录页一致的品牌设计语言：深蓝顶栏 + 白色内容区。
              左侧平台标识，中部导航菜单（router 模式），
              右侧教师姓名下拉（退出登录）。内容区通过 <router-view> 承载子页。
       ============================================================================ -->
  <div class="teacher-portal">
    <!-- 顶部导航栏 -->
    <header class="portal-header">
      <div class="header-inner">
        <!-- 左侧品牌区 -->
        <div class="brand" @click="router.push('/teacher/index')">
          <div class="brand-logo">智</div>
          <div class="brand-text">
            <div class="brand-name">智能教学平台</div>
            <div class="brand-tag">教师端</div>
          </div>
        </div>
        <!-- 中部导航菜单（router 模式，点击跳转） -->
        <el-menu mode="horizontal" router :default-active="activeMenu" class="portal-menu" :ellipsis="false">
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">{{ m.title }}</el-menu-item>
        </el-menu>
        <!-- 右侧用户区 -->
        <div class="user-area">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-name">
              <el-icon><Avatar /></el-icon>
              {{ userStore.nickName || userStore.name }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 内容区：承载各功能子页 -->
    <main class="portal-main">
      <router-view />
    </main>
  </div>
</template>

<script setup name="TeacherLayout">
// ============================================================================
// 【功能】教师门户布局逻辑：导航高亮 + 退出登录
// ============================================================================
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 顶部导航菜单配置（与静态路由 /teacher 子路径一致）
const menus = [
  { path: '/teacher/index', title: '工作台' },
  { path: '/teacher/class', title: '班级与分组' },
  { path: '/teacher/task', title: '任务管理' },
  { path: '/teacher/module', title: '模块设置' },
  { path: '/teacher/material', title: '资料发布' },
  { path: '/teacher/warning', title: '预警规则' },
  { path: '/teacher/score', title: '模块赋分' }
]

// 当前激活菜单（按路由路径高亮）
const activeMenu = computed(() => route.path)

/** 退出登录（二次确认后清除token回到登录页） */
function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
      userStore.logOut().then(() => {
        router.push('/login')
      })
    }).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
/* ==================== 门户整体：深蓝顶栏 + 浅灰内容区 ==================== */
.teacher-portal {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶栏 */
.portal-header {
  background: #1d2b3a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 24px;
}

/* 品牌区 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-right: 32px;
  flex-shrink: 0;
}
.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2e4a6b, #1d2b3a);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.brand-name {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.brand-tag {
  color: #9db4c8;
  font-size: 12px;
}

/* 导航菜单（覆盖 element 默认样式融入深蓝顶栏） */
.portal-menu {
  flex: 1;
  background: transparent;
  border-bottom: none;
  :deep(.el-menu-item) {
    color: #c4d2df;
    border-bottom: 2px solid transparent;
    background: transparent;
    height: 60px;
    line-height: 60px;
    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.06);
    }
    &.is-active {
      color: #fff;
      border-bottom-color: #4a90d9;
    }
  }
}

/* 用户区 */
.user-area {
  flex-shrink: 0;
  margin-left: 16px;
}
.user-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

/* 内容区 */
.portal-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
}
</style>
