/**
 * 在线状态 Hook
 *
 * 提供用户在线状态的查询、缓存和实时更新功能
 * 同时监听 WebSocket 和 SSE 两个通道的在线状态变更
 */

import {onMounted, onUnmounted, ref} from 'vue'
import wsManager from '@/utils/websocket'
import sseManager from '@/utils/sse'

export function useOnlineStatus() {
  // userId 作为字符串 key（雪花 ID 超过 JS 安全整数范围，必须用字符串避免精度丢失）
  const onlineStatus = ref({})
  const isConnected = ref(false)

  // WebSocket 状态变更回调
  const handleWsStatusChange = (userId, isOnline) => {
    onlineStatus.value = { ...onlineStatus.value, [userId]: isOnline }
  }

  // SSE 在线状态变更回调（格式：{ userId, online }）
  const handleSseOnlineChange = (data) => {
    if (data && data.userId != null) {
      const userId = String(data.userId)
      onlineStatus.value = { ...onlineStatus.value, [userId]: data.online }
    }
  }

  const handleConnected = () => {
    isConnected.value = true
  }

  const handleDisconnected = () => {
    isConnected.value = false
  }

  onMounted(() => {
    // 监听 WebSocket 的 status_change 事件（所有 WS 状态变更都通过此事件广播）
    wsManager.on('status_change', handleWsStatusChange)
    wsManager.on('connected', handleConnected)
    wsManager.on('disconnected', handleDisconnected)
    // 监听 SSE 的 user_online 事件（在线状态广播的补充通道）
    sseManager.on('user_online', handleSseOnlineChange)

    isConnected.value = wsManager.isConnected
  })

  onUnmounted(() => {
    wsManager.off('status_change', handleWsStatusChange)
    wsManager.off('connected', handleConnected)
    wsManager.off('disconnected', handleDisconnected)
    sseManager.off('user_online', handleSseOnlineChange)
  })

  const queryOnlineStatus = (userIds) => {
    wsManager.queryOnlineStatus(userIds)
  }

  const isOnline = (userId) => {
    // userId 保持字符串
    const id = String(userId)
    const status = onlineStatus.value[id] ?? wsManager.getCachedStatus(id) ?? false
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
