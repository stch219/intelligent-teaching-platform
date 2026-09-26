<template>
  <!-- ============================================================================
       【功能】平台登录页（独立设计，非若依默认样式）
       ----------------------------------------------------------------------------
       【说明】按角色门户设计：选择角色卡片（学生端/教师端/管理员）后输入账号密码登录；
              "注册"页签供学生填写班级/学号等信息提交申请，等待管理员审批。
       ============================================================================ -->
  <div class="portal-login">
    <!-- 左侧平台品牌区 -->
    <div class="brand-area">
      <div class="brand-title">智能教学平台</div>
      <div class="brand-sub">《汽车理论》项目制课程设计全过程管理</div>
      <div class="brand-points">
        <div class="point">· 在线协同完成课程设计十大模块</div>
        <div class="point">· AI 辅助批改与智能答疑</div>
        <div class="point">· 全过程进度监控与三级预警</div>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="login-card">
      <h2 class="card-title">欢迎登录</h2>
      <p class="card-sub">请选择您的角色并输入账号密码</p>

      <!-- 角色选择卡片：默认选中教师端样式可切换 -->
      <div class="role-label">选择角色</div>
      <div class="role-cards">
        <div v-for="r in roleList" :key="r.key" class="role-card" :class="{ active: role === r.key }" @click="role = r.key">
          <el-icon :size="22"><component :is="r.icon" /></el-icon>
          <span>{{ r.name }}</span>
        </div>
      </div>

      <!-- 登录 / 注册 页签 -->
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录页签 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" size="large" auto-complete="off" :placeholder="'请输入' + currentRole.name + '账号'">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" size="large" auto-complete="off" placeholder="请输入密码" show-password @keyup.enter="handleLogin">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <!-- 验证码（后端开启时显示） -->
            <el-form-item prop="code" v-if="captchaEnabled">
              <div class="code-row">
                <el-input v-model="loginForm.code" size="large" auto-complete="off" placeholder="验证码" @keyup.enter="handleLogin">
                  <template #prefix><el-icon><Key /></el-icon></template>
                </el-input>
                <img :src="codeUrl" @click="getCode" class="code-img" alt="验证码" />
              </div>
            </el-form-item>
            <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
            <el-button :loading="loading" size="large" class="login-btn" @click.prevent="handleLogin">
              <span v-if="!loading">{{ currentRole.name }}登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册页签（仅学生端显示：注册为审批制，教师/管理员由系统统一分配账号，无注册入口） -->
        <el-tab-pane v-if="role === 'student'" label="注册" name="register">
          <el-form ref="regRef" :model="regForm" :rules="regRules">
            <el-form-item prop="realName">
              <el-input v-model="regForm.realName" size="large" placeholder="请输入真实姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="studentNo">
              <el-input v-model="regForm.studentNo" size="large" placeholder="请输入学号">
                <template #prefix><el-icon><Postcard /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="className">
              <el-input v-model="regForm.className" size="large" placeholder="请输入班级名称（如：车辆2301班）">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="regForm.phone" size="large" placeholder="联系电话（选填）" maxlength="11">
                <template #prefix><el-icon><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-button :loading="regLoading" size="large" class="login-btn" @click.prevent="handleRegister">提交注册申请</el-button>
            <div class="reg-tip">提交后请等待管理员审批，审批通过后即可使用学号登录（初始密码为学号后6位）</div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 底部版权 -->
    <div class="portal-footer">{{ footerContent }}</div>
  </div>
</template>

<script setup name="Login">
// ============================================================================
// 【功能】登录页逻辑：角色选择 + 账号密码登录 + 学生注册申请
// ----------------------------------------------------------------------------
// 【说明】登录仍走若依统一 /login 接口（权限由后端角色决定）；
//         角色卡片仅用于入口引导与按钮文案区分；注册走 /teach/register 匿名接口。
// ============================================================================
import { getCodeImg } from "@/api/login"
import { submitRegistration } from "@/api/teach/registration"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

// 角色卡片配置（key 仅用于文案与样式，登录权限仍由后端决定）
const roleList = [
  { key: "student", name: "学生端", icon: "User" },
  { key: "teacher", name: "教师端", icon: "Avatar" },
  { key: "admin", name: "管理员", icon: "Coordinate" }
]
const role = ref("student")
// 当前选中角色的配置对象（按钮文案用）
const currentRole = computed(() => roleList.find(r => r.key === role.value))

const activeTab = ref("login")
// 仅学生端保留注册页签：切换到教师/管理员角色时自动回到登录页签
watch(role, (val) => {
  if (val !== "student") {
    activeTab.value = "login"
  }
})
const codeUrl = ref("")
const loading = ref(false)
const regLoading = ref(false)
// 验证码开关（由后端返回决定）
const captchaEnabled = ref(true)
const redirect = ref(undefined)

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

// 注册表单与校验规则（学生审批制注册）
const regForm = ref({ realName: "", studentNo: "", className: "", phone: "" })
const regRules = {
  realName: [{ required: true, trigger: "blur", message: "请输入真实姓名" }],
  studentNo: [{ required: true, trigger: "blur", message: "请输入学号" }],
  className: [{ required: true, trigger: "blur", message: "请输入班级名称" }]
}

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

/** 登录提交 */
function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用 action 的登录方法（登录后按 redirect 或进入系统首页）
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 登录失败重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

/** 学生注册申请提交（匿名接口，成功后提示等待审批） */
function handleRegister() {
  proxy.$refs.regRef.validate(valid => {
    if (valid) {
      regLoading.value = true
      submitRegistration(regForm.value).then(() => {
        proxy.$modal.msgSuccess("申请已提交，请等待管理员审批")
        // 清空表单并切回登录页签
        regForm.value = { realName: "", studentNo: "", className: "", phone: "" }
        activeTab.value = "login"
      }).finally(() => {
        regLoading.value = false
      })
    }
  })
}

/** 获取验证码 */
function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

/** 从 cookie 恢复记住的账号密码 */
function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  if (username !== undefined) {
    loginForm.value = {
      username: username,
      password: decrypt(password),
      rememberMe: Boolean(rememberMe)
    }
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
/* ==================== 整体布局：左侧品牌区 + 右侧登录卡片 ==================== */
.portal-login {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 80px;
  height: 100%;
  background: linear-gradient(135deg, #eef2f7 0%, #e3e9f2 100%);
}

/* 品牌区：平台名与亮点介绍 */
.brand-area {
  color: #2b3a55;
  .brand-title {
    font-size: 42px;
    font-weight: 700;
    letter-spacing: 2px;
  }
  .brand-sub {
    margin-top: 14px;
    font-size: 18px;
    color: #5a6b85;
  }
  .brand-points {
    margin-top: 36px;
    font-size: 15px;
    line-height: 2.2;
    color: #7a8aa3;
  }
}

/* 登录卡片 */
.login-card {
  width: 420px;
  background: #ffffff;
  border-radius: 16px;
  padding: 36px 40px 28px;
  box-shadow: 0 12px 40px rgba(43, 58, 85, 0.10);
  z-index: 1;
}
.card-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #2b3a55;
}
.card-sub {
  margin: 8px 0 20px;
  font-size: 14px;
  color: #98a5b8;
}

/* 角色选择卡片：三列网格，选中深色描边高亮 */
.role-label {
  font-size: 14px;
  color: #5a6b85;
  margin-bottom: 10px;
}
.role-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.role-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  border: 2px solid #e8edf4;
  border-radius: 12px;
  cursor: pointer;
  color: #7a8aa3;
  font-size: 14px;
  transition: all 0.2s;
  &:hover {
    border-color: #c6d2e2;
  }
  &.active {
    border-color: #2b3a55;
    color: #2b3a55;
    background: #f6f8fb;
    box-shadow: 0 4px 12px rgba(43, 58, 85, 0.12);
  }
}

/* 页签与表单 */
.login-tabs {
  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
  :deep(.el-tabs__item) {
    font-size: 16px;
    color: #98a5b8;
    &.is-active {
      color: #2b3a55;
    }
  }
  :deep(.el-tabs__active-bar) {
    background-color: #2b3a55;
  }
  .el-form-item {
    margin-bottom: 18px;
  }
}
.code-row {
  display: flex;
  width: 100%;
  gap: 10px;
  .code-img {
    width: 110px;
    height: 40px;
    border-radius: 6px;
    cursor: pointer;
  }
}

/* 登录/注册大按钮：深蓝主色 */
.login-btn {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  font-size: 16px;
  color: #ffffff;
  background: #2b3a55;
  border: none;
  border-radius: 10px;
  &:hover {
    background: #3a4d6f;
  }
}

/* 注册页提示文字 */
.reg-tip {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.6;
  color: #98a5b8;
  text-align: center;
}

/* 底部版权 */
.portal-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: #a5b0c2;
  font-size: 12px;
  letter-spacing: 1px;
}

/* 窄屏适配：隐藏品牌区，卡片居中 */
@media (max-width: 900px) {
  .portal-login {
    gap: 0;
  }
  .brand-area {
    display: none;
  }
}
</style>
