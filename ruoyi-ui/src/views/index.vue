<template>
  <!-- ============================================================================
       【功能】管理员端首页（平台欢迎工作台）
       ----------------------------------------------------------------------------
       【说明】替换若依默认介绍页：顶部品牌横幅 + 管理员常用功能快捷入口卡片，
               点击卡片跳转对应管理页面。三端统一深蓝品牌色（#1d2b3a）。
       ============================================================================ -->
  <div class="home-wrap">
    <!-- 品牌横幅：深蓝渐变 + 平台简介 -->
    <div class="hero">
      <svg class="hero-logo" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M22 9L24 10.5V16H22V9ZM2 9L12 3L22 9V10.5L12 16.5L2 10.5V9ZM6 13.5V16.5C6 17.6046 8.68629 19.5 12 19.5C15.3137 19.5 18 17.6046 18 16.5V13.5L12 17.25L6 13.5Z"/>
      </svg>
      <div>
        <h1 class="hero-title">智能教学平台</h1>
        <p class="hero-sub">《汽车理论》项目制课程设计全过程管理 · 注册审批 / 账号管理 / 教学过程监控</p>
      </div>
    </div>

    <!-- 快捷入口卡片：管理员四大常用功能 -->
    <el-row :gutter="16" class="entry-row">
      <el-col :sm="12" :lg="6" v-for="item in entries" :key="item.path">
        <el-card class="entry-card" shadow="hover" @click="$router.push(item.path)">
          <div class="entry-icon" :style="{ background: item.color }">
            <svg-icon :icon-class="item.icon" />
          </div>
          <div class="entry-info">
            <div class="entry-name">{{ item.name }}</div>
            <div class="entry-desc">{{ item.desc }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 说明文字 -->
    <el-alert
      class="tip"
      type="info"
      :closable="false"
      show-icon
      title="管理员负责注册审批与账号管理；教学过程（建班分组、任务分配、模块设置等）由指导教师在「课程设计管理」中完成。"
    />
  </div>
</template>

<script setup>
// 快捷入口配置：名称 / 描述 / 路由 / 图标 / 主题色
const entries = [
  { name: '注册审批', desc: '审批学生注册申请', path: '/teach/registration', icon: 'form', color: '#409EFF' },
  { name: '教师管理', desc: '教师账号管理（上限4名）', path: '/teach/teacher', icon: 'peoples', color: '#67C23A' },
  { name: '学生管理', desc: '学生账号层级管理', path: '/teach/student', icon: 'user', color: '#E6A23C' },
  { name: '课程设计管理', desc: '教师教学过程门户', path: '/teacher/index', icon: 'education', color: '#9B59E6' }
]
</script>

<style lang="scss" scoped>
/* ==================== 平台首页样式 ==================== */
.home-wrap { padding: 4px; }

/* 品牌横幅：深蓝渐变与登录页/侧边栏同色系 */
.hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 34px 30px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(120deg, #1d2b3a 0%, #2c4a63 60%, #3a6389 100%);

  .hero-logo { width: 52px; height: 52px; color: #ffd04b; flex-shrink: 0; }
  .hero-title { margin: 0 0 6px; font-size: 26px; letter-spacing: 2px; }
  .hero-sub { margin: 0; font-size: 14px; opacity: 0.85; }
}

/* 快捷入口卡片 */
.entry-row { margin-top: 16px; }

.entry-card {
  cursor: pointer;
  margin-bottom: 16px;

  :deep(.el-card__body) { display: flex; align-items: center; gap: 14px; padding: 20px; }

  &:hover { transform: translateY(-3px); transition: all 0.2s; }

  .entry-icon {
    width: 46px; height: 46px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: #fff; font-size: 22px; flex-shrink: 0;
  }
  .entry-name { font-size: 15px; font-weight: 600; color: #1d2b3a; }
  .entry-desc { font-size: 12px; color: #86909c; margin-top: 4px; }
}

.tip { margin-top: 6px; }
</style>
