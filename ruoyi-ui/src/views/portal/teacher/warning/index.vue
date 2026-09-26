<template>
  <!-- ============================================================================
       【功能】进度预警规则设置页（每班最多3条，三级预警）
       ----------------------------------------------------------------------------
       【说明】规则语义：距截止提交 X 天时，小组完成率低于 Y% 触发对应
              级别预警（1一般-黄 2重要-橙 3紧急-红）；触发由阶段8定时任务执行。
       ============================================================================ -->
  <div class="page">
    <!-- 页头：班级选择 + 新增 -->
    <div class="page-head">
      <div>
        <h2 class="page-title">预警规则</h2>
        <p class="page-sub">为班级设置进度预警（最多 3 条），低于阈值的组将收到对应级别预警通知</p>
      </div>
      <el-space>
        <el-select v-model="classId" placeholder="选择班级" style="width: 200px" @change="loadList">
          <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!classId || ruleList.length >= 3" @click="openDialog()">新增规则</el-button>
      </el-space>
    </div>

    <!-- 规则卡片：三级颜色标识 -->
    <div class="rule-grid" v-if="classId">
      <div v-for="r in ruleList" :key="r.id" class="rule-card" :class="'level-' + r.level">
        <div class="r-head">
          <span class="r-level">
            <span class="dot"></span>{{ levelName(r.level) }}
          </span>
          <el-switch v-model="r.enabled" :active-value="1" :inactive-value="0" @change="toggleEnabled(r)" />
        </div>
        <div class="r-body">
          距截止 <b>{{ r.daysBefore }}</b> 天时，完成率低于 <b>{{ r.progressThreshold }}%</b> 触发
        </div>
        <div class="r-foot">
          <span class="r-time">{{ r.createTime }}</span>
          <span>
            <el-button link type="primary" size="small" @click="openDialog(r)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(r)">删除</el-button>
          </span>
        </div>
      </div>
      <!-- 空槽位提示（最多3条） -->
      <div v-for="n in (3 - ruleList.length)" :key="'slot' + n" class="rule-card empty" @click="ruleList.length < 3 && openDialog()">
        <el-icon :size="24" color="#c0c4cc"><Plus /></el-icon>
        <div class="empty-text">添加预警规则（{{ ruleList.length }}/3）</div>
      </div>
    </div>
    <el-empty v-else description="请先选择班级" :image-size="90" />

    <!-- 新增/编辑规则对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑规则' : '新增预警规则'" width="460px">
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="预警级别" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio-button :value="1">一般（黄）</el-radio-button>
            <el-radio-button :value="2">重要（橙）</el-radio-button>
            <el-radio-button :value="3">紧急（红）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="距截止天数" prop="daysBefore">
          <el-input-number v-model="form.daysBefore" :min="1" :max="90" controls-position="right" style="width: 160px" />
          <span class="unit">天</span>
        </el-form-item>
        <el-form-item label="完成率阈值" prop="progressThreshold">
          <el-input-number v-model="form.progressThreshold" :min="1" :max="99" controls-position="right" style="width: 160px" />
          <span class="unit">%</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TeacherWarning">
// ============================================================================
// 【功能】预警规则逻辑：三级规则卡片管理（最多3条）+ 启停 + 编辑
// ============================================================================
import { listClass, listWarningRule, addWarningRule, updateWarningRule, delWarningRule } from '@/api/teach/portal'

const { proxy } = getCurrentInstance()

const classList = ref([])
const classId = ref(null)
const ruleList = ref([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({})
const rules = {
  level: [{ required: true, message: '请选择预警级别', trigger: 'change' }],
  daysBefore: [{ required: true, message: '请输入距截止天数', trigger: 'blur' }],
  progressThreshold: [{ required: true, message: '请输入完成率阈值', trigger: 'blur' }]
}

/** 级别名称 */
function levelName(level) {
  return { 1: '一般', 2: '重要', 3: '紧急' }[level] || '-'
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

/** 加载规则列表 */
async function loadList() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await listWarningRule(classId.value)
    ruleList.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 打开新增/编辑对话框 */
function openDialog(r) {
  form.value = r ? { ...r } : { classId: classId.value, level: 1, daysBefore: 7, progressThreshold: 50, enabled: 1 }
  dialogVisible.value = true
}

/** 提交规则 */
function submit() {
  proxy.$refs.ruleRef.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (form.value.id) {
        await updateWarningRule(form.value)
        proxy.$modal.msgSuccess('修改成功')
      } else {
        await addWarningRule({ ...form.value, classId: classId.value })
        proxy.$modal.msgSuccess('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitLoading.value = false
    }
  })
}

/** 启停切换 */
async function toggleEnabled(r) {
  await updateWarningRule({ id: r.id, level: r.level, daysBefore: r.daysBefore, progressThreshold: r.progressThreshold, enabled: r.enabled })
  proxy.$modal.msgSuccess(r.enabled === 1 ? '已启用' : '已停用')
}

/** 删除规则 */
function handleDelete(r) {
  proxy.$modal.confirm('确定删除该预警规则吗？').then(async () => {
    await delWarningRule(r.id)
    proxy.$modal.msgSuccess('删除成功')
    loadList()
  }).catch(() => {})
}

loadClassOptions()
</script>

<style lang="scss" scoped>
/* ==================== 预警规则页样式 ==================== */
.page-title { margin: 0 0 6px; font-size: 22px; color: #1d2b3a; }
.page-sub { margin: 0 0 20px; color: #86909c; font-size: 14px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; }
.unit { margin-left: 8px; color: #86909c; }

/* 规则卡片：三级颜色顶边 */
.rule-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.rule-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  border-top: 4px solid #e6a23c;
  &.level-1 { border-top-color: #e6cf5c; }
  &.level-2 { border-top-color: #e68a3c; }
  &.level-3 { border-top-color: #f56c6c; }
  &.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-top: 1px dashed #dcdfe6;
    cursor: pointer;
    color: #86909c;
    min-height: 120px;
    &:hover { border-color: #409eff; color: #409eff; }
  }
  .r-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .r-level { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #1d2b3a; }
  .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
  &.level-1 .dot { background: #e6cf5c; }
  &.level-2 .dot { background: #e68a3c; }
  &.level-3 .dot { background: #f56c6c; }
  .r-body { color: #4e5969; font-size: 14px; line-height: 1.6; margin-bottom: 10px; b { color: #1d2b3a; } }
  .r-foot { display: flex; justify-content: space-between; align-items: center; }
  .r-time { color: #c0c4cc; font-size: 12px; }
}
</style>
