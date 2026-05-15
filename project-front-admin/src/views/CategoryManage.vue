<script setup>
import {onMounted, ref} from 'vue'
import {adminCategoryApi} from '@/utils/request'
import {formatAbsoluteDate} from '@/utils/format'

const categories = ref([])
const loading = ref(false)
const showModal = ref(false)
const editingCategory = ref(null)
const form = ref({
  name: '',
  sort: 0
})
const formErrors = ref({
  name: ''
})

const fetchCategories = async () => {
  loading.value = true
  try {
    categories.value = await adminCategoryApi.getList()
  } catch (error) {
    console.error('获取分类列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCategories()
})

const openModal = (category = null) => {
  if (category) {
    editingCategory.value = category
    form.value = { name: category.name, sort: category.sort }
  } else {
    editingCategory.value = null
    form.value = { name: '', sort: categories.value.length + 1 }
  }
  formErrors.value = { name: '' }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingCategory.value = null
  formErrors.value = { name: '' }
}

const validateForm = () => {
  let isValid = true
  formErrors.value = { name: '' }

  if (!form.value.name.trim()) {
    formErrors.value.name = '分类名称不能为空'
    isValid = false
  }

  return isValid
}

const saveCategory = async () => {
  if (!validateForm()) return

  try {
    if (editingCategory.value) {
      await adminCategoryApi.update(editingCategory.value.id, {
        name: form.value.name,
        sort: form.value.sort
      })
    } else {
      await adminCategoryApi.create({
        name: form.value.name,
        sort: form.value.sort
      })
    }
    fetchCategories()
    closeModal()
  } catch (error) {
    console.error('保存分类失败:', error)
    alert('保存失败: ' + (error.message || '未知错误'))
  }
}

const deleteCategory = async (category) => {
  if (confirm(`确定要删除分类"${category.name}"吗？`)) {
    try {
      await adminCategoryApi.delete(category.id)
      fetchCategories()
    } catch (error) {
      console.error('删除分类失败:', error)
      alert('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">分类管理</h1>
      <button
        @click="openModal()"
        class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium flex items-center gap-2 w-fit"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        新增分类
      </button>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50 dark:bg-gray-700/50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">ID</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">分类名称</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">排序</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">文章数量</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">创建时间</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
            <tr v-for="category in categories" :key="category.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
              <td class="px-6 py-4 text-sm text-gray-900 dark:text-white">{{ category.id }}</td>
              <td class="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">{{ category.name }}</td>
              <td class="px-6 py-4">
                <span class="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 rounded-full">
                  {{ category.sort }}
                </span>
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 rounded-full">
                  {{ category.articleCount }} 篇
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500 dark:text-gray-400 hidden md:table-cell">{{ formatAbsoluteDate(category.createdAt) }}</td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    @click="openModal(category)"
                    class="p-1.5 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
                    title="编辑"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    @click="deleteCategory(category)"
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
      
      <div v-if="categories.length === 0" class="px-6 py-12 text-center">
        <svg class="w-12 h-12 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <p class="mt-4 text-gray-500 dark:text-gray-400">暂无分类数据</p>
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
                {{ editingCategory ? '编辑分类' : '新增分类' }}
              </h3>
            </div>
            <div class="p-6 space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  分类名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.name"
                  type="text"
                  placeholder="请输入分类名称"
                  class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500 transition-colors"
                  :class="formErrors.name ? 'border-red-500' : 'border-transparent'"
                />
                <p v-if="formErrors.name" class="mt-1 text-xs text-red-500">{{ formErrors.name }}</p>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">排序</label>
                <input
                  v-model.number="form.sort"
                  type="number"
                  min="0"
                  placeholder="请输入排序数字（数字越小越靠前）"
                  class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500 transition-colors"
                />
                <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">数字越小，排序越靠前</p>
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
                @click="saveCategory"
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
