<template>
  <!-- ============================================================================
       【功能】任务管理页（课程设计任务发布与分配）
       ----------------------------------------------------------------------------
       【说明】任务编号按字母自动生成（A/B/C/D…最多8个）；内容含设计目标/
              具体要求/评分标准/重要提示四要素；"分配"将任务指派到组
              （一组只领一个任务，一个任务可给多组）。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">任务管理</h2>
        <p class="page-sub">按班级发布课程设计任务，并分配到各小组（题目由教师分配，取消组长选题）</p>
      </div>
      <el-space>
        <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadAll">
          <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!classId" @click="openDialog()">新建任务</el-button>
      </el-space>
    </div>

    <!-- 任务表格 -->
    <el-card shadow="never" v-loading="loading">
      <el-table :data="taskList" v-if="classId">
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            <!-- 字母编号标签（A~H 颜色区分） -->
            <el-tag :color="codeColor(scope.row.taskCode)" effect="dark" class="code-tag">{{ scope.row.taskCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskName" label="任务题目" min-width="220" show-overflow-tooltip />
        <!-- 【阶段8三级预警】截止时间是扫描引擎的判定依据（距截止N天/已逾期） -->
        <el-table-column label="截止时间" width="170" align="center">
          <template #default="scope">
            <span v-if="scope.row.deadline">{{ scope.row.deadline }}</span>
            <el-tag v-else type="info" size="small">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="发布教师" width="110" />
        <el-table-column label="已分配组数" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.assignedCount > 0 ? 'success' : 'info'">{{ scope.row.assignedCount }} 组</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
            <el-button link type="success" @click="openAssign(scope.row)">分配</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="请先选择班级" :image-size="90" />
    </el-card>

    <!-- 新建/编辑任务对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑任务' : '新建任务'" width="680px" top="6vh">
      <el-form ref="taskRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="任务题目" prop="taskName">
          <el-input v-model="form.taskName" placeholder="如：汽车动力性计算与分析" maxlength="200" show-word-limit />
        </el-form-item>
        <!-- 【阶段8三级预警】设置总稿提交截止时间，预警扫描引擎据此判定黄/橙/红 -->
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择总稿提交截止时间（选填）"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="设计目标" prop="designGoal">
          <el-input v-model="form.designGoal" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="具体要求" prop="requirement">
          <el-input v-model="form.requirement" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="评分标准" prop="gradingStandard">
          <el-input v-model="form.gradingStandard" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="重要提示" prop="tips">
          <el-input v-model="form.tips" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框：勾选小组（整体覆盖提交） -->
    <el-dialog v-model="assignVisible" :title="'任务 ' + (assignTaskRow?.taskCode || '') + ' 分配到组'" width="460px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
        title="每组同一时间只能领取一个任务；勾选后保存，未勾选的组将被取消本任务分配。" />
      <el-checkbox-group v-model="assignGroupIds">
        <div class="assign-grid">
          <el-checkbox v-for="g in groupOptions" :key="g.id" :value="g.id" class="assign-item">
            {{ g.groupName }}（{{ g.memberCount }}人）
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">保存分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TeacherTask">
// ============================================================================
// 【功能】任务管理逻辑：任务CRUD（编号自动生成）+ 分配到组
// ============================================================================
import { listClass, listTask, addTask, updateTask, delTask, assignTask, assignList, groupBoard } from '@/api/teach/portal'

const { proxy } = getCurrentInstance()

// 班级与任务数据
const classList = ref([])
const classId = ref(null)
const taskList = ref([])
const loading = ref(false)

// 任务编号 → 标签颜色（8色对应 A~H）
const CODE_COLORS = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59E6', '#00B4D8', '#E91E63', '#795548']

// 新建/编辑表单
const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({})
const rules = {
  taskName: [{ required: true, message: '请输入任务题目', trigger: 'blur' }]
}

// 分配对话框
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignTaskRow = ref(null)
const assignGroupIds = ref([])
const groupOptions = ref([])

/** 加载班级下拉 */
async function loadClassOptions() {
  const res = await listClass()
  classList.value = res.data || []
  if (!classId.value && classList.value.length > 0) {
    classId.value = classList.value[0].id
  }
  if (classId.value) loadAll()
}

/** 加载任务列表与分配视图 */
async function loadAll() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await listTask({ classId: classId.value })
    taskList.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 打开新建/编辑对话框 */
function openDialog(row) {
  // 新建时 deadline 置空（截止时间选填，阶段8预警扫描依赖）
  form.value = row ? { ...row } : { classId: classId.value, taskName: '', deadline: null, designGoal: '', requirement: '', gradingStandard: '', tips: '' }
  dialogVisible.value = true
}

/** 提交任务 */
function submit() {
  proxy.$refs.taskRef.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (form.value.id) {
        await updateTask(form.value)
        proxy.$modal.msgSuccess('修改成功')
      } else {
        await addTask(form.value)
        proxy.$modal.msgSuccess('创建成功，编号已自动生成')
      }
      dialogVisible.value = false
      loadAll()
    } finally {
      submitLoading.value = false
    }
  })
}

/** 删除任务（已分配时后端拒绝） */
function handleDelete(row) {
  proxy.$modal.confirm('确定删除任务 ' + row.taskCode + '「' + row.taskName + '」吗？').then(async () => {
    await delTask(row.id)
    proxy.$modal.msgSuccess('删除成功')
    loadAll()
  }).catch(() => {})
}

/** 打开分配对话框（回显当前分配） */
async function openAssign(row) {
  assignTaskRow.value = row
  // 拉取小组选项与当前分配记录
  const [gRes, aRes] = await Promise.all([groupBoard(classId.value), assignList(classId.value)])
  groupOptions.value = gRes.data || []
  const current = (aRes.data || []).filter(a => a.taskId === row.id).map(a => a.groupId)
  assignGroupIds.value = current
  assignVisible.value = true
}

/** 保存分配（整体覆盖） */
async function submitAssign() {
  assignLoading.value = true
  try {
    await assignTask({ id: assignTaskRow.value.id, groupIds: assignGroupIds.value })
    proxy.$modal.msgSuccess('分配成功')
    assignVisible.value = false
    loadAll()
  } finally {
    assignLoading.value = false
  }
}

/** 编号转颜色（超出取模循环） */
function codeColor(code) {
  const idx = (code ? code.charCodeAt(0) - 65 : 0) % CODE_COLORS.length
  return CODE_COLORS[idx]
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== 任务管理页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }

.code-tag { border: none; color: #fff; font-weight: 700; }

/* 分配对话框：小组两列勾选 */
.assign-grid { display: grid; grid-template-columns: 1fr 1fr; }
.assign-item { margin-bottom: 6px; }
</style>
