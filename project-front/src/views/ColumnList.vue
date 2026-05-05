<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {NPagination, NSelect} from 'naive-ui'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import {columnApi} from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const columns = ref([])
const pagination = ref({ current: 1, size: 12, total: 0, pages: 0 })
const orderBy = ref('created_at')

const orderByOptions = [
  { label: '最新创建', value: 'created_at' },
  { label: '最多文章', value: 'articles_count' },
  { label: '最多浏览', value: 'views' }
]

const fetchColumns = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.value.current,
      size: pagination.value.size,
      orderBy: orderBy.value,
      status: 1
    }
    const result = await columnApi.getColumnPage(params)
    columns.value = result.records || []
    pagination.value.total = result.total || 0
    pagination.value.pages = result.pages || 0
  } catch (error) {
    console.error('获取专栏列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pagination.value.current = page
  fetchColumns()
}

const goToColumn = (columnId) => {
  router.push({ name: 'ColumnDetail', params: { id: columnId } })
}

const goToAuthor = (authorId, event) => {
  event.stopPropagation()
  router.push({ name: 'UserHome', params: { id: authorId } })
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchColumns()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 relative">
        <div class="text-center">
          <h1 class="text-3xl sm:text-4xl font-bold text-white mb-4">
            专栏
          </h1>
          <p class="text-white/80 text-lg max-w-2xl mx-auto">
            发现优质专栏，系统学习技术知识
          </p>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="flex justify-end mb-6">
        <div class="w-48">
          <n-select
            v-model:value="orderBy"
            :options="orderByOptions"
            placeholder="排序方式"
            size="large"
            @update:value="fetchColumns"
          />
        </div>
      </div>

      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="i in 6" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm border border-gray-100 dark:border-gray-700">
          <div class="h-40 bg-gray-200 dark:bg-gray-700 animate-pulse"></div>
          <div class="p-5 space-y-3">
            <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-2/3"></div>
          </div>
        </div>
      </div>

      <div v-else-if="columns.length === 0" class="text-center py-20">
        <div class="w-24 h-24 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
          <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <p class="text-gray-500 dark:text-gray-400 text-lg font-medium">暂无专栏</p>
        <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">还没有找到相关的专栏</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="column in columns"
          :key="column.id"
          @click="goToColumn(column.id)"
          class="group bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm border border-gray-100 dark:border-gray-700 hover:shadow-xl hover:border-purple-200 dark:hover:border-purple-800 transition-all duration-300 cursor-pointer"
        >
          <div class="relative h-40 overflow-hidden">
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
            <div class="absolute bottom-3 left-3 right-3">
              <h3 class="text-lg font-bold text-white line-clamp-1">{{ column.title }}</h3>
            </div>
          </div>

          <div class="p-5">
            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-4 min-h-[40px]">
              {{ column.description || '暂无描述' }}
            </p>

            <div class="flex items-center justify-between">
              <div
                class="flex items-center gap-2 cursor-pointer hover:opacity-80 transition-opacity"
                @click="goToAuthor(column.authorId, $event)"
              >
                <img
                  :src="column.authorAvatar || DEFAULT_AVATAR"
                  :alt="column.authorName"
                  class="w-8 h-8 rounded-full object-cover border-2 border-white dark:border-gray-700 shadow-sm"
                />
                <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {{ column.authorName }}
                </span>
              </div>

              <div class="flex items-center gap-3 text-xs text-gray-400 dark:text-gray-500">
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
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="pagination.pages > 1" class="flex justify-center mt-8">
        <n-pagination
          v-model:page="pagination.current"
          :page-count="pagination.pages"
          :page-size="pagination.size"
          @update:page="handlePageChange"
        />
      </div>
    </div>
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
