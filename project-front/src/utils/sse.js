/**
 * SSE (Server-Sent Events) 连接管理器
 * 用于接收服务器推送的通知消息
 */
import {getToken} from './request'
import JSONbig from 'json-bigint'

const JSONbigString = JSONbig({ storeAsString: true })

class SseManager {
  constructor() {
    this.eventSource = null
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.listeners = new Map()
    this.isConnecting = false
  }

  /**
   * 建立 SSE 连接
   */
  connect() {
    if (this.isConnecting || (this.eventSource && this.eventSource.readyState === EventSource.OPEN)) {
      return
    }

    const token = getToken()
    if (!token) {
      console.warn('[SSE] 未登录，无法建立连接')
      return
    }

    this.isConnecting = true

    // 关闭旧连接
    this.disconnect()

    // 创建新连接（通过 URL 参数传递 token）
    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
    const url = `${baseUrl}/interaction/sse/connect?token=${encodeURIComponent(token)}`
    
    try {
      this.eventSource = new EventSource(url)

      // 连接成功
      this.eventSource.addEventListener('connect', (event) => {
        this.reconnectAttempts = 0
        this.isConnecting = false
      })

      // 接收通知
      this.eventSource.addEventListener('notification', (event) => {
        try {
          const notification = JSONbigString.parse(event.data)
          this.emit('notification', notification)
        } catch (e) {
          console.error('[SSE] 解析通知失败:', e)
        }
      })

      // 接收在线状态变更（用户上下线广播）
      this.eventSource.addEventListener('user_online', (event) => {
        try {
          const data = JSONbigString.parse(event.data)
          this.emit('user_online', data)
        } catch (e) {
          console.error('[SSE] 解析在线状态失败:', e)
        }
      })

      // 接收未读消息数更新
      this.eventSource.addEventListener('unread_update', (event) => {
        try {
          const data = JSONbigString.parse(event.data)
          this.emit('unread_update', data)
        } catch (e) {
          console.error('[SSE] 解析未读消息数失败:', e)
        }
      })

      // 连接错误
      this.eventSource.onerror = (error) => {
        console.error('[SSE] 连接错误:', error)
        this.isConnecting = false
        this.reconnect()
      }

      // 连接关闭
      this.eventSource.onclose = () => {
        this.isConnecting = false
      }
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error)
      this.isConnecting = false
      this.reconnect()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = 0
    this.isConnecting = false
  }

  /**
   * 重连机制
   */
  reconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[SSE] 达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.reconnectDelay)
  }

  /**
   * 监听事件
   */
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  /**
   * 移除监听
   */
  off(event, callback) {
    if (!this.listeners.has(event)) return
    const callbacks = this.listeners.get(event)
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    if (!this.listeners.has(event)) return
    this.listeners.get(event).forEach(callback => {
      try {
        callback(data)
      } catch (error) {
        console.error(`[SSE] 执行回调失败:`, error)
      }
    })
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return this.eventSource && this.eventSource.readyState === EventSource.OPEN
  }
}

// 单例模式
export const sseManager = new SseManager()

export default sseManager
