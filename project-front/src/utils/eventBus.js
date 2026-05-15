/**
 * 简易事件总线（无依赖版本）
 */

// 事件处理器存储
const handlers = {}

// 订阅事件
export const on = (event, handler) => {
  if (!handlers[event]) {
    handlers[event] = []
  }
  handlers[event].push(handler)
}

// 取消订阅
export const off = (event, handler) => {
  if (!handlers[event]) return
  handlers[event] = handlers[event].filter(h => h !== handler)
}

// 触发事件
export const emit = (event, data) => {
  if (!handlers[event]) {
    return
  }
  handlers[event].forEach(handler => handler(data))
}

// 事件类型
export const Events = {
  // 通知已读（带减少的未读数）
  NOTIFICATION_READ: 'notification:read',
  // 私信已读（带减少的未读数）
  MESSAGE_READ: 'message:read',
  // 刷新未读数
  REFRESH_UNREAD_COUNT: 'notification:refreshUnreadCount'
}
