/**
 * 通知状态管理
 */
import {defineStore} from 'pinia'
import {ref} from 'vue'
import {interactionApi} from '@/utils/request'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const currentChatUserId = ref(null)
  
  const fetchUnreadCount = async () => {
    try {
      const [bizRes, systemRes] = await Promise.all([
        interactionApi.getBizUnreadCount(),
        interactionApi.getSystemUnreadCount()
      ])
      
      unreadCount.value = (bizRes?.count || 0) + (systemRes?.count || 0)
    } catch (error) {
      console.error('获取未读消息数失败:', error)
    }
  }
  
  const decrementUnreadCount = (count = 1) => {
    unreadCount.value = Math.max(0, unreadCount.value - count)
  }
  
  const setCurrentChatUser = (userId) => {
    currentChatUserId.value = userId ? String(userId) : null
  }
  
  const clearCurrentChatUser = () => {
    currentChatUserId.value = null
  }
  
  return {
    unreadCount,
    currentChatUserId,
    fetchUnreadCount,
    decrementUnreadCount,
    setCurrentChatUser,
    clearCurrentChatUser
  }
})
