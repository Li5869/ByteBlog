<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {NInput, NModal, NSwitch} from 'naive-ui'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'
import {columnApi, uploadApi} from '@/utils/request'
import {formatAbsoluteDate, formatNumber} from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const columns = ref([])
const showCreateModal = ref(false)
const editingColumn = ref(null)
const submitting = ref(false)
const uploading = ref(false)
const isDragging = ref(false)

const formData = ref({
  title: '',
  description: '',
  cover: '',
  status: 0
})

const fetchMyColumns = async () => {
  loading.value = true
  try {
    columns.value = await columnApi.getMyColumns()
  } catch (error) {
    console.error('获取我的专栏失败:', error)
    toast.error('获取专栏列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  editingColumn.value = null
  formData.value = {
    title: '',
    description: '',
    cover: '',
    status: 0
  }
  showCreateModal.value = true
}

const openEditModal = (column) => {
  editingColumn.value = column
  formData.value = {
    title: column.title,
    description: column.description,
    cover: column.cover,
    status: column.status
  }
  showCreateModal.value = true
}

const handleSubmit = async () => {
  if (!formData.value.title.trim()) {
    toast.error('请输入专栏标题')
    return
  }

  submitting.value = true
  try {
    if (editingColumn.value) {
      await columnApi.updateColumn(editingColumn.value.id, formData.value)
      toast.success('更新成功')
    } else {
      await columnApi.createColumn(formData.value)
      toast.success('创建成功')
    }
    
    showCreateModal.value = false
    fetchMyColumns()
  } catch (error) {
    console.error('保存专栏失败:', error)
    toast.error(error.message || '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (columnId) => {
  const confirmed = await modal.confirm('确定要删除这个专栏吗？删除后专栏内的文章关联将被移除，文章本身不会被删除。', {
    title: '删除专栏',
    confirmText: '删除',
    icon: 'error'
  })
  
  if (!confirmed) return
  
  try {
    await columnApi.deleteColumn(columnId)
    toast.success('删除成功')
    fetchMyColumns()
  } catch (error) {
    console.error('删除专栏失败:', error)
    toast.error(error.message || '删除失败，请稍后重试')
  }
}

const handleFileSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  await uploadCover(file)
}

const handleDrop = async (event) => {
  event.preventDefault()
  isDragging.value = false
  
  const file = event.dataTransfer.files[0]
  if (!file) return
  
  await uploadCover(file)
}

const handleDragOver = (event) => {
  event.preventDefault()
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const uploadCover = async (file) => {
  if (!file.type.startsWith('image/')) {
    toast.error('请上传图片文件')
    return
  }
  
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过5MB')
    return
  }
  
  uploading.value = true
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    formData.value.cover = e.target.result
    
    try {
      const url = await uploadApi.uploadFile(file)
      formData.value.cover = url
      toast.success('封面上传成功')
    } catch (error) {
      console.error('上传封面失败:', error)
      toast.error('封面上传失败，请重试')
      formData.value.cover = ''
    } finally {
      uploading.value = false
    }
  }
  reader.readAsDataURL(file)
}

const removeCover = () => {
  formData.value.cover = ''
}

const goToColumn = (columnId) => {
  router.push({ name: 'ColumnDetail', params: { id: columnId } })
}

const manageArticles = (columnId, event) => {
  event.stopPropagation()
  router.push({ name: 'ColumnArticleManage', params: { id: columnId } })
}


const getStatusText = (status) => {
  return status === 1 ? '已发布' : '草稿'
}

const getStatusClass = (status) => {
  return status === 1
    ? 'bg-gradient-to-r from-green-400 to-emerald-400 text-white shadow-lg shadow-green-500/30'
    : 'bg-gradient-to-r from-yellow-400 to-amber-400 text-white shadow-lg shadow-yellow-500/30'
}

onMounted(() => {
  fetchMyColumns()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="bg-gradient-to-r from-purple-500 via-pink-500 to-rose-500 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h1 class="text-2xl sm:text-3xl font-bold text-white mb-2">我的专栏</h1>
            <p class="text-white/80 text-sm sm:text-base">管理你的专栏，整理你的知识体系</p>
          </div>
          <button
            @click="openCreateModal"
            class="px-5 py-2.5 bg-white text-purple-600 rounded-xl text-sm font-semibold hover:bg-white/90 transition-all shadow-lg hover:shadow-xl hover:-translate-y-0.5 flex items-center gap-2"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            创建专栏
          </button>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="i in 3" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm border border-gray-100 dark:border-gray-700">
          <div class="h-32 bg-gray-200 dark:bg-gray-700 animate-pulse"></div>
          <div class="p-5 space-y-3">
            <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
          </div>
        </div>
      </div>

      <div v-else-if="columns.length === 0" class="text-center py-20">
        <div class="w-24 h-24 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
          <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <p class="text-gray-500 dark:text-gray-400 text-lg font-medium mb-2">暂无专栏</p>
        <p class="text-sm text-gray-400 dark:text-gray-500 mb-6">创建你的第一个专栏，开始整理你的知识体系</p>
        <button
          @click="openCreateModal"
          class="px-6 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl text-sm font-semibold hover:from-purple-600 hover:to-pink-600 transition-all shadow-lg"
        >
          创建专栏
        </button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="column in columns"
          :key="column.id"
          @click="goToColumn(column.id)"
          class="group bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm border border-gray-100 dark:border-gray-700 hover:shadow-xl hover:border-purple-200 dark:hover:border-purple-800 transition-all duration-300 cursor-pointer"
        >
          <div class="relative h-32 overflow-hidden">
            <img
              v-if="column.cover"
              :src="column.cover"
              :alt="column.title"
              class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
            />
            <div v-else class="w-full h-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
              <svg class="w-16 h-16 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>
            <div class="absolute top-3 right-3">
              <span
                class="px-2.5 py-1 text-xs font-semibold rounded-lg"
                :class="getStatusClass(column.status)"
              >
                {{ getStatusText(column.status) }}
              </span>
            </div>
            <div class="absolute bottom-3 left-3 right-3">
              <h3 class="text-lg font-bold text-white line-clamp-1">{{ column.title }}</h3>
            </div>
          </div>

          <div class="p-5">
            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-4 min-h-[40px]">
              {{ column.description || '暂无描述' }}
            </p>

            <div class="flex items-center justify-between text-xs text-gray-400 dark:text-gray-500 mb-4">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {{ column.articlesCount }}篇
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ formatNumber(column.views) }}
              </span>
              <span>{{ formatAbsoluteDate(column.createdAt) }}</span>
            </div>

            <div class="flex items-center gap-2">
              <button
                @click.stop="openEditModal(column)"
                class="flex-1 px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-purple-500 hover:bg-purple-50 dark:hover:bg-purple-900/20 rounded-lg transition-colors font-medium"
              >
                编辑
              </button>
              <button
                @click="manageArticles(column.id, $event)"
                class="flex-1 px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors font-medium"
              >
                管理文章
              </button>
              <button
                @click.stop="handleDelete(column.id)"
                class="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors font-medium"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <n-modal
      v-model:show="showCreateModal"
      preset="dialog"
      :title="editingColumn ? '编辑专栏' : '创建专栏'"
      :positive-text="editingColumn ? '更新' : '创建'"
      negative-text="取消"
      :loading="submitting"
      @positive-click="handleSubmit"
    >
      <div class="space-y-4 py-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            专栏标题 <span class="text-red-500">*</span>
          </label>
          <n-input
            v-model:value="formData.title"
            placeholder="请输入专栏标题"
            maxlength="100"
            show-count
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            专栏描述
          </label>
          <n-input
            v-model:value="formData.description"
            type="textarea"
            placeholder="请输入专栏描述"
            :rows="3"
            maxlength="500"
            show-count
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            封面图片
          </label>
          
          <div
            v-if="!formData.cover"
            class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-dashed rounded-xl transition-colors cursor-pointer"
            :class="isDragging ? 'border-purple-500 bg-purple-50 dark:bg-purple-900/20' : 'border-gray-300 dark:border-gray-600 hover:border-purple-400'"
            @click="$refs.fileInput.click()"
            @drop="handleDrop"
            @dragover="handleDragOver"
            @dragleave="handleDragLeave"
          >
            <div class="space-y-2 text-center">
              <svg v-if="uploading" class="mx-auto h-12 w-12 text-purple-500 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <svg v-else class="mx-auto h-12 w-12 text-gray-400" stroke="currentColor" fill="none" viewBox="0 0 48 48">
                <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <div class="flex text-sm text-gray-600 dark:text-gray-400">
                <span class="text-purple-500 hover:text-purple-600 font-medium">点击上传</span>
                <span class="pl-1">或拖拽图片到此处</span>
              </div>
              <p class="text-xs text-gray-400">支持 JPG、PNG、GIF，最大 5MB</p>
            </div>
          </div>
          
          <div v-else class="mt-1 relative group">
            <img
              :src="formData.cover"
              alt="封面预览"
              class="h-32 w-full object-cover rounded-xl"
            />
            <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl flex items-center justify-center">
              <button
                type="button"
                @click="removeCover"
                class="px-3 py-2 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600 transition-colors"
              >
                移除封面
              </button>
            </div>
          </div>
          
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="handleFileSelect"
          />
        </div>

        <div class="flex items-center justify-between">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
              发布状态
            </label>
            <p class="text-xs text-gray-400 mt-0.5">
              {{ formData.status === 1 ? '所有人可见' : '仅自己可见' }}
            </p>
          </div>
          <n-switch
            v-model:value="formData.status"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>
              发布
            </template>
            <template #unchecked>
              草稿
            </template>
          </n-switch>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
