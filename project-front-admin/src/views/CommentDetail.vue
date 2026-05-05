<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {adminCommentApi} from '../utils/request'

const route = useRoute()
const router = useRouter()

const commentId = computed(() => route.params.id)
const loading = ref(true)
const comment = ref(null)
const replies = ref([])

const fetchDetail = async () => {
  loading.value = true
  try {
    const detail = await adminCommentApi.getDetail(commentId.value)
    comment.value = detail
    replies.value = detail.replies || []
  } catch (e) {
    console.error('获取评论详情失败:', e)
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const texts = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return texts[status] || status
}

const getStatusClass = (status) => {
  const classes = {
    pending: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    approved: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    rejected: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400'
  }
  return classes[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goBack = () => {
  router.back()
}

const handleApprove = async () => {
  try {
    await adminCommentApi.approve(commentId.value)
    comment.value.status = 'approved'
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

const handleReject = async () => {
  try {
    await adminCommentApi.reject(commentId.value)
    comment.value.status = 'rejected'
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

const handleDelete = async () => {
  if (confirm('确定要删除该评论吗？')) {
    try {
      await adminCommentApi.delete(commentId.value)
      router.back()
    } catch (e) {
      alert(e.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="mb-6">
        <button
          @click="goBack"
          class="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          返回列表
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-12">
        <div class="inline-block w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="mt-2 text-gray-500 dark:text-gray-400">加载中...</p>
      </div>

      <div v-else-if="comment" class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-start justify-between gap-4">
            <div class="flex items-start gap-4">
              <img
                :src="comment.authorAvatar"
                :alt="comment.authorName"
                class="w-12 h-12 rounded-full object-cover"
              />
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <span class="font-semibold text-gray-900 dark:text-white">{{ comment.authorName }}</span>
                  <span
                    class="px-2 py-1 text-xs rounded-full"
                    :class="getStatusClass(comment.status)"
                  >
                    {{ getStatusText(comment.status) }}
                  </span>
                </div>
                <div class="text-sm text-gray-500 dark:text-gray-400">
                  {{ comment.authorEmail || '-' }}
                </div>
              </div>
            </div>
            <div class="text-right text-sm text-gray-500 dark:text-gray-400">
              <div>评论ID: {{ comment.id }}</div>
              <div>{{ formatDate(comment.createdAt) }}</div>
            </div>
          </div>
        </div>

        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">评论内容</h2>
          <p class="text-gray-900 dark:text-white leading-relaxed">
            {{ comment.content }}
          </p>
        </div>

        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-4">关联内容</h2>
          <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <div class="flex items-center gap-2 mb-2">
              <span class="px-2 py-1 text-xs bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400 rounded">
                文章
              </span>
              <span class="text-sm text-gray-500 dark:text-gray-400">ID: {{ comment.targetId }}</span>
            </div>
            <div class="text-gray-900 dark:text-white font-medium mb-1">
              {{ comment.targetTitle || '-' }}
            </div>
          </div>
        </div>

        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-4">统计信息</h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
            <div class="text-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ comment.likes }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">点赞数</div>
            </div>
            <div class="text-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ replies.length }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">回复数</div>
            </div>
            <div class="text-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-sm font-medium text-gray-900 dark:text-white">{{ comment.reviewedAt ? formatDate(comment.reviewedAt) : '-' }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">审核时间</div>
            </div>
          </div>
        </div>

        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-4">回复列表</h2>
          <div v-if="replies.length > 0" class="space-y-4">
            <div
              v-for="reply in replies"
              :key="reply.id"
              class="flex items-start gap-3 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
            >
              <img
                :src="reply.authorAvatar"
                :alt="reply.authorName"
                class="w-8 h-8 rounded-full object-cover"
              />
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-1">
                  <span class="font-medium text-gray-900 dark:text-white text-sm">{{ reply.authorName }}</span>
                  <span class="text-xs text-gray-500 dark:text-gray-400">{{ formatDate(reply.createdAt) }}</span>
                </div>
                <p class="text-sm text-gray-600 dark:text-gray-300">{{ reply.content }}</p>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8 text-gray-500 dark:text-gray-400">
            暂无回复
          </div>
        </div>

        <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700/50 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between">
            <div class="text-sm text-gray-500 dark:text-gray-400">
              <span v-if="comment.reviewedAt">审核时间：{{ formatDate(comment.reviewedAt) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <button
                v-if="comment.status !== 'approved'"
                @click="handleApprove"
                class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
              >
                通过审核
              </button>
              <button
                v-if="comment.status !== 'rejected'"
                @click="handleReject"
                class="px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition-colors"
              >
                拒绝
              </button>
              <button
                @click="handleDelete"
                class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
              >
                删除评论
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
