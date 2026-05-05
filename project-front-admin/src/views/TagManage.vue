<script setup>
import {onMounted, ref} from 'vue'
import {adminTagApi} from '@/utils/request'

const tags = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)
const sortField = ref('created_at')
const sortOrder = ref('desc')

const showModal = ref(false)
const editingTag = ref(null)
const form = ref({
  name: ''
})
const formErrors = ref({
  name: ''
})

const sortOptions = [
  { label: '按时间排序', value: 'created_at' },
  { label: '按使用次数排序', value: 'use_count' }
]

const fetchTags = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      sortField: sortField.value,
      sortOrder: sortOrder.value
    }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    const res = await adminTagApi.getList(params)
    tags.value = res.records || []
    total.value = res.total || 0
    totalPages.value = res.pages || 0
  } catch (error) {
    console.error('获取标签列表失败:', error)
  } finally {
    loading.value = false
  }
}

let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchTags()
  }, 300)
}

const onSortChange = (field) => {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortField.value = field
    sortOrder.value = 'desc'
  }
  currentPage.value = 1
  fetchTags()
}

const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  currentPage.value = 1
  fetchTags()
}

const openModal = (tag = null) => {
  if (tag) {
    editingTag.value = tag
    form.value = { name: tag.name }
  } else {
    editingTag.value = null
    form.value = { name: '' }
  }
  formErrors.value = { name: '' }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingTag.value = null
  formErrors.value = { name: '' }
}

const validateForm = () => {
  let isValid = true
  formErrors.value = { name: '' }

  if (!form.value.name.trim()) {
    formErrors.value.name = '标签名称不能为空'
    isValid = false
  }

  return isValid
}

const saveTag = async () => {
  if (!validateForm()) return

  try {
    if (editingTag.value) {
      await adminTagApi.update(editingTag.value.id, {
        name: form.value.name.trim()
      })
    } else {
      await adminTagApi.create({
        name: form.value.name.trim()
      })
    }
    fetchTags()
    closeModal()
  } catch (error) {
    console.error('保存标签失败:', error)
    if (error.message && error.message.includes('已存在')) {
      formErrors.value.name = '标签名称已存在'
    } else {
      alert('保存失败: ' + (error.message || '未知错误'))
    }
  }
}

const deleteTag = async (tag) => {
  if (confirm(`确定要删除标签"${tag.name}"吗？`)) {
    try {
      await adminTagApi.delete(tag.id)
      fetchTags()
    } catch (error) {
      console.error('删除标签失败:', error)
      alert('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

const handlePageChange = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchTags()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchTags()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchTags()
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  fetchTags()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">标签管理</h1>
      <button
        @click="openModal()"
        class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium flex items-center gap-2 w-fit"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        新增标签
      </button>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
      <!-- 搜索和排序栏 -->
      <div class="p-4 border-b border-gray-200 dark:border-gray-700">
        <div class="flex flex-col sm:flex-row gap-4">
          <div class="relative flex-1 max-w-md">
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="searchQuery"
              @input="onSearch"
              type="text"
              placeholder="搜索标签名称..."
              class="w-full pl-10 pr-4 py-2 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500 transition-colors"
            />
          </div>
          <div class="flex items-center gap-2">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              @click="onSortChange(option.value)"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-1"
              :class="sortField === option.value
                ? 'bg-red-500 text-white'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
            >
              {{ option.label }}
              <svg v-if="sortField === option.value" class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" @click.stop="toggleSortOrder">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="sortOrder === 'desc' ? 'M19 9l-7 7-7-7' : 'M5 15l7-7 7 7'" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="p-8 text-center">
        <div class="inline-block w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50 dark:bg-gray-700/50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">ID</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">标签名称</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">使用次数</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">创建时间</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
            <tr v-for="tag in tags" :key="tag.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
              <td class="px-6 py-4 text-sm text-gray-900 dark:text-white">{{ tag.id }}</td>
              <td class="px-6 py-4">
                <span class="inline-flex items-center px-3 py-1 text-sm font-medium bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-full">
                  {{ tag.name }}
                </span>
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 rounded-full">
                  {{ tag.usageCount }} 次
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500 dark:text-gray-400 hidden md:table-cell">{{ formatDate(tag.createdAt) }}</td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    @click="openModal(tag)"
                    class="p-1.5 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
                    title="编辑"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    @click="deleteTag(tag)"
                    class="p-1.5 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
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

      <div v-if="!loading && tags.length === 0" class="px-6 py-12 text-center">
        <svg class="w-12 h-12 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
        </svg>
        <p class="mt-4 text-gray-500 dark:text-gray-400">
          {{ searchQuery ? '未找到匹配的标签' : '暂无标签数据' }}
        </p>
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

    <Teleport to="body">
      <transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="closeModal"></div>
          <div class="relative w-full max-w-md bg-white dark:bg-gray-800 rounded-xl shadow-2xl">
            <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                {{ editingTag ? '编辑标签' : '新增标签' }}
              </h3>
            </div>
            <div class="p-6 space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  标签名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.name"
                  type="text"
                  placeholder="请输入标签名称"
                  class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500 transition-colors"
                  :class="formErrors.name ? 'border-red-500' : 'border-transparent'"
                />
                <p v-if="formErrors.name" class="mt-1 text-xs text-red-500">{{ formErrors.name }}</p>
              </div>
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">预览效果</p>
                <span class="inline-flex items-center px-3 py-1 text-sm font-medium bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-full">
                  {{ form.name || '标签名称' }}
                </span>
              </div>
            </div>
            <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex items-center justify-end gap-3">
              <button
                @click="closeModal"
                class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                取消
              </button>
              <button
                @click="saveTag"
                class="px-4 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>
