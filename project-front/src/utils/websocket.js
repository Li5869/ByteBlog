/**
 * WebSocket 管理器
 * 
 * 提供统一的 WebSocket 连接管理、心跳检测、自动重连功能
 * 支持在线状态查询和状态变更通知
 */

const getToken = () => localStorage.getItem('token')

import JSONbig from 'json-bigint'

const JSONbigString = JSONbig({ storeAsString: true })

class WebSocketManager {
  constructor() {
    this.ws = null
    this.isConnected = false
    this.isConnecting = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 1000
    this.heartbeatInterval = null
    this.heartbeatTimeout = 30000
    this.listeners = new Map()
    this.onlineStatus = new Map()
  }

  connect() {
    const token = getToken()
    if (!token) {
      console.warn('[WebSocket] 未登录，跳过连接')
      return
    }

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }

    if (this.isConnecting) {
      return
    }

    this.isConnecting = true
    const wsUrl = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws?token=${token}`
    
    try {
      this.ws = new WebSocket(wsUrl)
      this.setupEventHandlers()
    } catch (error) {
      console.error('[WebSocket] 连接失败:', error)
      this.isConnecting = false
      this.handleReconnect()
    }
  }

  setupEventHandlers() {
    this.ws.onopen = () => {
      this.isConnected = true
      this.isConnecting = false
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.emit('connected')
    }

    this.ws.onmessage = (event) => {
      this.handleMessage(event.data)
    }

    this.ws.onclose = (event) => {
      this.isConnected = false
      this.isConnecting = false
      this.stopHeartbeat()
      this.emit('disconnected')
      
      if (event.code !== 1000) {
        this.handleReconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('[WebSocket] 连接错误:', error)
      this.isConnected = false
      this.isConnecting = false
      this.emit('error', error)
    }
  }

  handleMessage(data) {
    if (data === 'pong') {
      return
    }

    try {
      const message = JSONbigString.parse(data)
      
      switch (message.type) {
        case 'welcome':
          this.emit('welcome', message.data)
          break

        case 'online_status':
          this.updateOnlineStatus(message.data)
          this.emit('online_status', message.data)
          break

        case 'user_online':
          this.handleUserOnlineChange(message.data)
          break

        case 'private_message':
          this.emit('private_message', message.data)
          break

        case 'error':
          console.error('[WebSocket] 服务端错误:', message.data)
          this.emit('error', message.data)
          break

        default:
      }
    } catch (error) {
      console.error('[WebSocket] 消息解析失败:', error)
    }
  }

  updateOnlineStatus(data) {
    Object.entries(data).forEach(([userId, isOnline]) => {
      // userId 保持字符串，避免精度丢失
      const id = String(userId)
      const prev = this.onlineStatus.get(id)
      if (prev !== isOnline) {
        this.onlineStatus.set(id, isOnline)
        // 广播单个用户的变更，所有监听者都能收到
        this.emit('status_change', id, isOnline)
      }
    })
  }

  handleUserOnlineChange(data) {
    const { userId, online } = data
    this.updateOnlineStatus({ [userId]: online })
  }

  send(type, data) {
    if (!this.isConnected || !this.ws) {
      console.warn('[WebSocket] 未连接，无法发送消息')
      return false
    }

    try {
      const message = JSON.stringify({ type, data, timestamp: Date.now() })
      this.ws.send(message)
      return true
    } catch (error) {
      console.error('[WebSocket] 发送消息失败:', error)
      return false
    }
  }

  queryOnlineStatus(userIds) {
    if (!Array.isArray(userIds) || userIds.length === 0) {
      return
    }
    // 直接发送查询请求，响应通过 updateOnlineStatus → status_change 事件统一处理
    this.send('query_online', userIds)
  }

  getCachedStatus(userId) {
    // userId 保持字符串
    return this.onlineStatus.get(String(userId)) ?? null
  }

  startHeartbeat() {
    this.stopHeartbeat()
    
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, this.heartbeatTimeout)
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  handleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] 达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1)

    setTimeout(() => {
      this.connect()
    }, delay)
  }

  disconnect() {
    this.stopHeartbeat()
    
    if (this.ws) {
      this.ws.close(1000, '主动断开')
      this.ws = null
    }
    
    this.isConnected = false
    this.isConnecting = false
    this.onlineStatus.clear()
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    if (!this.listeners.has(event)) return
    
    if (callback) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    } else {
      this.listeners.delete(event)
    }
  }

  emit(event, data) {
    if (!this.listeners.has(event)) return
    
    this.listeners.get(event).forEach(callback => {
      try {
        callback(data)
      } catch (error) {
        console.error(`[WebSocket] 事件回调执行失败 (${event}):`, error)
      }
    })
  }
}

export const wsManager = new WebSocketManager()

export default wsManager
