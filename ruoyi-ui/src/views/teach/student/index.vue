<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：班级→小组层级树 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input v-model="treeFilter" placeholder="筛选班级/小组" clearable style="margin-bottom: 10px" />
        </div>
        <div class="head-container">
          <el-tree :data="treeData" :props="{ label: 'label', children: 'children' }"
            :expand-on-click-node="false" :filter-node-method="filterNode" ref="treeRef"
            highlight-current default-expand-all @node-click="handleNodeClick" />
        </div>
      </el-col>

      <!-- 右侧：学生列表 -->
      <el-col :span="20" :xs="24">
        <!-- 搜索区 -->
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
          <el-form-item label="学生姓名" prop="nickName">
            <el-input v-model="queryParams.nickName" placeholder="请输入学生姓名" clearable style="width: 160px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="学号" prop="studentNo">
            <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width: 160px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="账号状态" clearable style="width: 140px">
              <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 工具栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['teach:student:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['teach:student:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['teach:student:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['teach:student:import']">导入</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['teach:student:export']">导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <!-- 学生表格 -->
        <el-table v-loading="loading" :data="studentList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="学号" align="center" prop="studentNo" width="120" />
          <el-table-column label="姓名" align="center" prop="nickName" />
          <el-table-column label="班级" align="center" prop="className" />
          <el-table-column label="小组" align="center" prop="groupName">
            <template #default="scope">
              <span>{{ scope.row.groupName || '未分组' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="联系电话" align="center" prop="phonenumber" />
          <el-table-column label="状态" align="center" prop="status" width="90">
            <template #default="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
                :disabled="!checkPermi(['teach:student:edit'])" @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
          <el-table-column label="操作" width="220" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['teach:student:edit']">修改</el-button>
              <el-button link type="primary" icon="Key" @click="handleResetPwd(scope.row)" v-hasPermi="['teach:student:resetPwd']">重置密码</el-button>
              <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['teach:student:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>

    <!-- 新增/修改学生弹窗 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="studentRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="学号（同时作为登录账号）" :disabled="form.userId != null" maxlength="20" />
        </el-form-item>
        <el-form-item label="学生姓名" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入学生姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <!-- 班级下拉：从层级树取一级节点 -->
          <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="item in classOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phonenumber">
          <el-input v-model="form.phonenumber" placeholder="请输入联系电话" maxlength="11" />
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
        <el-form-item label="学生姓名">
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

    <!-- Excel 导入弹窗（复用若依封装组件，含模板下载与覆盖更新选项） -->
    <excel-import-dialog
      ref="importRef"
      title="学生导入"
      action="/teach/student/importData"
      template-action="/teach/student/importTemplate"
      template-file-name="student_template"
      update-support-label="是否更新已经存在的学生数据"
      @success="onImportSuccess"
    />
  </div>
</template>

<script setup name="Student">
// ============================================================================
// 【功能】学生管理页面（管理员端）
// ----------------------------------------------------------------------------
// 【说明】左侧"班级→小组"层级树导航（点击节点过滤学生）；
//         右侧学生列表支持新增/修改/启用禁用/重置密码/删除（二次确认）/
//         Excel 批量导入（含模板下载）/导出。
// ============================================================================
import { listStudent, classGroupTree, getStudent, addStudent, updateStudent, changeStudentStatus, resetStudentPwd, delStudent } from "@/api/teach/student"
import { checkPermi } from "@/utils/permission"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict("sys_normal_disable")

const studentList = ref([])
const treeData = ref([])          // 层级树原始数据（班级+小组平铺，前端组装成树）
const classOptions = ref([])      // 班级下拉选项（树的一级节点）
const treeFilter = ref("")
const treeRef = ref(null)
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
const importRef = ref(null)

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
    classId: undefined,
    groupId: undefined,
    nickName: undefined,
    studentNo: undefined,
    status: undefined
  },
  rules: {
    studentNo: [{ required: true, message: "学号不能为空", trigger: "blur" }],
    nickName: [{ required: true, message: "学生姓名不能为空", trigger: "blur" }]
  }
})
const { queryParams, form, rules } = toRefs(data)

/** 树节点筛选关键字 */
watch(treeFilter, val => {
  treeRef.value && treeRef.value.filter(val)
})

/** 树筛选方法 */
function filterNode(value, data) {
  if (!value) return true
  return data.label && data.label.indexOf(value) !== -1
}

/** 查询学生列表 */
function getList() {
  loading.value = true
  listStudent(queryParams.value).then(response => {
    studentList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 查询层级树并组装为班级→小组两级结构 */
function getTree() {
  classGroupTree().then(response => {
    const nodes = response.data || []
    // 组装：一级班级节点带 children，二级小组挂到对应班级
    classOptions.value = nodes.filter(n => Number(n.level) === 1)
    treeData.value = classOptions.value.map(c => ({
      ...c,
      children: nodes.filter(n => Number(n.level) === 2 && Number(n.parentId) === Number(c.id))
    }))
  })
}

/** 点击树节点：一级传班级ID，二级传小组ID（父级班级ID一并传） */
function handleNodeClick(node) {
  if (Number(node.level) === 1) {
    queryParams.value.classId = node.id
    queryParams.value.groupId = undefined
  } else {
    queryParams.value.classId = node.parentId
    queryParams.value.groupId = node.id
  }
  handleQuery()
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
    studentNo: undefined,
    nickName: undefined,
    classId: undefined,
    phonenumber: undefined
  }
  proxy.resetForm("studentRef")
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置搜索（保留树选择） */
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
  title.value = "新增学生（初始密码为学号后6位）"
}

/** 修改按钮 */
function handleUpdate(row) {
  reset()
  const userId = row.userId || ids.value[0]
  getStudent(userId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改学生"
  })
}

/** 提交新增/修改 */
function submitForm() {
  proxy.$refs["studentRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != null) {
        updateStudent(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
          getTree()
        })
      } else {
        addStudent(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
          getTree()
        })
      }
    }
  })
}

/** 切换账号状态（启用/禁用） */
function handleStatusChange(row) {
  const text = row.status === "0" ? "启用" : "禁用"
  proxy.$modal.confirm(`确认要「${text}」学生「${row.nickName}」的账号吗？`).then(() => {
    return changeStudentStatus(row.userId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(`${text}成功`)
  }).catch(() => {
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
      resetStudentPwd(resetPwdForm.value.userId, resetPwdForm.value.password).then(() => {
        resetPwdOpen.value = false
        proxy.$modal.msgSuccess("密码重置成功")
      })
    }
  })
}

/** 删除学生（强制二次确认） */
function handleDelete(row) {
  const userIds = row.userId ? [row.userId] : ids.value
  proxy.$modal.confirm(`⚠️ 确认删除选中的 ${userIds.length} 名学生？删除后账号与学籍信息将被清除且不可恢复！`).then(() => {
    return delStudent(userIds)
  }).then(() => {
    getList()
    getTree()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 打开导入弹窗 */
function handleImport() {
  importRef.value.open()
}

/** 导入成功后刷新列表与层级树 */
function onImportSuccess() {
  getList()
  getTree()
}

/** 导出学生 Excel */
function handleExport() {
  proxy.download("teach/student/export", { ...queryParams.value }, `student_${new Date().getTime()}.xlsx`)
}

getTree()
getList()
</script>
