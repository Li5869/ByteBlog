<script setup>
import {computed, onMounted, ref} from 'vue'
import {knowledgeApi} from '@/utils/request'
import {formatAbsoluteDate} from '@/utils/format'

// ==================== 状态管理 ====================
const loading = ref(false)
const files = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

// 搜索筛选
const searchQuery = ref('')
const sourceFilter = ref('all')
const categoryFilter = ref('all')

// 批量操作
const selectedIds = ref([])
const isAllSelected = ref(false)

// 弹窗状态
const showUploadArea = ref(false)
const showEditModal = ref(false)
const showDetailDrawer = ref(false)
const selectedFile = ref(null)
const editForm = ref({ fileName: '', description: '' })

// 上传状态
const fileInput = ref(null)
const selectedUploadFile = ref(null)
const uploadDescription = ref('')
const uploadCategory = ref('general')
const fileDragOver = ref(false)
const uploadLoading = ref(false)
const uploadSuccess = ref(false)
const uploadMessage = ref('')

// ==================== 来源选项 ====================
const sourceOptions = [
  { label: '全部来源', value: 'all' },
  { label: '文件上传', value: 'file_upload' }
]

// ==================== 知识库分类选项 ====================
const categoryOptions = [
  { label: '项目知识库', value: 'project', description: '项目实现、系统架构、代码逻辑' },
  { label: '面试知识库', value: 'interview', description: '技术原理、底层机制、面试题' },
  { label: '通用知识库', value: 'general', description: '通用技术知识、学习资料' }
]

// ==================== 分类筛选选项 ====================
const categoryFilterOptions = [
  { label: '全部分类', value: 'all' },
  { label: '项目知识库', value: 'project' },
  { label: '面试知识库', value: 'interview' },
  { label: '通用知识库', value: 'general' }
]

// ==================== 计算属性 ====================
const hasSelected = computed(() => selectedIds.value.length > 0)

// ==================== 数据加载 ====================
const fetchFiles = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }

    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }

    if (sourceFilter.value !== 'all') {
      params.source = sourceFilter.value
    }

    if (categoryFilter.value !== 'all') {
      params.category = categoryFilter.value
    }

    const res = await knowledgeApi.getList(params)
    files.value = res.records || []
    total.value = res.total || 0
    totalPages.value = res.pages || Math.ceil(res.total / pageSize.value)

    // 清空选中状态
    selectedIds.value = []
    isAllSelected.value = false
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    alert('获取列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ==================== 搜索筛选 ====================
let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchFiles()
  }, 300)
}

const onSourceChange = (source) => {
  sourceFilter.value = source
  currentPage.value = 1
  fetchFiles()
}

const onCategoryChange = (category) => {
  categoryFilter.value = category
  currentPage.value = 1
  fetchFiles()
}

// ==================== 分页操作 ====================
const handlePageChange = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchFiles()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchFiles()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchFiles()
  }
}

// ==================== 批量操作 ====================
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
    isAllSelected.value = false
  } else {
    selectedIds.value = files.value.map(f => f.id)
    isAllSelected.value = true
  }
}

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
  isAllSelected.value = selectedIds.value.length === files.value.length
}

const isSelected = (id) => selectedIds.value.includes(id)

// ==================== 文件操作 ====================
const viewDetail = async (file) => {
  try {
    const detail = await knowledgeApi.getDetail(file.id)
    selectedFile.value = detail
    showDetailDrawer.value = true
  } catch (error) {
    console.error('获取文件详情失败:', error)
    alert('获取详情失败: ' + (error.message || '未知错误'))
  }
}

const openEditModal = (file) => {
  selectedFile.value = file
  editForm.value = {
    fileName: file.fileName,
    description: file.description || ''
  }
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedFile.value = null
}

const saveEdit = async () => {
  if (!editForm.value.fileName.trim()) {
    alert('文件名不能为空')
    return
  }

  try {
    await knowledgeApi.updateFile(selectedFile.value.id, {
      fileName: editForm.value.fileName,
      description: editForm.value.description
    })

    closeEditModal()
    fetchFiles()
    alert('更新成功')
  } catch (error) {
    console.error('更新文件信息失败:', error)
    alert('更新失败: ' + (error.message || '未知错误'))
  }
}

const deleteFile = async (file) => {
  if (!confirm(`确定要删除文件 "${file.fileName}" 吗？\n删除后将同时清除关联的 ${file.chunkCount} 个文本块。`)) {
    return
  }

  try {
    await knowledgeApi.deleteFile(file.id)
    fetchFiles()
    alert('删除成功')
  } catch (error) {
    console.error('删除文件失败:', error)
    alert('删除失败: ' + (error.message || '未知错误'))
  }
}

const batchDelete = async () => {
  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 个文件吗？\n删除后将同时清除所有关联的文本块。`)) {
    return
  }

  try {
    await knowledgeApi.batchDelete(selectedIds.value)
    selectedIds.value = []
    isAllSelected.value = false
    fetchFiles()
    alert('批量删除成功')
  } catch (error) {
    console.error('批量删除失败:', error)
    alert('批量删除失败: ' + (error.message || '未知错误'))
  }
}

const closeDetailDrawer = () => {
  showDetailDrawer.value = false
  selectedFile.value = null
}

// ==================== 文件上传 ====================
const handleFileSelect = (event) => {
  const file = event.target.files[0]
  validateAndSetFile(file)
}

const handleFileDrop = (event) => {
  event.preventDefault()
  fileDragOver.value = false
  const file = event.dataTransfer.files[0]
  validateAndSetFile(file)
}

const validateAndSetFile = (file) => {
  if (!file) return

  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()

  if (fileExtension !== '.md') {
    alert('仅支持 .md 格式的 Markdown 文件')
    return
  }

  selectedUploadFile.value = file
}

const removeSelectedFile = () => {
  selectedUploadFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const uploadFile = async () => {
  if (!selectedUploadFile.value) {
    alert('请选择文件')
    return
  }

  uploadLoading.value = true
  uploadSuccess.value = false

  try {
    const result = await knowledgeApi.uploadFile(selectedUploadFile.value, uploadDescription.value, uploadCategory.value)

    uploadSuccess.value = true
    uploadMessage.value = `文件 "${result.fileName}" 上传成功！共生成 ${result.chunkCount} 个文本块`

    // 重置上传表单
    selectedUploadFile.value = null
    uploadDescription.value = ''
    uploadCategory.value = 'general'
    if (fileInput.value) {
      fileInput.value.value = ''
    }

    // 刷新列表
    fetchFiles()
  } catch (error) {
    console.error('上传文件失败:', error)
    alert('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}

// ==================== 工具函数 ====================
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getSourceText = (source) => {
  const map = { file_upload: '文件上传' }
  return map[source] || source
}

const getSourceClass = (source) => {
  const map = {
    file_upload: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400'
  }
  return map[source] || ''
}

const getCategoryText = (category) => {
  const map = { 
    project: '项目', 
    interview: '面试', 
    general: '通用' 
  }
  return map[category] || category
}

const getCategoryClass = (category) => {
  const map = {
    project: 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400',
    interview: 'bg-cyan-100 text-cyan-600 dark:bg-cyan-900/30 dark:text-cyan-400',
    general: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
  }
  return map[category] || ''
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchFiles()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-7xl mx-auto space-y-6">

      <!-- ==================== 标题栏 ==================== -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">知识库管理</h1>
        <button
          @click="showUploadArea = !showUploadArea"
          class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ showUploadArea ? '收起上传' : '上传文件' }}
        </button>
      </div>

      <!-- ==================== 上传文件区域 ==================== -->
      <transition
        enter-active-class="transition-all duration-300 ease-out"
        leave-active-class="transition-all duration-200 ease-in"
        enter-from-class="opacity-0 -translate-y-4"
        enter-to-class="opacity-100 translate-y-0"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-4"
      >
        <div v-if="showUploadArea" class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">上传文件到知识库</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">支持 .md 格式的 Markdown 文件，上传后自动切片并向量化存储</p>
          </div>

          <div class="p-6 space-y-4">
            <!-- 上传成功提示 -->
            <div v-if="uploadSuccess" class="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
              <div class="flex items-center gap-2">
                <svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <p class="text-sm text-green-800 dark:text-green-200">{{ uploadMessage }}</p>
              </div>
            </div>

            <!-- 文件拖拽区域 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                选择文件 <span class="text-red-500">*</span>
              </label>
              <div
                @dragover.prevent="fileDragOver = true"
                @dragleave.prevent="fileDragOver = false"
                @drop="handleFileDrop"
                @click="$refs.fileInput.click()"
                :class="[
                  'border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors',
                  fileDragOver
                    ? 'border-red-500 bg-red-50 dark:bg-red-900/20'
                    : 'border-gray-300 dark:border-gray-600 hover:border-red-400 dark:hover:border-red-500'
                ]"
              >
                <input
                  ref="fileInput"
                  type="file"
                  accept=".md"
                  @change="handleFileSelect"
                  class="hidden"
                />
                <svg class="w-12 h-12 mx-auto text-gray-400 dark:text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
                <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">点击或拖拽文件到此处上传</p>
                <p class="text-xs text-gray-500 dark:text-gray-500">仅支持 .md 格式的 Markdown 文件</p>
              </div>
            </div>

            <!-- 已选文件信息 -->
            <div v-if="selectedUploadFile" class="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <div>
                    <p class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedUploadFile.name }}</p>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ formatFileSize(selectedUploadFile.size) }}</p>
                  </div>
                </div>
                <button
                  @click="removeSelectedFile"
                  class="p-1.5 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- 文件描述 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                文件描述（可选）
              </label>
              <input
                v-model="uploadDescription"
                type="text"
                placeholder="输入文件描述，便于后续管理"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent text-sm"
              />
            </div>

            <!-- 知识库分类 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                知识库分类
              </label>
              <div class="grid grid-cols-3 gap-3">
                <button
                  v-for="option in categoryOptions"
                  :key="option.value"
                  @click="uploadCategory = option.value"
                  :class="[
                    'px-4 py-3 rounded-lg border-2 text-left transition-all',
                    uploadCategory === option.value
                      ? 'border-red-500 bg-red-50 dark:bg-red-900/20'
                      : 'border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500'
                  ]"
                >
                  <p class="text-sm font-medium" :class="uploadCategory === option.value ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'">
                    {{ option.label }}
                  </p>
                  <p class="text-xs mt-0.5" :class="uploadCategory === option.value ? 'text-red-500 dark:text-red-400' : 'text-gray-500 dark:text-gray-400'">
                    {{ option.description }}
                  </p>
                </button>
              </div>
            </div>

            <!-- 上传按钮 -->
            <div class="flex justify-end">
              <button
                @click="uploadFile"
                :disabled="uploadLoading || !selectedUploadFile"
                class="px-6 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm font-medium flex items-center gap-2"
              >
                <svg v-if="uploadLoading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ uploadLoading ? '上传中...' : '上传到知识库' }}
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- ==================== 主内容卡片 ==================== -->
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">

        <!-- 搜索筛选区域 -->
        <div class="p-4 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row gap-4">
            <div class="flex-1">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  @input="onSearch"
                  type="text"
                  placeholder="搜索文件名..."
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent text-sm"
                />
              </div>
            </div>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="option in categoryFilterOptions"
                :key="option.value"
                @click="onCategoryChange(option.value)"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="categoryFilter === option.value
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="option in sourceOptions"
                :key="option.value"
                @click="onSourceChange(option.value)"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="sourceFilter === option.value
                  ? 'bg-red-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>

        <!-- 批量操作栏 -->
        <div v-if="hasSelected" class="px-4 py-3 bg-red-50 dark:bg-red-900/20 border-b border-red-200 dark:border-red-800 flex items-center justify-between">
          <span class="text-sm text-red-700 dark:text-red-300">
            已选中 {{ selectedIds.length }} 个文件
          </span>
          <button
            @click="batchDelete"
            class="px-4 py-1.5 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium"
          >
            批量删除
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin"></div>
          <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
        </div>

        <!-- 数据表格 -->
        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left">
                  <input
                    type="checkbox"
                    :checked="isAllSelected"
                    @change="toggleSelectAll"
                    class="w-4 h-4 text-red-500 border-gray-300 dark:border-gray-600 rounded focus:ring-red-500"
                  />
                </th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">文件名</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">大小</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">Chunk数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">分类</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">来源</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">上传时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="file in files"
                :key="file.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4 whitespace-nowrap">
                  <input
                    type="checkbox"
                    :checked="isSelected(file.id)"
                    @change="toggleSelect(file.id)"
                    class="w-4 h-4 text-red-500 border-gray-300 dark:border-gray-600 rounded focus:ring-red-500"
                  />
                </td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400">
                      MD
                    </div>
                    <div class="min-w-0">
                      <p class="text-sm font-medium text-gray-900 dark:text-white truncate max-w-xs">{{ file.fileName }}</p>
                      <p v-if="file.description" class="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">{{ file.description }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden sm:table-cell">
                  {{ formatFileSize(file.fileSize) }}
                </td>
                <td class="px-4 py-4 whitespace-nowrap hidden sm:table-cell">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200">
                    {{ file.chunkCount }} 个
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getCategoryClass(file.category)"
                  >
                    {{ getCategoryText(file.category) }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getSourceClass(file.source)"
                  >
                    {{ getSourceText(file.source) }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">
                  {{ formatAbsoluteDate(file.createdAt) }}
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm">
                  <div class="flex items-center gap-2">
                    <button
                      @click="viewDetail(file)"
                      class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
                      title="查看详情"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button
                      @click="openEditModal(file)"
                      class="text-blue-500 hover:text-blue-600 transition-colors"
                      title="编辑"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <a
                      v-if="file.fileUrl"
                      :href="file.fileUrl"
                      target="_blank"
                      class="text-green-500 hover:text-green-600 transition-colors"
                      title="下载源文件"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                      </svg>
                    </a>
                    <button
                      @click="deleteFile(file)"
                      class="text-red-500 hover:text-red-600 transition-colors"
                      title="删除"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && files.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无知识库文件</p>
          <button
            @click="showUploadArea = true"
            class="mt-2 text-red-500 hover:text-red-600 text-sm font-medium"
          >
            上传第一个文件
          </button>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-600 dark:text-gray-400">
              共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="prevPage"
                :disabled="currentPage === 1"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                上一页
              </button>
              <div class="flex items-center gap-1">
                <button
                  v-for="page in Math.min(totalPages, 7)"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 text-sm rounded-lg transition-colors"
                  :class="currentPage === page
                    ? 'bg-red-500 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
                >
                  {{ page }}
                </button>
              </div>
              <button
                @click="nextPage"
                :disabled="currentPage === totalPages"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 使用说明 ==================== -->
      <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
        <div class="flex gap-3">
          <svg class="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="text-sm text-blue-800 dark:text-blue-200">
            <p class="font-medium mb-1">使用说明</p>
            <ul class="list-disc list-inside space-y-1 text-blue-700 dark:text-blue-300">
              <li>仅支持上传 .md 格式的 Markdown 文件</li>
              <li>上传的文件会自动按标题结构进行智能切片和向量化处理</li>
              <li>上传成功后，内容将可用于 RAG 问答</li>
              <li>建议使用有意义的文件名，方便后续管理</li>
              <li>删除文件将同时清除所有关联的文本块</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 编辑弹窗 ==================== -->
    <Teleport to="body">
      <transition
        enter-active-class="transition-all duration-200"
        leave-active-class="transition-all duration-150"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <!-- 遮罩层 -->
          <div class="absolute inset-0 bg-black/50" @click="closeEditModal"></div>

          <!-- 弹窗内容 -->
          <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-md w-full">
            <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">编辑文件信息</h3>
            </div>

            <div class="p-6 space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  文件名 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="editForm.fileName"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent text-sm"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  文件描述
                </label>
                <textarea
                  v-model="editForm.description"
                  rows="3"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent text-sm resize-none"
                ></textarea>
              </div>
            </div>

            <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
              <button
                @click="closeEditModal"
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
              >
                取消
              </button>
              <button
                @click="saveEdit"
                class="px-4 py-2 text-sm font-medium text-white bg-red-500 rounded-lg hover:bg-red-600 transition-colors"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- ==================== 详情抽屉 ==================== -->
    <Teleport to="body">
      <transition
        enter-active-class="transition-all duration-300"
        leave-active-class="transition-all duration-200"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showDetailDrawer" class="fixed inset-0 z-50 flex justify-end">
          <!-- 遮罩层 -->
          <div class="absolute inset-0 bg-black/50" @click="closeDetailDrawer"></div>

          <!-- 抽屉内容 -->
          <div class="relative w-full max-w-lg bg-white dark:bg-gray-800 shadow-xl overflow-y-auto">
            <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">文件详情</h3>
              <button
                @click="closeDetailDrawer"
                class="p-1.5 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div v-if="selectedFile" class="p-6 space-y-6">
              <!-- 文件基本信息 -->
              <div class="flex items-center gap-4">
                <div class="w-16 h-16 rounded-xl flex items-center justify-center text-2xl font-bold bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400">
                  MD
                </div>
                <div>
                  <h4 class="text-lg font-medium text-gray-900 dark:text-white">{{ selectedFile.fileName }}</h4>
                  <p v-if="selectedFile.description" class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ selectedFile.description }}</p>
                </div>
              </div>

              <!-- 详细信息列表 -->
              <div class="space-y-4">
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">文件ID</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedFile.id }}</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">原始文件名</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedFile.originalName }}</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">文件大小</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ formatFileSize(selectedFile.fileSize) }}</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">Chunk 数量</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedFile.chunkCount }} 个</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">知识库分类</span>
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getCategoryClass(selectedFile.category)"
                  >
                    {{ getCategoryText(selectedFile.category) }}
                  </span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">来源</span>
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getSourceClass(selectedFile.source)"
                  >
                    {{ getSourceText(selectedFile.source) }}
                  </span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">上传者</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedFile.uploaderName || '-' }}</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">上传时间</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ formatAbsoluteDate(selectedFile.createdAt) }}</span>
                </div>
                <div class="flex justify-between py-3 border-b border-gray-100 dark:border-gray-700">
                  <span class="text-sm text-gray-500 dark:text-gray-400">更新时间</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ formatAbsoluteDate(selectedFile.updatedAt) }}</span>
                </div>
                <div v-if="selectedFile.parentIds && selectedFile.parentIds.length > 0" class="py-3">
                  <span class="text-sm text-gray-500 dark:text-gray-400 block mb-2">Parent Chunk IDs</span>
                  <div class="flex flex-wrap gap-2">
                    <span
                      v-for="(pid, index) in selectedFile.parentIds"
                      :key="index"
                      class="inline-flex px-2 py-1 text-xs font-mono bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded"
                    >
                      {{ pid.substring(0, 8) }}...
                    </span>
                  </div>
                </div>
              </div>

              <!-- 源文件链接 -->
              <div v-if="selectedFile.fileUrl">
                <a
                  :href="selectedFile.fileUrl"
                  target="_blank"
                  class="flex items-center justify-center gap-2 w-full px-4 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-sm font-medium"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                  </svg>
                  下载源文件
                </a>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>
