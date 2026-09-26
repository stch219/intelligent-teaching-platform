<template>
  <div class="sidebar-logo-container" :class="{ 'collapse': collapse }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <!-- 品牌图标：学士帽（平台 Logo，折叠态仅显示图标） -->
        <svg class="sidebar-logo" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M22 9L24 10.5V16H22V9ZM2 9L12 3L22 9V10.5L12 16.5L2 10.5V9ZM6 13.5V16.5C6 17.6046 8.68629 19.5 12 19.5C15.3137 19.5 18 17.6046 18 16.5V13.5L12 17.25L6 13.5Z"/>
        </svg>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <!-- 品牌图标 + 平台名称 -->
        <svg class="sidebar-logo" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M22 9L24 10.5V16H22V9ZM2 9L12 3L22 9V10.5L12 16.5L2 10.5V9ZM6 13.5V16.5C6 17.6046 8.68629 19.5 12 19.5C15.3137 19.5 18 17.6046 18 16.5V13.5L12 17.25L6 13.5Z"/>
        </svg>
        <h1 class="sidebar-title">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import useSettingsStore from '@/store/modules/settings'
import variables from '@/assets/styles/variables.module.scss'

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

// 平台名称取自环境变量（.env 中 VITE_APP_TITLE）
const title = import.meta.env.VITE_APP_TITLE
const settingsStore = useSettingsStore()
const sideTheme = computed(() => settingsStore.sideTheme)

// 获取Logo背景色
const getLogoBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-bg)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightBg
  }
  return sideTheme.value === 'theme-dark' ? variables.menuBg : variables.menuLightBg
})

// 获取Logo文字颜色
const getLogoTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-logo-text)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightText
  }
  return sideTheme.value === 'theme-dark' ? '#fff' : variables.menuLightText
})
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 50px;
  line-height: 50px;
  background: v-bind(getLogoBackground);
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: v-bind(getLogoTextColor);
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>