/**
 * Toast 提示工具
 * 
 * 提供简单的消息提示功能
 */

const showToast = (message, type = 'error', duration = 3000) => {
  const toast = document.createElement('div')
  toast.className = `fixed top-4 right-4 z-[9999] px-6 py-3 rounded-lg shadow-lg transition-all duration-300 transform translate-x-full ${
    type === 'error' 
      ? 'bg-red-500 text-white' 
      : type === 'success' 
      ? 'bg-green-500 text-white'
      : type === 'warning'
      ? 'bg-yellow-500 text-white'
      : 'bg-blue-500 text-white'
  }`
  toast.textContent = message
  
  document.body.appendChild(toast)
  
  requestAnimationFrame(() => {
    toast.classList.remove('translate-x-full')
  })
  
  setTimeout(() => {
    toast.classList.add('translate-x-full')
    setTimeout(() => {
      document.body.removeChild(toast)
    }, 300)
  }, duration)
}

export const toast = {
  error: (message) => showToast(message, 'error'),
  success: (message) => showToast(message, 'success'),
  warning: (message) => showToast(message, 'warning'),
  info: (message) => showToast(message, 'info'),
}

export default toast
