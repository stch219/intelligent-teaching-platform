<template>
  <div class="app-container">
    <!-- 搜索区：姓名/学号/班级 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="学生姓名" prop="realName">
        <el-input v-model="queryParams.realName" placeholder="请输入学生姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="学号" prop="studentNo">
        <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="班级" prop="className">
        <el-input v-model="queryParams.className" placeholder="请输入班级名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 状态页签：全部/待审批/通过/驳回 -->
    <el-tabs v-model="activeTab" @tab-change="handleQuery">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待审批" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已驳回" name="2" />
    </el-tabs>

    <!-- 工具栏：批量删除 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['teach:registration:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 申请列表 -->
    <el-table v-loading="loading" :data="registrationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="申请编号" align="center" prop="id" width="80" />
      <el-table-column label="学生姓名" align="center" prop="realName" />
      <el-table-column label="学号" align="center" prop="studentNo" />
      <el-table-column label="班级" align="center" prop="className" />
      <el-table-column label="联系电话" align="center" prop="phone" />
      <el-table-column label="申请时间" align="center" prop="createTime" width="170" />
      <el-table-column label="审批状态" align="center" prop="status" width="100">
        <template #default="scope">
          <!-- 状态标签：0待审批(黄) 1通过(绿) 2驳回(红) -->
          <el-tag v-if="scope.row.status === '0'">待审批</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批信息" align="center" width="220">
        <template #default="scope">
          <!-- 通过/驳回时展示审批人与时间；驳回额外展示原因 -->
          <div v-if="scope.row.status !== '0'">
            <div>{{ scope.row.approveBy }} · {{ parseTime(scope.row.approveTime, '{y}-{m}-{d}') }}</div>
            <div v-if="scope.row.status === '2'" class="reject-reason">原因：{{ scope.row.rejectReason }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <!-- 仅待审批状态显示通过/驳回按钮 -->
          <template v-if="scope.row.status === '0'">
            <el-button link type="primary" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['teach:registration:approve']">通过</el-button>
            <el-button link type="warning" icon="Close" @click="handleReject(scope.row)" v-hasPermi="['teach:registration:approve']">驳回</el-button>
          </template>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['teach:registration:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 驳回原因弹窗 -->
    <el-dialog title="驳回注册申请" v-model="rejectOpen" width="450px" append-to-body>
      <el-form ref="rejectRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="学生姓名">
          <span>{{ rejectForm.realName }}（{{ rejectForm.studentNo }}）</span>
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（学生可见）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitReject">确认驳回</el-button>
        <el-button @click="rejectOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Registration">
// ============================================================================
// 【功能】学生注册审批页面（管理员端）
// ----------------------------------------------------------------------------
// 【说明】按状态页签展示注册申请；审批通过后系统自动创建学生登录账号
//         （学号即账号，初始密码 123456）；驳回需填写原因。
// ============================================================================
import { listRegistration, approveRegistration, rejectRegistration, delRegistration } from "@/api/teach/registration"

const { proxy } = getCurrentInstance()

const registrationList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const activeTab = ref("all")
const rejectOpen = ref(false)

// 驳回弹窗表单与校验规则
const rejectForm = ref({})
const rejectRules = {
  rejectReason: [{ required: true, message: "驳回原因不能为空", trigger: "blur" }]
}

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    realName: undefined,
    studentNo: undefined,
    className: undefined
  }
})
const { queryParams } = toRefs(data)

/** 查询申请列表（结合当前页签状态过滤） */
function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  if (activeTab.value !== "all") {
    params.status = activeTab.value
  }
  listRegistration(params).then(response => {
    registrationList.value = response.rows
    total.value = response.total
    loading.value = false
  })
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

/** 多选框选中变化 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 审批通过（二次确认后调用后端自动建号） */
function handleApprove(row) {
  proxy.$modal.confirm(`确认通过「${row.realName}（${row.studentNo}）」的注册申请？通过后将自动创建学生账号（初始密码 123456）`).then(() => {
    return approveRegistration(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("审批通过，账号已创建")
  }).catch(() => {})
}

/** 打开驳回弹窗 */
function handleReject(row) {
  rejectForm.value = { id: row.id, realName: row.realName, studentNo: row.studentNo, rejectReason: "" }
  rejectOpen.value = true
}

/** 提交驳回 */
function submitReject() {
  proxy.$refs["rejectRef"].validate(valid => {
    if (valid) {
      rejectRegistration(rejectForm.value).then(() => {
        rejectOpen.value = false
        getList()
        proxy.$modal.msgSuccess("已驳回")
      })
    }
  })
}

/** 删除申请（支持批量，带二次确认） */
function handleDelete(row) {
  const regIds = row.id ? [row.id] : ids.value
  proxy.$modal.confirm(`确认删除选中的 ${regIds.length} 条申请记录？删除后不可恢复`).then(() => {
    return delRegistration(regIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>

<style scoped>
/* 驳回原因小字红色提示 */
.reject-reason {
  color: #f56c6c;
  font-size: 12px;
}
</style>
