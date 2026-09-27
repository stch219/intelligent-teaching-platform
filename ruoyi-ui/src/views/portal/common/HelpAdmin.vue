<template>
  <!-- ============================================================================
       【功能】帮助中心管理（管理员，阶段8）
       ----------------------------------------------------------------------------
       【说明】维护帮助文档条目：新增/修改/删除/上下架；
              分类为固定下拉（快速上手/学生端指南/教师端指南/常见问题），
              内容使用 Quill 富文本编辑器（与模块编辑同源组件）。
       ============================================================================ -->
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">帮助中心管理</h2>
      <p class="page-sub">维护师生可见的帮助文档与常见问题</p>
    </div>

    <!-- 工具条：筛选 + 新增 -->
    <el-card shadow="never" style="margin-bottom: 14px">
      <el-form :inline="true">
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部分类" style="width: 160px">
            <el-option v-for="c in cateOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.title" clearable placeholder="标题关键词" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openForm()">新增条目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 条目列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading">
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" prop="category" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.category }}</el-tag></template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '已发布' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="160" />
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-button size="small" :type="row.status === '0' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === '0' ? '下架' : '发布' }}
            </el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑条目' : '新增条目'" width="720px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" placeholder="条目标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" style="width: 200px">
            <el-option v-for="c in cateOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="tip-text">（同分类内越小越靠前）</span>
        </el-form-item>
        <el-form-item label="内容">
          <!-- Quill 富文本编辑器（全局注册的 editor 组件） -->
          <editor v-model="form.content" :min-height="220" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="HelpAdmin">
// ============================================================================
// 【功能】帮助中心管理逻辑：CRUD + 上下架切换
// ============================================================================
import { listHelp, addHelp, updateHelp, delHelp } from '@/api/teach/dashboard'

// 分类固定选项（与种子数据保持一致）
const cateOptions = ['快速上手', '学生端指南', '教师端指南', '常见问题']

// 列表数据与查询条件
const list = ref([])
const loading = ref(false)
const query = ref({ category: '', title: '' })

// 表单对话框状态
const formVisible = ref(false)
const form = ref({})

/** 加载全量条目（管理端口径含下架） */
async function load() {
  loading.value = true
  try {
    const res = await listHelp({ category: query.value.category || undefined, title: query.value.title || undefined })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 打开表单（row 为空即新增） */
function openForm(row) {
  form.value = row
    ? { ...row }
    : { title: '', category: '常见问题', content: '', sortOrder: 0, status: '0' }
  formVisible.value = true
}

/** 保存（新增或修改） */
async function save() {
  if (!form.value.title || !form.value.category) {
    ElMessage.warning('请填写标题与分类')
    return
  }
  if (form.value.id) {
    await updateHelp(form.value)
    ElMessage.success('修改成功')
  } else {
    await addHelp(form.value)
    ElMessage.success('新增成功')
  }
  formVisible.value = false
  load()
}

/** 上下架切换（复用修改接口，仅改状态字段） */
async function toggleStatus(row) {
  await updateHelp({ id: row.id, status: row.status === '0' ? '1' : '0' })
  ElMessage.success(row.status === '0' ? '已下架' : '已发布')
  load()
}

/** 删除（二次确认） */
async function remove(row) {
  await ElMessageBox.confirm('确认删除「' + row.title + '」？', '提示', { type: 'warning' })
  await delHelp(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<style lang="scss" scoped>
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 16px; color: #86909c; font-size: 14px; }
.tip-text { margin-left: 8px; font-size: 12px; color: #86909c; }
</style>
