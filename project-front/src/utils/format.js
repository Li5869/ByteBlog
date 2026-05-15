/**
 * 公共格式化工具函数
 *
 * 提取自 15+ 个组件中重复定义的 formatNumber / formatDate / formatRelativeTime
 */

/**
 * 大数字缩写（15000 -> "1.5w", 1500 -> "1.5k"）
 */
export const formatNumber = (num) => {
  if (!num && num !== 0) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

/**
 * 绝对日期格式化（"2024年3月15日"）
 */
export const formatAbsoluteDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

/**
 * 相对日期格式化（"今天"、"昨天"、"3天前"、"3月15日"）
 */
export const formatRelativeDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

/**
 * 相对时间格式化（"刚刚"、"5分钟前"、"3小时前"、"3天前"）
 */
export const formatRelativeTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatAbsoluteDate(dateStr)
}

/**
 * 日期时间格式化（"03/15 14:30"）
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
