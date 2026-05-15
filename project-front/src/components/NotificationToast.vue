<template>
  <Transition name="notification">
    <div v-if="visible" class="notification-toast" :class="toastClass" @click="handleClick">
      <!-- 审核通知 -->
      <template v-if="isModeration">
        <div class="moderation-content">
          <div class="moderation-icon" :class="moderationIconClass">
            <svg v-if="notification.reviewStatus === 'approved'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div class="moderation-info">
            <div class="moderation-title">
              <span class="status-text" :class="notification.reviewStatus === 'approved' ? 'approved' : 'rejected'">
                {{ notification.reviewStatus === 'approved' ? '审核通过' : '审核未通过' }}
              </span>
            </div>
            <div class="moderation-detail">
              您的{{ getBizTypeName(notification.bizType) }}「{{ truncateTitle(notification.title) }}」
              <template v-if="notification.reviewStatus === 'rejected' && notification.reason">
                ，原因：{{ notification.reason }}
              </template>
            </div>
            <div class="time">{{ formatTime(notification.createdAt) }}</div>
          </div>
          <button class="close-btn" @click.stop="close">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </template>
      
      <!-- 普通通知 -->
      <template v-else>
        <div class="notification-content">
          <img :src="notification.senderAvatar || defaultAvatar" class="avatar" />
          <div class="info">
            <div class="title">
              <span class="nickname">{{ notification.senderNickname }}</span>
              <span class="action">{{ getActionText(notification.actionType) }}</span>
            </div>
            <div class="time">{{ formatTime(notification.createdAt) }}</div>
          </div>
          <button class="close-btn" @click.stop="close">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </template>
    </div>
  </Transition>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {sseManager} from '@/utils/sse'

const router = useRouter()
const visible = ref(false)
const notification = ref({})
let timer = null

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIiBmaWxsPSJub25lIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI0U1RTdFQiIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzgiIHI9IjE4IiBmaWxsPSIjOUNBM0FGIi8+CiAgPHBhdGggZD0iTTIwIDg1QzIwIDY1IDM1IDU1IDUwIDU1QzY1IDU1IDgwIDY1IDgwIDg1IiBmaWxsPSIjOUNBM0FGIi8+Cjwvc3ZnPg=='

const isModeration = computed(() => notification.value.type === 'moderation')

const moderationIconClass = computed(() => ({
  'approved-icon': notification.value.reviewStatus === 'approved',
  'rejected-icon': notification.value.reviewStatus === 'rejected'
}))

const toastClass = computed(() => {
  if (!isModeration.value) return {}
  return {
    'moderation-toast': true,
    'approved': notification.value.reviewStatus === 'approved',
    'rejected': notification.value.reviewStatus === 'rejected'
  }
})

const getBizTypeName = (bizType) => {
  const types = {
    'article': '文章',
    'comment': '评论',
    'question': '问题'
  }
  return types[bizType] || '内容'
}

const getActionText = (actionType) => {
  const actions = {
    'follow': '关注了你',
    'like': '点赞了你的文章',
    'comment': '评论了你的文章',
    'reply': '回复了你的评论',
    'collection': '收藏了你的文章',
    'answer': '回答了你的问题',
    'adopt': '采纳了你的回答'
  }
  return actions[actionType] || '与你互动'
}

const truncateTitle = (title, maxLength = 15) => {
  if (!title) return '内容'
  if (title.length <= maxLength) return title
  return title.substring(0, maxLength) + '...'
}

const formatTime = (time) => {
  if (!time) return '刚刚'
  
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${Math.floor(diff / 86400)}天前`
}

const showNotification = (data) => {
  notification.value = data
  visible.value = true
  
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    close()
  }, 5000)
}

const close = () => {
  visible.value = false
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

const handleClick = () => {
  if (isModeration.value) {
    if (notification.value.bizType === 'article' && notification.value.bizId) {
      router.push(`/article/${notification.value.bizId}`)
    } else {
      router.push('/mine?tab=articles')
    }
  } else {
    router.push('/notifications')
  }
  close()
}

onMounted(() => {
  sseManager.on('notification', showNotification)
})

onUnmounted(() => {
  sseManager.off('notification', showNotification)
  if (timer) clearTimeout(timer)
})
</script>

<style scoped>
.notification-toast {
  position: fixed;
  top: 80px;
  right: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 16px;
  z-index: 9999;
  cursor: pointer;
  transition: all 0.3s ease;
  max-width: 360px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.notification-toast:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.notification-content,
.moderation-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.info,
.moderation-info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.nickname {
  font-weight: 600;
  color: #1a1a1a;
}

.action {
  color: #666;
}

.time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.close-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f5f5f5;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.2s;
  opacity: 0;
}

.notification-toast:hover .close-btn {
  opacity: 1;
}

.close-btn:hover {
  background: #e0e0e0;
  color: #666;
}

/* 审核通知样式 */
.moderation-toast {
  border-left: 4px solid;
}

.moderation-toast.approved {
  border-left-color: #10b981;
}

.moderation-toast.rejected {
  border-left-color: #ef4444;
}

.moderation-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.approved-icon {
  background: linear-gradient(135deg, #10b981, #34d399);
  color: white;
}

.rejected-icon {
  background: linear-gradient(135deg, #ef4444, #f87171);
  color: white;
}

.moderation-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.status-text {
  font-weight: 600;
}

.status-text.approved {
  color: #10b981;
}

.status-text.rejected {
  color: #ef4444;
}

.moderation-detail {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

/* 暗色模式 */
:global(.dark) .notification-toast {
  background: #1f2937;
  border-color: rgba(255, 255, 255, 0.1);
}

:global(.dark) .title,
:global(.dark) .moderation-title {
  color: #e5e7eb;
}

:global(.dark) .nickname {
  color: #f3f4f6;
}

:global(.dark) .action,
:global(.dark) .moderation-detail {
  color: #9ca3af;
}

:global(.dark) .time {
  color: #6b7280;
}

:global(.dark) .close-btn {
  background: #374151;
  color: #9ca3af;
}

:global(.dark) .close-btn:hover {
  background: #4b5563;
  color: #d1d5db;
}

/* 动画 */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.notification-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .notification-toast {
    top: 70px;
    right: 10px;
    left: 10px;
    max-width: none;
  }
}
</style>
