<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[9999] space-y-3 pointer-events-none">
      <TransitionGroup name="notification">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="pointer-events-auto bg-white dark:bg-gray-800 rounded-xl shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden max-w-sm w-80 cursor-pointer hover:shadow-3xl transition-all duration-300"
          @click="handleClick(notification)"
        >
          <div class="p-4">
            <div class="flex items-start gap-3">
              <div class="relative flex-shrink-0">
                <img
                  :src="notification.senderAvatar || defaultAvatar"
                  :alt="notification.senderName"
                  class="w-12 h-12 rounded-full object-cover ring-2 ring-gray-100 dark:ring-gray-700"
                />
                <div class="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white dark:border-gray-800"></div>
              </div>
              
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between mb-1">
                  <h4 class="text-sm font-semibold text-gray-900 dark:text-white truncate">
                    {{ notification.senderName }}
                  </h4>
                  <button
                    @click.stop="removeNotification(notification.id)"
                    class="flex-shrink-0 ml-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                
                <p class="text-sm text-gray-600 dark:text-gray-300 line-clamp-2">
                  {{ notification.content }}
                </p>
                
                <div class="flex items-center gap-2 mt-2">
                  <span class="text-xs text-gray-400">
                    {{ formatTime(notification.timestamp) }}
                  </span>
                  <span v-if="notification.hasImage" class="text-xs text-blue-500 flex items-center gap-1">
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    图片
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import wsManager from '@/utils/websocket'
import {useNotificationStore} from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const notificationStore = useNotificationStore()
const notifications = ref([])
const defaultAvatar = new URL('@/assets/default-avatar.svg', import.meta.url).href

let notificationId = 0
const AUTO_CLOSE_DURATION = 5000

const addNotification = (data) => {
  const id = ++notificationId
  const notification = {
    id,
    senderId: data.senderId,
    senderName: data.senderName || '用户',
    senderAvatar: data.senderAvatar,
    content: data.content,
    hasImage: data.hasImage || false,
    messageId: data.messageId,
    timestamp: data.timestamp || Date.now()
  }
  
  notifications.value.push(notification)
  
  setTimeout(() => {
    removeNotification(id)
  }, AUTO_CLOSE_DURATION)
}

const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

const handleClick = (notification) => {
  router.push({
    path: '/notifications',
    query: { userId: notification.senderId }
  })
  removeNotification(notification.id)
}

const formatTime = (timestamp) => {
  const now = Date.now()
  const diff = now - timestamp
  
  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    const date = new Date(timestamp)
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
  }
}

const isInChatWithSender = (senderId) => {
  const currentChatUserId = notificationStore.currentChatUserId
  if (!currentChatUserId) {
    return false
  }
  
  return String(currentChatUserId) === String(senderId)
}

const handlePrivateMessage = (data) => {
  console.log('[MessageNotification] 收到私信推送:', data)
  console.log('[MessageNotification] 当前登录用户:', getUserInfo()?.id)
  console.log('[MessageNotification] 发送者ID:', data.senderId)
  console.log('[MessageNotification] Store中的当前聊天用户ID:', notificationStore.currentChatUserId)
  
  const currentUserId = getUserInfo()?.id
  if (currentUserId && String(currentUserId) === String(data.senderId)) {
    console.log('[MessageNotification] 这是自己发送的消息，不显示通知')
    return
  }
  
  if (isInChatWithSender(data.senderId)) {
    console.log('[MessageNotification] 用户正在与发送者聊天，不显示弹窗通知')
    return
  }
  
  addNotification(data)
}

const getUserInfo = () => {
  const userInfo = localStorage.getItem('userInfo')
  return userInfo ? JSON.parse(userInfo) : null
}

onMounted(() => {
  wsManager.on('private_message', handlePrivateMessage)
})

onUnmounted(() => {
  wsManager.off('private_message', handlePrivateMessage)
})
</script>

<style scoped>
.notification-enter-active {
  animation: slideInRight 0.3s ease-out;
}

.notification-leave-active {
  animation: slideOutRight 0.3s ease-in;
}

.notification-enter-from,
.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOutRight {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(100%);
  }
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
