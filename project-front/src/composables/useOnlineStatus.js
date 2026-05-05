/**
 * 在线状态 Hook
 *
 * 提供用户在线状态的查询、缓存和实时更新功能
 */

import {onMounted, onUnmounted, ref} from 'vue'
import wsManager from '@/utils/websocket'

export function useOnlineStatus() {
  // userId 作为字符串 key（雪花 ID 超过 JS 安全整数范围，必须用字符串避免精度丢失）
  const onlineStatus = ref({})
  const isConnected = ref(false)

  const handleStatusChange = (userId, isOnline) => {
    console.log(`[useOnlineStatus] 状态变更: 用户 ${userId} -> ${isOnline}`)
    onlineStatus.value = { ...onlineStatus.value, [userId]: isOnline }
  }

  const handleConnected = () => {
    console.log('[useOnlineStatus] WebSocket 已连接')
    isConnected.value = true
  }

  const handleDisconnected = () => {
    console.log('[useOnlineStatus] WebSocket 已断开')
    isConnected.value = false
  }

  onMounted(() => {
    console.log('[useOnlineStatus] 注册事件监听')
    // 监听统一的 status_change 事件（所有状态变更都通过此事件广播）
    wsManager.on('status_change', handleStatusChange)
    wsManager.on('connected', handleConnected)
    wsManager.on('disconnected', handleDisconnected)

    isConnected.value = wsManager.isConnected
    console.log('[useOnlineStatus] 当前连接状态:', isConnected.value)
  })

  onUnmounted(() => {
    wsManager.off('status_change', handleStatusChange)
    wsManager.off('connected', handleConnected)
    wsManager.off('disconnected', handleDisconnected)
  })

  const queryOnlineStatus = (userIds) => {
    console.log('[useOnlineStatus] 查询在线状态:', userIds)
    wsManager.queryOnlineStatus(userIds)
  }

  const isOnline = (userId) => {
    // userId 保持字符串
    const id = String(userId)
    const status = onlineStatus.value[id] ?? wsManager.getCachedStatus(id) ?? false
    console.log(`[useOnlineStatus] 查询用户 ${id} 在线状态:`, status)
    return status
  }

  const getOnlineStatusMap = () => {
    return onlineStatus.value
  }

  return {
    onlineStatus,
    isConnected,
    isOnline,
    queryOnlineStatus,
    getOnlineStatusMap,
  }
}

export default useOnlineStatus
