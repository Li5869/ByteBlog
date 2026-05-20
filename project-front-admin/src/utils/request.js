/**
 * API 请求封装 - 管理端
 *
 * 基于 fetch 封装，提供统一的请求和响应处理
 * 自动携带 Token，处理认证失败等场景
 * 支持 Refresh Token 自动刷新机制
 * 使用 json-bigint 防止雪花 ID 精度丢失
 */

import JSONbig from 'json-bigint'

const JSONbigString = JSONbig({ storeAsString: true })

const safeParseJson = async (response) => {
  const text = await response.text()
  return JSONbigString.parse(text)
}

const BASE_URL = '/api'

// 标记是否正在刷新 Token，防止并发刷新
let isRefreshing = false
// 存储等待刷新完成的请求队列
let refreshSubscribers = []

/**
 * 订阅刷新完成事件
 */
const subscribeTokenRefresh = (callback) => {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有订阅者刷新完成
 */
const onRefreshed = (newToken) => {
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
}

/**
 * 刷新失败，清除所有订阅者
 */
const onRefreshFailed = () => {
  refreshSubscribers = []
  clearAuth()
  window.location.href = '/login'
}

/**
 * 获取存储的 Access Token
 */
const getToken = () => {
  return localStorage.getItem('admin-token')
}

/**
 * 获取存储的 Refresh Token
 */
export const getRefreshToken = () => {
  return localStorage.getItem('admin-refresh-token')
}

/**
 * 获取存储的用户信息
 */
export const getAdminInfo = () => {
  try {
    const info = localStorage.getItem('admin-info')
    return info ? JSONbigString.parse(info) : null
  } catch {
    localStorage.removeItem('admin-info')
    return null
  }
}

/**
 * 保存认证信息（包含 Access Token 和 Refresh Token）
 */
export const saveAuth = (token, adminInfo, refreshToken = null) => {
  localStorage.setItem('admin-token', token)
  localStorage.setItem('admin-info', JSON.stringify(adminInfo))
  if (refreshToken) {
    localStorage.setItem('admin-refresh-token', refreshToken)
  }
}

/**
 * 保存 Refresh Token
 */
export const saveRefreshToken = (refreshToken) => {
  localStorage.setItem('admin-refresh-token', refreshToken)
}

/**
 * 清除认证信息
 */
export const clearAuth = () => {
  localStorage.removeItem('admin-token')
  localStorage.removeItem('admin-refresh-token')
  localStorage.removeItem('admin-info')
}

/**
 * 检查是否已登录
 */
export const isLoggedIn = () => {
  return !!getToken()
}

/**
 * 刷新 Access Token
 */
const refreshAccessToken = async () => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }

  try {
    const response = await fetch(`${BASE_URL}/admin/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    })
    const data = await safeParseJson(response)

    if (data.code !== 0) {
      console.error('Token 刷新失败:', data.msg)
      return null
    }

    // 更新本地存储的 Token
    const { accessToken, refreshToken: newRefreshToken } = data.data
    localStorage.setItem('admin-token', accessToken)
    localStorage.setItem('admin-refresh-token', newRefreshToken)

    return accessToken
  } catch (error) {
    console.error('Token 刷新请求失败:', error)
    return null
  }
}

/**
 * 统一请求方法
 * 支持 401 时自动刷新 Token
 */
const request = async (url, options = {}) => {
  const token = getToken()
  
  const defaultHeaders = {
    'Content-Type': 'application/json',
  }
  
  // 统一使用 "token" 请求头
  if (token) {
    defaultHeaders['token'] = token
  }
  
  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }
  
  try {
    const response = await fetch(`${BASE_URL}${url}`, config)
    const data = await safeParseJson(response)

    // 处理 401 未授权错误
    if (response.status === 401) {
      // 如果是刷新接口本身返回 401，说明 Refresh Token 也过期了
      if (url === '/admin/auth/refresh') {
        clearAuth()
        window.location.href = '/login'
        return Promise.reject(new Error('登录状态已过期，请重新登录'))
      }

      // 尝试使用 Refresh Token 刷新
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        // 没有 Refresh Token，直接跳转登录
        clearAuth()
        window.location.href = '/login'
        return Promise.reject(new Error('未登录或登录已过期'))
      }

      // 如果正在刷新，将当前请求加入队列等待
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken) => {
            // 使用新 Token 重新发起请求
            config.headers['token'] = newToken
            fetch(`${BASE_URL}${url}`, config)
              .then(res => safeParseJson(res))
              .then(retryData => {
                if (retryData.code !== 0) {
                  reject(new Error(retryData.msg || '请求失败'))
                } else {
                  resolve(retryData.data)
                }
              })
              .catch(reject)
          })
        })
      }

      // 开始刷新 Token，使用 try/finally 确保锁一定释放
      isRefreshing = true
      try {
        const newToken = await refreshAccessToken()

        if (newToken) {
          // 刷新成功，更新请求头并重新发起请求
          config.headers['token'] = newToken
          // 通知等待队列中的请求
          onRefreshed(newToken)

          // 重新发起当前请求
          const retryResponse = await fetch(`${BASE_URL}${url}`, config)
          const retryData = await safeParseJson(retryResponse)

          if (retryData.code !== 0) {
            return Promise.reject(new Error(retryData.msg || '请求失败'))
          }

          return retryData.data
        } else {
          // 刷新失败，清除认证信息并跳转登录
          onRefreshFailed()
          return Promise.reject(new Error('登录状态已过期，请重新登录'))
        }
      } finally {
        isRefreshing = false
      }
    }
    
    if (response.status === 403) {
      return Promise.reject(new Error('无权限访问'))
    }
    
    if (data.code !== 0) {
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    
    return data.data
  } catch (error) {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
}

/**
 * GET 请求
 */
export const get = (url, params) => {
  let fullUrl = url
  if (params) {
    const queryString = new URLSearchParams(params).toString()
    fullUrl = `${url}?${queryString}`
  }
  return request(fullUrl, { method: 'GET' })
}

/**
 * POST 请求
 */
export const post = (url, data) => {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

/**
 * PUT 请求
 */
export const put = (url, data) => {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

/**
 * DELETE 请求
 */
export const del = (url) => {
  return request(url, { method: 'DELETE' })
}

/**
 * 认证相关 API（用户端，保留用于刷新Token等场景）
 */
export const authApi = {
  login: (data) => post('/auth/login', data),
  logout: () => post('/auth/logout'),
  getUserInfo: () => get('/auth/info'),
  refresh: (refreshToken) => post('/auth/refresh', { refreshToken }),
}

/**
 * 管理端认证 API（独立接口，仅管理员可用）
 */
export const adminAuthApi = {
  login: (data) => post('/admin/auth/login', data),
  logout: () => post('/admin/auth/logout'),
  getUserInfo: () => get('/admin/auth/info'),
  refresh: (refreshToken) => post('/admin/auth/refresh', { refreshToken }),
}

export const userApi = {
  getProfile: () => get('/user/me'),
  updateProfile: (data) => put('/user/profile', data),
}

export const uploadApi = {
  uploadFile: async (file) => {
    const token = getToken()
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await fetch(`${BASE_URL}/upload`, {
      method: 'POST',
      headers: {
        // 统一使用 "token" 请求头
        ...(token ? { 'token': token } : {})
      },
      body: formData
    })
    
    const data = await safeParseJson(response)

    if (response.status === 401) {
      clearAuth()
      window.location.href = '/login'
      return Promise.reject(new Error('未登录或登录已过期'))
    }

    if (data.code !== 0) {
      return Promise.reject(new Error(data.msg || '上传失败'))
    }

    return data.data
  }
}

export const updateAdminInfo = (newInfo) => {
  const adminInfo = getAdminInfo()
  if (adminInfo) {
    const updated = { ...adminInfo, ...newInfo }
    localStorage.setItem('admin-info', JSON.stringify(updated))
  }
}

/**
 * 专栏管理 API
 */
export const columnApi = {
  list: (params) => post('/article/columns/list', params),
  detail: (id) => get(`/article/columns/${id}`),
  create: (data) => post('/article/columns', data),
  update: (id, data) => put(`/article/columns/${id}`, data),
  delete: (id) => del(`/article/columns/${id}`),
  myColumns: () => get('/article/columns/my'),
  hotColumns: () => get('/article/columns/hot'),
}

/**
 * 管理端仪表盘 API
 */
export const dashboardApi = {
  getStatistics: () => get('/admin/dashboard/statistics'),
  getTrends: (year) => get('/admin/dashboard/trends', year ? { year } : undefined),
  getRecentArticles: (size = 5) => get('/admin/dashboard/recent-articles', { size }),
}

/**
 * 管理端文章管理 API
 */
export const adminArticleApi = {
  getPage: (params) => post('/admin/articles/list', params),
  getDetail: (id) => get(`/admin/articles/${id}`),
  delete: (id) => del(`/admin/articles/${id}`),
  approve: (id) => put(`/admin/articles/${id}/approve`),
  reject: (id, reason) => put(`/admin/articles/${id}/reject`, reason),
  toggleTop: (id, isTop) => put(`/admin/articles/${id}/top?isTop=${isTop}`),
}

/**
 * 管理端分类管理 API
 */
export const adminCategoryApi = {
  getList: () => get('/admin/categories'),
  create: (data) => post('/admin/categories', data),
  update: (id, data) => put(`/admin/categories/${id}`, data),
  delete: (id) => del(`/admin/categories/${id}`),
}

/**
 * 管理端标签管理 API
 */
export const adminTagApi = {
  getList: (params) => post('/admin/tags/list', params),
  create: (data) => post('/admin/tags', data),
  update: (id, data) => put(`/admin/tags/${id}`, data),
  delete: (id) => del(`/admin/tags/${id}`),
}

/**
 * 管理端用户管理 API
 */
export const adminUserApi = {
  getPage: (params) => post('/admin/users/list', params),
  getDetail: (id) => get(`/admin/users/${id}`),
  update: (id, data) => put(`/admin/users/${id}`, data),
  updateStatus: (id, data) => put(`/admin/users/${id}/status`, data),
  delete: (id) => del(`/admin/users/${id}`),
}

/**
 * 管理端评论管理 API
 */
export const adminCommentApi = {
  getPage: (params) => post('/admin/comments/list', params),
  getDetail: (id) => get(`/admin/comments/${id}`),
  approve: (id) => put(`/admin/comments/${id}/approve`),
  reject: (id) => put(`/admin/comments/${id}/reject`),
  delete: (id) => del(`/admin/comments/${id}`),
}

/**
 * 管理端问答管理 API
 */
export const adminQuestionApi = {
  getPage: (params) => post('/admin/questions/list', params),
  getDetail: (id) => get(`/admin/questions/${id}`),
  delete: (id) => del(`/admin/questions/${id}`),
  deleteAnswer: (id) => del(`/admin/answers/${id}`),
}

/**
 * 管理端操作日志 API
 */
export const adminLogApi = {
  getPage: (params) => post('/admin/logs/list', params),
}

/**
 * 知识库管理 API
 * 上传接口调用 Python AI 服务，其他接口调用 Java 后端
 */
export const knowledgeApi = {
  /**
   * 上传文件到知识库
   * @param {File} file - 文件对象
   * @param {string} description - 文件描述（可选）
   */
  uploadFile: async (file, description) => {
    const token = getToken()
    const formData = new FormData()
    formData.append('file', file)
    if (description) {
      formData.append('description', description)
    }

    const response = await fetch(`${BASE_URL}/ai/knowledge/file`, {
      method: 'POST',
      headers: {
        ...(token ? { 'token': token } : {})
      },
      body: formData
    })

    const data = await safeParseJson(response)

    if (response.status === 401) {
      clearAuth()
      window.location.href = '/login'
      return Promise.reject(new Error('未登录或登录已过期'))
    }

    if (data.code !== 0) {
      return Promise.reject(new Error(data.msg || '上传失败'))
    }

    return data.data
  },

  /**
   * 分页查询知识库文件列表
   * @param {Object} params - 查询参数 { current, size, keyword, uploaderId, source }
   */
  getList: (params) => get('/ai/knowledge/list', params),

  /**
   * 获取知识库文件详情
   * @param {number|string} fileId - 文件ID
   */
  getDetail: (fileId) => get(`/ai/knowledge/${fileId}`),

  /**
   * 更新知识库文件信息
   * @param {number|string} fileId - 文件ID
   * @param {Object} data - 更新数据 { fileName, description }
   */
  updateFile: (fileId, data) => put(`/ai/knowledge/${fileId}`, data),

  /**
   * 删除知识库文件（级联删除）
   * @param {number|string} fileId - 文件ID
   */
  deleteFile: (fileId) => del(`/ai/knowledge/${fileId}`),

  /**
   * 批量删除知识库文件
   * @param {Array} fileIds - 文件ID列表
   */
  batchDelete: (fileIds) => post('/ai/knowledge/batch-delete', { fileIds })
}

export default {
  get,
  post,
  put,
  del,
  authApi,
  adminAuthApi,
  userApi,
  uploadApi,
  columnApi,
  dashboardApi,
  adminArticleApi,
  adminCategoryApi,
  adminTagApi,
  adminUserApi,
  adminCommentApi,
  adminQuestionApi,
  adminLogApi,
  knowledgeApi,
  getToken,
  getRefreshToken,
  getAdminInfo,
  saveAuth,
  saveRefreshToken,
  clearAuth,
  isLoggedIn,
  updateAdminInfo,
}
