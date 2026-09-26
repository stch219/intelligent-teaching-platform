<template>
  <!-- ============================================================================
       【功能】资料发布页（教师按班级发布学习资料）
       ----------------------------------------------------------------------------
       【说明】资料四类：任务指导书/说明书模板/参考答案/辅助资料。
              文件经若依 /common/upload 落盘；参考答案严格保密
              （学生端永不可见，可见性开关禁用）。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 + 上传 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">资料发布</h2>
        <p class="page-sub">发布任务指导书、说明书模板等学习资料，供对应班级学生查看下载</p>
      </div>
      <el-space>
        <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadList">
          <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!classId" @click="uploadVisible = true">上传资料</el-button>
      </el-space>
    </div>

    <!-- 资料列表 -->
    <el-card shadow="never" v-loading="loading">
      <el-table :data="materialList" v-if="classId">
        <el-table-column label="类型" width="120" align="center">
          <template #default="scope">
            <el-tag :type="typeTag(scope.row.materialType)" effect="plain">{{ typeName(scope.row.materialType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="资料名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="大小" width="100" align="center">
          <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="学生可见" width="100" align="center">
          <template #default="scope">
            <!-- 参考答案（类型3）强制不可见且禁用开关 -->
            <el-switch v-model="scope.row.visible" :active-value="1" :inactive-value="0"
              :disabled="scope.row.materialType === 3" @change="toggleVisible(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column prop="uploadBy" label="上传人" width="110" />
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="download(scope.row)">下载</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="请先选择班级" :image-size="90" />
    </el-card>

    <!-- 上传资料对话框 -->
    <el-dialog v-model="uploadVisible" title="上传资料" width="520px">
      <el-form ref="materialRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="资料类型" prop="materialType">
          <el-select v-model="form.materialType" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="资料名称" prop="fileName">
          <el-input v-model="form.fileName" placeholder="资料显示名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="选择文件" prop="filePath">
          <el-upload drag ref="uploadRef" :limit="1" :action="uploadUrl" :headers="headers" :on-success="onUploadSuccess" :on-error="() => $modal.msgError('文件上传失败')">
            <el-icon :size="36" color="#c0c4cc"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="学生可见" v-if="form.materialType !== 3">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <!-- 参考答案保密提示 -->
        <el-alert v-if="form.materialType === 3" type="warning" :closable="false" show-icon
          title="参考答案严格保密：仅作为 AI 批改依据，学生端永不可见。" />
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="!form.filePath" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TeacherMaterial">
// ============================================================================
// 【功能】资料发布逻辑：上传（走若依common/upload）+ 元数据保存 + 可见性控制
// ============================================================================
import { listClass, listMaterial, addMaterial, updateMaterial, delMaterial } from '@/api/teach/portal'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance()

const classList = ref([])
const classId = ref(null)
const materialList = ref([])
const loading = ref(false)

// 上传配置：走若依通用上传接口，携带登录token
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/upload'
const headers = ref({ Authorization: 'Bearer ' + getToken() })

// 类型配置
const typeOptions = [
  { value: 1, label: '任务指导书' },
  { value: 2, label: '说明书模板' },
  { value: 3, label: '参考答案（保密）' },
  { value: 4, label: '辅助资料' }
]

// 上传对话框
const uploadVisible = ref(false)
const submitLoading = ref(false)
const form = ref({})
const rules = {
  materialType: [{ required: true, message: '请选择资料类型', trigger: 'change' }],
  fileName: [{ required: true, message: '请输入资料名称', trigger: 'blur' }],
  filePath: [{ required: true, message: '请上传文件', trigger: 'change' }]
}

/** 加载班级下拉 */
async function loadClassOptions() {
  const res = await listClass()
  classList.value = res.data || []
  if (!classId.value && classList.value.length > 0) {
    classId.value = classList.value[0].id
  }
  if (classId.value) loadList()
}

/** 加载资料列表 */
async function loadList() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await listMaterial({ classId: classId.value })
    materialList.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 上传成功回调：记录文件路径与大小 */
function onUploadSuccess(res) {
  if (res.code !== 200) {
    proxy.$modal.msgError(res.msg || '上传失败')
    return
  }
  form.value.filePath = res.fileName
  proxy.$modal.msgSuccess('文件上传成功')
}

/** 保存资料元数据 */
function submit() {
  proxy.$refs.materialRef.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await addMaterial({ ...form.value, classId: classId.value })
      proxy.$modal.msgSuccess('发布成功')
      uploadVisible.value = false
      form.value = { materialType: 1, fileName: '', filePath: '', visible: 1 }
      loadList()
    } finally {
      submitLoading.value = false
    }
  })
}

/** 切换学生可见性 */
async function toggleVisible(row) {
  await updateMaterial({ id: row.id, visible: row.visible })
  proxy.$modal.msgSuccess(row.visible === 1 ? '已对学生可见' : '已对学生隐藏')
}

/** 下载资料（通过若依通用下载接口） */
function download(row) {
  proxy.download(row.filePath, {}, row.fileName)
}

/** 删除资料 */
function handleDelete(row) {
  proxy.$modal.confirm('确定删除资料「' + row.fileName + '」吗？').then(async () => {
    await delMaterial(row.id)
    proxy.$modal.msgSuccess('删除成功')
    loadList()
  }).catch(() => {})
}

/** 文件大小格式化 */
function formatSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1048576) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1048576).toFixed(1) + ' MB'
}

/** 类型名与标签色 */
function typeName(t) {
  return (typeOptions.find(o => o.value === t) || {}).label || '-'
}
function typeTag(t) {
  return { 1: 'primary', 2: 'success', 3: 'danger', 4: 'warning' }[t] || 'info'
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== 资料发布页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }
</style>
