<template>
  <!-- ============================================================================
       【功能】班级与分组管理页（教师端核心）
       ----------------------------------------------------------------------------
       【说明】左侧班级列表（新建/编辑/删除）；右侧分组看板（A/B/C…组卡片
              含成员表）。支持：Excel名单导入（自动建号+按学号末2位自动分组）、
              自动重新分组、手动调组、设置截止时间。
       ============================================================================ -->
  <div class="page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">班级与分组</h2>
        <p class="page-sub">创建指导班级，导入学生名单后系统自动建号并分组</p>
      </div>
      <el-button type="primary" @click="openClassDialog()">新建班级</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：班级列表 -->
      <el-col :span="8">
        <el-card shadow="never" class="class-list-card" v-loading="loading">
          <template #header><span class="card-title">指导班级（{{ classList.length }}）</span></template>
          <div v-for="c in classList" :key="c.id" class="class-item" :class="{ active: currentClassId === c.id }" @click="selectClass(c)">
            <div class="ci-row1">
              <span class="ci-name">{{ c.className }}</span>
              <span class="ci-count">{{ c.studentCount }} 名学生</span>
            </div>
            <div class="ci-row2">
              <el-tag size="small" type="info">{{ c.groupCount }} 组</el-tag>
              <el-tag size="small" :type="deadlineTagType(c.deadline)">{{ deadlineText(c.deadline) }}</el-tag>
              <span class="ci-ops">
                <el-button link type="primary" size="small" @click.stop="openClassDialog(c)">编辑</el-button>
                <el-button link type="danger" size="small" @click.stop="handleDeleteClass(c)">删除</el-button>
              </span>
            </div>
          </div>
          <el-empty v-if="classList.length === 0" description="暂无班级" :image-size="70" />
        </el-card>
      </el-col>

      <!-- 右侧：分组看板 -->
      <el-col :span="16">
        <el-card shadow="never" v-loading="boardLoading">
          <template #header>
            <div class="board-head">
              <span class="card-title">分组看板{{ currentClass ? ' - ' + currentClass.className : '' }}</span>
              <span>
                <el-button type="primary" plain size="small" :disabled="!currentClassId" @click="importVisible = true">名单导入</el-button>
                <el-button type="warning" plain size="small" :disabled="!currentClassId" @click="handleAutoGroup">自动重新分组</el-button>
              </span>
            </div>
          </template>
          <!-- 组卡片流式布局：组名色条 + 成员表 -->
          <div class="group-grid">
            <div v-for="g in board" :key="g.id" class="group-card">
              <div class="g-head" :style="{ borderLeftColor: g.colorCode }">
                <span class="g-name">{{ g.groupName }}</span>
                <span class="g-count">{{ g.memberCount }} 人</span>
              </div>
              <el-table :data="g.members" size="small" max-height="220" class="g-table">
                <el-table-column prop="studentNo" label="学号" width="110" />
                <el-table-column prop="nickName" label="姓名" />
                <el-table-column label="操作" width="60">
                  <template #default="scope">
                    <el-button link type="primary" size="small" @click="openMove(scope.row)">调组</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          <el-empty v-if="board.length === 0 && currentClassId" description="暂无小组" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建/编辑班级对话框 -->
    <el-dialog v-model="classDialogVisible" :title="form.id ? '编辑班级' : '新建班级'" width="520px">
      <el-form ref="classRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如：车辆2301班" maxlength="50" />
        </el-form-item>
        <el-form-item label="分组数" prop="groupCount">
          <!-- 分组数可选 4/5/6；按学号末2位对组数取模自动分组 -->
          <el-radio-group v-model="form.groupCount">
            <el-radio-button :value="4">4组</el-radio-button>
            <el-radio-button :value="5">5组</el-radio-button>
            <el-radio-button :value="6">6组</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="课程设计任务截止提交时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitClass">确定</el-button>
      </template>
    </el-dialog>

    <!-- 名单导入对话框 -->
    <el-dialog v-model="importVisible" title="导入学生名单" width="520px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px"
        title="Excel 列：学号、姓名、联系电话（选填）。导入后系统自动创建登录账号（初始密码=学号后6位），并按学号末2位自动分组（余1→A组…余0→最后一组）。" />
      <el-upload drag ref="uploadRef" accept=".xlsx,.xls" :limit="1" :auto-upload="false" :on-exceed="() => $modal.msgWarning('一次只能导入一个文件')" :on-change="file => (uploadFile = file.raw)">
        <el-icon :size="40" color="#c0c4cc"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击选择 Excel</em></div>
      </el-upload>
      <el-checkbox v-model="updateSupport" style="margin-top: 10px">已存在学号时更新其班级与分组</el-checkbox>
      <template #footer>
        <el-button size="small" @click="downloadTemplate">下载模板</el-button>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 手动调组对话框 -->
    <el-dialog v-model="moveVisible" title="调整分组" width="400px">
      <el-form label-width="90px">
        <el-form-item label="学生">
          <span>{{ moveStudent.nickName }}（{{ moveStudent.studentNo }}）</span>
        </el-form-item>
        <el-form-item label="目标小组">
          <el-select v-model="moveGroupId" style="width: 100%">
            <el-option v-for="g in board" :key="g.id" :label="g.groupName + '（' + g.memberCount + '人）'" :value="g.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TeacherClass">
// ============================================================================
// 【功能】班级与分组逻辑：班级CRUD + 名单导入 + 分组看板 + 调组
// ============================================================================
import { listClass, addClass, updateClass, delClass, groupBoard, importStudents, importTemplate, autoGrouping, assignStudent } from '@/api/teach/portal'

const route = useRoute()
const { proxy } = getCurrentInstance()

// ---------------- 班级列表 ----------------
const classList = ref([])
const loading = ref(false)
const currentClassId = ref(null)
const currentClass = computed(() => classList.value.find(c => c.id === currentClassId.value))

// ---------------- 新建/编辑班级 ----------------
const classDialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({})
const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  groupCount: [{ required: true, message: '请选择分组数', trigger: 'change' }]
}

// ---------------- 分组看板 ----------------
const board = ref([])
const boardLoading = ref(false)

// ---------------- 名单导入 ----------------
const importVisible = ref(false)
const importLoading = ref(false)
const updateSupport = ref(true)
const uploadFile = ref(null)

// ---------------- 手动调组 ----------------
const moveVisible = ref(false)
const moveStudent = ref({})
const moveGroupId = ref(null)

/** 加载班级列表 */
async function loadClassList() {
  loading.value = true
  try {
    const res = await listClass()
    classList.value = res.data || []
    // 默认选中第一个班级；支持从工作台带 classId 进入
    const target = route.query.classId ? Number(route.query.classId) : classList.value[0]?.id
    if (target && classList.value.some(c => c.id === target)) {
      selectClass({ id: target })
    } else if (classList.value.length > 0) {
      selectClass(classList.value[0])
    } else {
      board.value = []
    }
  } finally {
    loading.value = false
  }
}

/** 选中班级并刷新分组看板 */
function selectClass(c) {
  currentClassId.value = c.id
  loadBoard()
}

/** 加载分组看板 */
async function loadBoard() {
  if (!currentClassId.value) return
  boardLoading.value = true
  try {
    const res = await groupBoard(currentClassId.value)
    board.value = res.data || []
  } finally {
    boardLoading.value = false
  }
}

/** 打开新建/编辑对话框 */
function openClassDialog(c) {
  form.value = c ? { ...c } : { className: '', groupCount: 4, deadline: null, remark: '' }
  classDialogVisible.value = true
}

/** 提交班级（新建或编辑） */
function submitClass() {
  proxy.$refs.classRef.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (form.value.id) {
        await updateClass(form.value)
        proxy.$modal.msgSuccess('修改成功')
      } else {
        await addClass(form.value)
        proxy.$modal.msgSuccess('创建成功，已自动生成 ' + form.value.groupCount + ' 个小组')
      }
      classDialogVisible.value = false
      await loadClassList()
    } finally {
      submitLoading.value = false
    }
  })
}

/** 删除班级（有学生时后端拒绝） */
function handleDeleteClass(c) {
  proxy.$modal.confirm('确定删除班级「' + c.className + '」吗？班内配置数据将一并清理。').then(async () => {
    await delClass(c.id)
    proxy.$modal.msgSuccess('删除成功')
    loadClassList()
  }).catch(() => {})
}

/** 提交名单导入 */
function submitImport() {
  if (!uploadFile.value) {
    proxy.$modal.msgWarning('请先选择 Excel 文件')
    return
  }
  importLoading.value = true
  const fd = new FormData()
  fd.append('updateSupport', updateSupport.value)
  fd.append('file', uploadFile.value)
  importStudents(currentClassId.value, fd).then(res => {
    // 后端返回逐行导入结果（HTML），弹窗展示
    ElMessageBox.alert(res.msg, '导入结果', { dangerouslyUseHTMLString: true })
    importVisible.value = false
    uploadFile.value = null
    loadClassList()
    loadBoard()
  }).finally(() => {
    importLoading.value = false
  })
}

/** 下载导入模板 */
async function downloadTemplate() {
  const res = await importTemplate()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '学生名单导入模板.xlsx'
  link.click()
  URL.revokeObjectURL(link.href)
}

/** 自动重新分组 */
function handleAutoGroup() {
  proxy.$modal.confirm('将按"学号末2位对组数取模"重新分配本班全部学生的组别，确定继续吗？').then(async () => {
    const res = await autoGrouping(currentClassId.value)
    proxy.$modal.msgSuccess(res.msg)
    loadBoard()
  }).catch(() => {})
}

/** 打开手动调组对话框 */
function openMove(student) {
  moveStudent.value = student
  moveGroupId.value = student.groupId
  moveVisible.value = true
}

/** 提交手动调组 */
async function submitMove() {
  await assignStudent({ userId: moveStudent.value.userId, groupId: moveGroupId.value })
  proxy.$modal.msgSuccess('调整成功')
  moveVisible.value = false
  loadBoard()
}

/** 截止时间提示 */
function deadlineText(deadline) {
  if (!deadline) return '未设截止'
  const d = Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
  return d < 0 ? '已截止' : '剩' + d + '天'
}
function deadlineTagType(deadline) {
  const d = deadline ? Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000) : 999
  return d < 0 ? 'info' : d < 7 ? 'danger' : 'success'
}

loadClassList()
</script>

<style lang="scss" scoped>
/* ==================== 班级与分组页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.card-title { font-weight: 600; color: #1d2b3a; }

/* 班级列表项 */
.class-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 8px;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; border-color: #b3d8ff; }
  .ci-row1 { display: flex; justify-content: space-between; margin-bottom: 8px; }
  .ci-name { font-weight: 600; color: #1d2b3a; }
  .ci-count { color: #86909c; font-size: 12px; }
  .ci-row2 { display: flex; align-items: center; gap: 8px; }
  .ci-ops { margin-left: auto; }
}

/* 分组看板：组卡片两列布局 */
.group-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.group-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  .g-head {
    display: flex;
    justify-content: space-between;
    padding: 8px 12px;
    background: #fafbfc;
    border-left: 4px solid #409eff;
  }
  .g-name { font-weight: 600; color: #1d2b3a; }
  .g-count { color: #86909c; font-size: 12px; }
  .g-table { width: 100%; }
}
.board-head { display: flex; justify-content: space-between; align-items: center; }
</style>
