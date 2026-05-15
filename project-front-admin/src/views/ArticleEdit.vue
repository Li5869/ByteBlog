<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {Folder, Plus} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const isEdit = !!route.params.id

const form = ref({
  title: '',
  category: '',
  tags: '',
  content: '',
  status: 'draft',
  columnIds: []
})

const categories = ['前端开发', '后端开发', '运维', '数据库', '其他']

const myColumns = ref([
  { id: '1', title: 'Spring Boot 实战教程', status: 1 },
  { id: '2', title: 'Vue 3 深入浅出', status: 1 },
  { id: '3', title: 'PostgreSQL 高级特性', status: 0 }
])

const showCreateColumnDialog = ref(false)
const newColumnName = ref('')
const newColumnDescription = ref('')
const isCreatingColumn = ref(false)

const toggleColumn = (columnId) => {
  const index = form.value.columnIds.indexOf(columnId)
  if (index > -1) {
    form.value.columnIds.splice(index, 1)
  } else {
    if (form.value.columnIds.length >= 3) {
      ElMessage.warning('最多只能选择3个专栏')
      return
    }
    form.value.columnIds.push(columnId)
  }
}

const openCreateColumnDialog = () => {
  newColumnName.value = ''
  newColumnDescription.value = ''
  showCreateColumnDialog.value = true
}

const handleCreateColumn = async () => {
  if (!newColumnName.value.trim()) {
    ElMessage.error('请输入专栏名称')
    return
  }
  
  isCreatingColumn.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const newColumn = {
      id: Date.now().toString(),
      title: newColumnName.value,
      description: newColumnDescription.value,
      status: 0
    }
    
    myColumns.value.push(newColumn)
    form.value.columnIds.push(newColumn.id)
    showCreateColumnDialog.value = false
    ElMessage.success('专栏创建成功')
  } catch (error) {
    console.error('创建专栏失败:', error)
    ElMessage.error('创建专栏失败')
  } finally {
    isCreatingColumn.value = false
  }
}

const handleSave = (status) => {
  form.value.status = status
  router.push('/articles')
}

onMounted(() => {
  // 这里可以加载专栏列表
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ isEdit ? '编辑文章' : '新建文章' }}</h1>
      <button
        @click="router.back()"
        class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors text-sm"
      >
        返回列表
      </button>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">文章标题</label>
              <input
                v-model="form.title"
                type="text"
                placeholder="请输入文章标题"
                class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">文章内容</label>
              <textarea
                v-model="form.content"
                rows="15"
                placeholder="请输入文章内容，支持 Markdown 格式"
                class="w-full px-4 py-3 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500 resize-none"
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-4">发布设置</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">分类</label>
              <select
                v-model="form.category"
                class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 text-gray-900 dark:text-white"
              >
                <option value="">请选择分类</option>
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">标签</label>
              <input
                v-model="form.tags"
                type="text"
                placeholder="多个标签用逗号分隔"
                class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500"
              />
            </div>
            
            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300">添加到专栏</label>
                <el-button type="primary" link size="small" @click="openCreateColumnDialog">
                  <el-icon class="mr-1"><Plus /></el-icon>
                  新建专栏
                </el-button>
              </div>
              
              <div v-if="myColumns.length > 0" class="space-y-2">
                <div class="flex flex-wrap gap-2">
                  <el-tag
                    v-for="column in myColumns"
                    :key="column.id"
                    :type="form.columnIds.includes(column.id) ? 'danger' : 'info'"
                    :effect="form.columnIds.includes(column.id) ? 'dark' : 'plain'"
                    class="cursor-pointer"
                    @click="toggleColumn(column.id)"
                  >
                    <el-icon class="mr-1"><Folder /></el-icon>
                    {{ column.title }}
                    <span v-if="column.status === 0" class="ml-1 text-xs text-yellow-300">(草稿)</span>
                  </el-tag>
                </div>
                <p class="text-xs text-gray-400">
                  已选择 {{ form.columnIds.length }} 个专栏（最多3个）
                </p>
              </div>
              
              <div v-else class="text-center py-4 bg-gray-50 dark:bg-gray-700/30 rounded-lg">
                <el-icon class="text-3xl text-gray-300 mb-2"><Folder /></el-icon>
                <p class="text-sm text-gray-400 mb-2">暂无专栏</p>
                <el-button type="primary" link size="small" @click="openCreateColumnDialog">
                  创建第一个专栏
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-4">操作</h3>
          <div class="space-y-3">
            <button
              @click="handleSave('published')"
              class="w-full px-4 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium"
            >
              发布文章
            </button>
            <button
              @click="handleSave('draft')"
              class="w-full px-4 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-sm font-medium"
            >
              保存草稿
            </button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showCreateColumnDialog"
      title="新建专栏"
      width="500px"
    >
      <el-form label-width="80px">
        <el-form-item label="专栏名称" required>
          <el-input
            v-model="newColumnName"
            maxlength="100"
            placeholder="请输入专栏名称"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="专栏描述">
          <el-input
            v-model="newColumnDescription"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="请输入专栏描述（可选）"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateColumnDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleCreateColumn"
          :loading="isCreatingColumn"
          :disabled="!newColumnName.trim()"
        >
          创建专栏
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
