<script setup>
import {computed} from 'vue'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'edit', 'toggle-status'])

const statusText = computed(() => {
  const map = {
    normal: '正常',
    banned: '封禁'
  }
  return map[props.user.status] || props.user.status
})

const statusClass = computed(() => {
  const map = {
    normal: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    banned: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400'
  }
  return map[props.user.status] || ''
})

const formatDateTime = (dateStr) => {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleClose = () => {
  emit('close')
}

const handleEdit = () => {
  emit('edit', props.user)
}

const handleToggleStatus = () => {
  emit('toggle-status', props.user)
}
</script>

<template>
  <div class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex min-h-full items-center justify-center p-4">
      <div 
        class="fixed inset-0 bg-black/50 transition-opacity"
        @click="handleClose"
      ></div>
      
      <div class="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-lg transform transition-all">
        <div class="flex items-center justify-between p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">用户详情</h3>
          <button 
            @click="handleClose"
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="p-4 sm:p-6">
          <div class="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 mb-6">
            <img 
              :src="user.avatar" 
              :alt="user.nickname"
              class="w-20 h-20 sm:w-24 sm:h-24 rounded-full object-cover border-4 border-gray-100 dark:border-gray-700"
            />
            <div class="text-center sm:text-left flex-1">
              <div class="flex items-center justify-center sm:justify-start gap-2 mb-1">
                <h4 class="text-xl font-bold text-gray-900 dark:text-white">{{ user.nickname }}</h4>
                <span 
                  class="px-2 py-0.5 text-xs font-medium rounded-full"
                  :class="statusClass"
                >
                  {{ statusText }}
                </span>
              </div>
              <p class="text-gray-500 dark:text-gray-400 text-sm mb-1">@{{ user.username }}</p>
              <p class="text-gray-600 dark:text-gray-300 text-sm">{{ user.bio }}</p>
            </div>
          </div>
          
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mb-6">
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <div class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">{{ user.articleCount }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">文章</div>
            </div>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <div class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">{{ user.fanCount }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">粉丝</div>
            </div>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <div class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">{{ user.followCount }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">关注</div>
            </div>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <div class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">{{ user.likeCount }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">获赞</div>
            </div>
          </div>
          
          <div class="space-y-3 sm:space-y-4">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-gray-500 dark:text-gray-400">邮箱</div>
                <div class="text-sm text-gray-900 dark:text-white truncate">{{ user.email }}</div>
              </div>
            </div>
            
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-gray-500 dark:text-gray-400">手机号</div>
                <div class="text-sm text-gray-900 dark:text-white">{{ user.phone }}</div>
              </div>
            </div>
            
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-gray-500 dark:text-gray-400">性别</div>
                <div class="text-sm text-gray-900 dark:text-white">{{ user.gender }}</div>
              </div>
            </div>
            
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-gray-500 dark:text-gray-400">注册时间</div>
                <div class="text-sm text-gray-900 dark:text-white">{{ formatDateTime(user.createdAt) }}</div>
              </div>
            </div>
            
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-gray-500 dark:text-gray-400">最后登录</div>
                <div class="text-sm text-gray-900 dark:text-white">{{ formatDateTime(user.lastLoginAt) }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="flex flex-col sm:flex-row items-center justify-end gap-2 sm:gap-3 p-4 sm:p-6 border-t border-gray-200 dark:border-gray-700">
          <button 
            @click="handleClose"
            class="w-full sm:w-auto px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            关闭
          </button>
          <button 
            @click="handleToggleStatus"
            class="w-full sm:w-auto px-4 py-2 text-sm font-medium rounded-lg transition-colors"
            :class="user.status === 'normal' 
              ? 'text-red-600 bg-red-100 hover:bg-red-200 dark:bg-red-900/30 dark:text-red-400 dark:hover:bg-red-900/50' 
              : 'text-green-600 bg-green-100 hover:bg-green-200 dark:bg-green-900/30 dark:text-green-400 dark:hover:bg-green-900/50'"
          >
            {{ user.status === 'normal' ? '封禁用户' : '解封用户' }}
          </button>
          <button 
            @click="handleEdit"
            class="w-full sm:w-auto px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-lg hover:bg-primary-600 transition-colors"
          >
            编辑用户
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
