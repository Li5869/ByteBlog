/**
 * 自定义模态框服务
 * 替代浏览器原生的 alert、confirm、prompt
 */

let modalRoot = null
let modalId = 0

// 创建模态框容器（单例）
const getModalRoot = () => {
  if (!modalRoot) {
    modalRoot = document.createElement('div')
    modalRoot.id = 'custom-modal-root'
    modalRoot.innerHTML = `
      <style>
        #custom-modal-root .modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          backdrop-filter: blur(4px);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 99999;
          animation: modalFadeIn 0.2s ease-out;
        }
        
        #custom-modal-root .modal-content {
          background: var(--modal-bg, white);
          border-radius: 16px;
          padding: 24px;
          max-width: 420px;
          width: 90%;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
          animation: modalSlideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
        }
        
        #custom-modal-root .modal-icon {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin: 0 auto 16px;
        }
        
        #custom-modal-root .modal-icon.success {
          background: linear-gradient-to-br from green-400 to-green-500;
        }
        
        #custom-modal-root .modal-icon.error {
          background: linear-gradient-to-br from-red-400 to-red-500;
        }
        
        #custom-modal-root .modal-icon.warning {
          background: linear-gradient-to-br from-orange-400 to-orange-500;
        }
        
        #custom-modal-root .modal-icon.info {
          background: linear-gradient-to-br from-blue-400 to-blue-500;
        }
        
        #custom-modal-root .modal-icon svg {
          width: 24px;
          height: 24px;
          color: white;
        }
        
        #custom-modal-root .modal-title {
          font-size: 18px;
          font-weight: 600;
          text-align: center;
          margin-bottom: 12px;
          color: var(--modal-title-color, #111827);
        }
        
        #custom-modal-root .modal-message {
          font-size: 14px;
          color: var(--modal-text-color, #6b7280);
          text-align: center;
          line-height: 1.6;
          margin-bottom: 24px;
          word-break: break-word;
        }
        
        #custom-modal-root .modal-buttons {
          display: flex;
          gap: 12px;
        }
        
        #custom-modal-root .modal-btn {
          flex: 1;
          padding: 12px 20px;
          border-radius: 10px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
          border: none;
          outline: none;
        }
        
        #custom-modal-root .modal-btn:active {
          transform: scale(0.98);
        }
        
        #custom-modal-root .modal-btn-cancel {
          background: #f3f4f6;
          color: #4b5563;
        }
        
        #custom-modal-root .modal-btn-cancel:hover {
          background: #e5e7eb;
        }
        
        #custom-modal-root .modal-btn-confirm {
          background: linear-gradient-to-r from-red-500 to-rose-500;
          color: white;
          box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4);
        }
        
        #custom-modal-root .modal-btn-confirm:hover {
          background: linear-gradient-to-r from-red-600 to-rose-600);
          box-shadow: 0 6px 20px rgba(239, 68, 68, 0.5);
        }
        
        #custom-modal-root .modal-btn-confirm.success {
          background: linear-gradient-to-r from-green-500 to-emerald-500;
          box-shadow: 0 4px 14px rgba(34, 197, 94, 0.4);
        }
        
        #custom-modal-root .modal-btn-confirm.success:hover {
          background: linear-gradient-to-r from-green-600 to-emerald-600);
        }
        
        /* 输入框样式 */
        #custom-modal-root .modal-input {
          width: 100%;
          padding: 12px 16px;
          border: 2px solid #e5e7eb;
          border-radius: 10px;
          font-size: 14px;
          outline: none;
          transition: border-color 0.2s;
          margin-bottom: 16px;
          box-sizing: border-box;
        }
        
        #custom-modal-root .modal-input:focus {
          border-color: #3b82f6;
        }
        
        /* 暗色模式 */
        @media (prefers-color-scheme: dark) {
          #custom-modal-root .modal-content {
            background: #1f2937 !important;
          }
          #custom-modal-root .modal-title {
            color: #f9fafb !important;
          }
          #custom-modal-root .modal-message {
            color: #9ca3af !important;
          }
          #custom-modal-root .modal-btn-cancel {
            background: #374151;
            color: #d1d5db;
          }
          #custom-modal-root .modal-btn-cancel:hover {
            background: #4b5563;
          }
          #custom-modal-root .modal-input {
            background: #374151;
            border-color: #4b5563;
            color: #f9fafb;
          }
        }
        
        @keyframes modalFadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        
        @keyframes modalSlideIn {
          from {
            opacity: 0;
            transform: scale(0.95) translateY(10px);
          }
          to {
            opacity: 1;
            transform: scale(1) translateY(0);
          }
        }
        
        /* 暗色模式媒体查询 */
        @media (prefers-color-scheme: dark) {
          #custom-modal-root {
            --modal-bg: #1f2937;
            --modal-title-color: #f9fafb;
            --modal-text-color: #9ca3af;
          }
        }
      </style>
    `
    document.body.appendChild(modalRoot)
  }
  return modalRoot
}

// 显示模态框
const showModal = (options) => {
  return new Promise((resolve) => {
    const root = getModalRoot()
    const id = `modal-${++modalId}`
    
    const {
      type = 'confirm', // confirm | alert | prompt
      title = '',
      message = '',
      confirmText = '确定',
      cancelText = '取消',
      confirmClass = '', // '' | 'success'
      icon = '', // '' | 'success' | 'error' | 'warning' | 'info'
      defaultValue = '',
      inputPlaceholder = ''
    } = options

    // 图标 SVG
    const icons = {
      success: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>',
      error: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>',
      warning: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>',
      info: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>'
    }

    // 按钮HTML
    let buttonsHtml = ''
    if (type === 'confirm') {
      buttonsHtml = `
        <button class="modal-btn modal-btn-cancel" data-action="cancel">${cancelText}</button>
        <button class="modal-btn modal-btn-confirm ${confirmClass}" data-action="confirm">${confirmText}</button>
      `
    } else if (type === 'alert') {
      buttonsHtml = `
        <button class="modal-btn modal-btn-confirm" style="background: linear-gradient-to-r from-blue-500 to-cyan-500); box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);" data-action="confirm">${confirmText}</button>
      `
    } else if (type === 'prompt') {
      buttonsHtml = `
        <button class="modal-btn modal-btn-cancel" data-action="cancel">${cancelText}</button>
        <button class="modal-btn modal-btn-confirm" data-action="confirm">${confirmText}</button>
      `
    }

    // 图标HTML
    const iconHtml = icon && icons[icon] 
      ? `<div class="modal-icon ${icon}">${icons[icon]}</div>` 
      : ''

    // 输入框HTML
    const inputHtml = type === 'prompt' 
      ? `<input type="text" class="modal-input" value="${defaultValue}" placeholder="${inputPlaceholder}" data-action="input" />` 
      : ''

    const overlay = document.createElement('div')
    overlay.className = 'modal-overlay'
    overlay.id = id
    overlay.innerHTML = `
      <div class="modal-content">
        ${iconHtml}
        ${title ? `<div class="modal-title">${title}</div>` : ''}
        ${message ? `<div class="modal-message">${message}</div>` : ''}
        ${inputHtml}
        <div class="modal-buttons">${buttonsHtml}</div>
      </div>
    `

    // 点击事件处理
    const handleClick = (e) => {
      const action = e.target.dataset.action
      
      if (action === 'confirm') {
        const input = overlay.querySelector('[data-action="input"]')
        resolve(type === 'prompt' ? (input?.value || '') : true)
        close()
      } else if (action === 'cancel') {
        resolve(false)
        close()
      }
    }

    // ESC 键关闭
    const handleKeydown = (e) => {
      if (e.key === 'Escape') {
        resolve(type === 'confirm' ? false : (type === 'prompt' ? null : undefined))
        close()
      } else if (e.key === 'Enter' && type !== 'prompt') {
        resolve(true)
        close()
      }
    }

    const close = () => {
      overlay.style.animation = 'modalFadeIn 0.15s ease-out reverse'
      overlay.querySelector('.modal-content').style.animation = 'modalSlideIn 0.15s ease-out reverse'
      setTimeout(() => {
        overlay.remove()
        overlay.removeEventListener('click', handleClick)
        document.removeEventListener('keydown', handleKeydown)
      }, 150)
    }

    overlay.addEventListener('click', (e) => {
      // 点击遮罩层关闭（仅 confirm 类型）
      if (e.target === overlay && type === 'confirm') {
        resolve(false)
        close()
      }
    })
    
    overlay.addEventListener('click', handleClick)
    document.addEventListener('keydown', handleKeydown)
    
    root.appendChild(overlay)
  })
}

// 对外 API
export const modal = {
  /**
   * 确认对话框
   * @param {string} message - 提示消息
   * @param {object} options - 配置选项
   * @returns {Promise<boolean>}
   */
  confirm: (message, options = {}) => {
    return showModal({
      type: 'confirm',
      message,
      icon: options.icon || 'warning',
      title: options.title || '确认操作',
      confirmText: options.confirmText || '确定',
      cancelText: options.cancelText || '取消',
      confirmClass: options.confirmClass || '',
      ...options
    })
  },

  /**
   * 警告提示框
   * @param {string} message - 提示消息
   * @param {string} title - 标题
   * @returns {Promise<boolean>}
   */
  alert: (message, title = '提示') => {
    return showModal({
      type: 'alert',
      message,
      title,
      icon: 'info',
      confirmText: '我知道了'
    })
  },

  /**
   * 成功提示框
   * @param {string} message - 提示消息
   * @param {string} title - 标题
   * @returns {Promise<boolean>}
   */
  success: (message, title = '成功') => {
    return showModal({
      type: 'alert',
      message,
      title,
      icon: 'success',
      confirmText: '好的'
    })
  },

  /**
   * 错误提示框
   * @param {string} message - 提示消息
   * @param {string} title - 标题
   * @returns {Promise<boolean>}
   */
  error: (message, title = '出错了') => {
    return showModal({
      type: 'alert',
      message,
      title,
      icon: 'error',
      confirmText: '知道了'
    })
  },

  /**
   * 输入对话框
   * @param {string} message - 提示消息
   * @param {string} defaultValue - 默认值
   * @returns {Promise<string|false>} - 返回输入的值或 false（取消）
   */
  prompt: (message, defaultValue = '', placeholder = '') => {
    return showModal({
      type: 'prompt',
      message,
      defaultValue,
      inputPlaceholder: placeholder,
      icon: 'info',
      title: '请输入',
      confirmText: '确定',
      cancelText: '取消'
    })
  },

  /**
   * 危险操作确认（红色强调）
   * @param {string} message - 提示消息
   * @param {string} title - 标题
   * @returns {Promise<boolean>}
   */
  danger: (message, title = '危险操作') => {
    return showModal({
      type: 'confirm',
      message,
      title,
      icon: 'error',
      confirmText: '确认删除',
      cancelText: '取消',
      confirmClass: ''
    })
  }
}

export default modal
