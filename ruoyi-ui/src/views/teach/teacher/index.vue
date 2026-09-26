<template>
  <div class="app-container">
    <!-- 搜索区：教师姓名/账号/状态 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="教师姓名" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入教师姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="登录账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入登录账号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="账号状态" clearable style="width: 160px">
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏：新增/修改/删除/导出 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['teach:teacher:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['teach:teacher:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['teach:teacher:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['teach:teacher:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 教师列表 -->
    <el-table v-loading="loading" :data="teacherList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="登录账号" align="center" prop="userName" />
      <el-table-column label="教师姓名" align="center" prop="nickName" />
      <el-table-column label="职称" align="center" prop="title" />
      <el-table-column label="联系电话" align="center" prop="phonenumber" />
      <el-table-column label="指导班级数" align="center" prop="classCount" width="100">
        <template #default="scope">
          <!-- 班级数>0 红色提示（该教师不可删除） -->
          <el-tag :type="scope.row.classCount > 0 ? 'danger' : 'info'">{{ scope.row.classCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <!-- 状态开关：直接切换启用/禁用 -->
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
            :disabled="!checkPermi(['teach:teacher:edit'])" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
      <el-table-column label="操作" width="220" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['teach:teacher:edit']">修改</el-button>
          <el-button link type="primary" icon="Key" @click="handleResetPwd(scope.row)" v-hasPermi="['teach:teacher:resetPwd']">重置密码</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['teach:teacher:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改教师弹窗 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="teacherRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="userName">
          <el-input v-model="form.userName" placeholder="教师登录账号" :disabled="form.userId != null" maxlength="30" />
        </el-form-item>
        <el-form-item label="教师姓名" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入教师姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="form.title" placeholder="如：教授 / 副教授 / 讲师" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phonenumber">
          <el-input v-model="form.phonenumber" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog title="重置密码" v-model="resetPwdOpen" width="420px" append-to-body>
      <el-form ref="resetPwdRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="90px">
        <el-form-item label="教师姓名">
          <span>{{ resetPwdForm.nickName }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitResetPwd">确 定</el-button>
        <el-button @click="resetPwdOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Teacher">
// ============================================================================
// 【功能】教师管理页面（管理员端）
// ----------------------------------------------------------------------------
// 【说明】教师账号全平台上限 4 名（后端强校验，超限新增会被拒绝）；
//         删除前二次确认，且名下有指导班级的教师后端会拒绝删除；
//         状态开关直接切换账号启用/禁用。
// ============================================================================
import { listTeacher, getTeacher, addTeacher, updateTeacher, changeTeacherStatus, resetTeacherPwd, delTeacher } from "@/api/teach/teacher"
import { checkPermi } from "@/utils/permission"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict("sys_normal_disable")

const teacherList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const resetPwdOpen = ref(false)
const resetPwdForm = ref({})
const resetPwdRules = {
  password: [
    { required: true, message: "新密码不能为空", trigger: "blur" },
    { min: 5, max: 20, message: "密码长度 5-20 个字符", trigger: "blur" }
  ]
}

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    nickName: undefined,
    userName: undefined,
    status: undefined
  },
  rules: {
    userName: [{ required: true, message: "登录账号不能为空", trigger: "blur" }],
    nickName: [{ required: true, message: "教师姓名不能为空", trigger: "blur" }]
  }
})
const { queryParams, form, rules } = toRefs(data)

/** 查询教师列表 */
function getList() {
  loading.value = true
  listTeacher(queryParams.value).then(response => {
    teacherList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消弹窗 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    userId: undefined,
    userName: undefined,
    nickName: undefined,
    title: undefined,
    phonenumber: undefined,
    email: undefined,
    remark: undefined
  }
  proxy.resetForm("teacherRef")
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置搜索 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选变化 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.userId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增教师（初始密码 123456）"
}

/** 修改按钮（工具栏/行内） */
function handleUpdate(row) {
  reset()
  const userId = row.userId || ids.value[0]
  getTeacher(userId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改教师"
  })
}

/** 提交新增/修改 */
function submitForm() {
  proxy.$refs["teacherRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != null) {
        updateTeacher(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTeacher(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 切换账号状态（启用/禁用） */
function handleStatusChange(row) {
  const text = row.status === "0" ? "启用" : "禁用"
  proxy.$modal.confirm(`确认要「${text}」教师「${row.nickName}」的账号吗？`).then(() => {
    return changeTeacherStatus(row.userId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(`${text}成功`)
  }).catch(() => {
    // 取消后恢复开关状态
    row.status = row.status === "0" ? "1" : "0"
  })
}

/** 打开重置密码弹窗 */
function handleResetPwd(row) {
  resetPwdForm.value = { userId: row.userId, nickName: row.nickName, password: "" }
  resetPwdOpen.value = true
}

/** 提交重置密码 */
function submitResetPwd() {
  proxy.$refs["resetPwdRef"].validate(valid => {
    if (valid) {
      resetTeacherPwd(resetPwdForm.value.userId, resetPwdForm.value.password).then(() => {
        resetPwdOpen.value = false
        proxy.$modal.msgSuccess("密码重置成功")
      })
    }
  })
}

/** 删除教师（强制二次确认；名下有班级后端会拒绝） */
function handleDelete(row) {
  const userIds = row.userId ? [row.userId] : ids.value
  proxy.$modal.confirm(`⚠️ 确认删除选中的 ${userIds.length} 名教师？删除后账号无法登录且不可恢复！`).then(() => {
    return delTeacher(userIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出教师 Excel */
function handleExport() {
  proxy.download("teach/teacher/export", { ...queryParams.value }, `teacher_${new Date().getTime()}.xlsx`)
}

getList()
</script>
