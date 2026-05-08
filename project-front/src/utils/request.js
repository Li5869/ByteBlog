/**
 * API 请求封装
 * 
 * 基于 fetch 封装，提供统一的请求和响应处理
 * 自动携带 Token，处理认证失败等场景
 * 支持 Refresh Token 自动刷新机制
 */

import JSONbig from 'json-bigint'

// 配置 json-bigint：大数转为字符串，避免雪花ID精度丢失
const JSONbigString = JSONbig({ storeAsString: true })

const BASE_URL = '/api'

// 标记是否正在刷新 Token，防止并发刷新
let isRefreshing = false
// 存储等待刷新完成的请求队列
let refreshSubscribers = []

/**
 * 订阅刷新完成事件
 * 当 Token 刷新完成后，重新执行等待的请求
 */
const subscribeTokenRefresh = (callback) => {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有订阅者刷新完成
 * 刷新成功后，重新执行所有等待的请求
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
  window.location.href = '/'
}

/**
 * 获取存储的 Access Token
 */
export const getToken = () => {
  return localStorage.getItem('token')
}

/**
 * 获取存储的 Refresh Token
 */
export const getRefreshToken = () => {
  return localStorage.getItem('refreshToken')
}

/**
 * 获取存储的用户信息
 */
export const getUserInfo = () => {
  const userInfo = localStorage.getItem('userInfo')
  return userInfo ? JSON.parse(userInfo) : null
}

/**
 * 保存认证信息（包含 Access Token 和 Refresh Token）
 */
export const saveAuth = (token, userInfo, refreshToken = null) => {
  localStorage.setItem('token', token)
  localStorage.setItem('userInfo', JSON.stringify(userInfo))
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken)
  }
}

/**
 * 保存 Refresh Token
 */
export const saveRefreshToken = (refreshToken) => {
  localStorage.setItem('refreshToken', refreshToken)
}

/**
 * 清除认证信息
 */
export const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
}

/**
 * 检查是否已登录
 */
export const isLoggedIn = () => {
  return !!getToken()
}

/**
 * 刷新 Access Token
 * 使用 Refresh Token 获取新的 Access Token
 */
const refreshAccessToken = async () => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }

  try {
    const response = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    })
    const text = await response.text()
    const data = JSONbigString.parse(text)

    if (data.code !== 0) {
      // 刷新失败，清除认证信息
      console.error('Token 刷新失败:', data.msg)
      return null
    }

    // 更新本地存储的 Token
    const { accessToken, refreshToken: newRefreshToken } = data.data
    localStorage.setItem('token', accessToken)
    localStorage.setItem('refreshToken', newRefreshToken)

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
    const text = await response.text()
    const data = JSONbigString.parse(text)
    
    // 处理 401 未授权错误
    if (response.status === 401) {
      // 如果是刷新接口本身返回 401，说明 Refresh Token 也过期了
      if (url === '/auth/refresh') {
        clearAuth()
        window.location.href = '/'
        return Promise.reject(new Error('登录状态已过期，请重新登录'))
      }

      // 尝试使用 Refresh Token 刷新
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        // 没有 Refresh Token，直接跳转登录
        clearAuth()
        window.location.href = '/'
        return Promise.reject(new Error('未登录或登录已过期'))
      }

      // 如果正在刷新，将当前请求加入队列等待
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken) => {
            // 使用新 Token 重新发起请求
            config.headers['token'] = newToken
            fetch(`${BASE_URL}${url}`, config)
              .then(res => res.text())
              .then(text => {
                const retryData = JSONbigString.parse(text)
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

      // 开始刷新 Token
      isRefreshing = true
      const newToken = await refreshAccessToken()
      isRefreshing = false

      if (newToken) {
        // 刷新成功，更新请求头并重新发起请求
        config.headers['token'] = newToken
        // 通知等待队列中的请求
        onRefreshed(newToken)
        
        // 重新发起当前请求
        const retryResponse = await fetch(`${BASE_URL}${url}`, config)
        const retryText = await retryResponse.text()
        const retryData = JSONbigString.parse(retryText)
        
        if (retryData.code !== 0) {
          return Promise.reject(new Error(retryData.msg || '请求失败'))
        }
        
        return retryData.data
      } else {
        // 刷新失败，清除认证信息并跳转登录
        onRefreshFailed()
        return Promise.reject(new Error('登录状态已过期，请重新登录'))
      }
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
export const del = (url, data) => {
  const config = { method: 'DELETE' }
  if (data) {
    config.body = JSON.stringify(data)
  }
  return request(url, config)
}

/**
 * 认证相关 API
 */
export const authApi = {
  login: (data) => post('/auth/login', data),
  register: (data) => post('/auth/register', data),
  logout: () => post('/auth/logout'),
  getUserInfo: () => get('/auth/info'),
  refresh: (refreshToken) => post('/auth/refresh', { refreshToken }),
}

/**
 * 用户相关 API
 */
export const userApi = {
  getProfileStats: () => get('/user/me'),
  getProfile: () => get('/user/me'),
  updateProfile: (data) => put('/user/profile', data),
  getActiveUsers: (size) => get('/user/active', size ? { size } : null),
  getAuthorInfo: (id) => get(`/user/users/${id}`),
}

/**
 * 文章相关 API
 */
export const articleApi = {
  getBanners: (size) => get('/article/banners', size ? { size } : null),
  getArticlePage: (data) => post('/article/articles', data),
  getArticleDetail: (id) => get(`/article/articles/${id}`),
  /** 获取文章基础信息（含正文、分类、标签，可缓存） */
  getArticleMetadata: (id) => get(`/article/articles/${id}`),
  /** 获取文章互动数据（浏览量、点赞、收藏、评论，实时） */
  getArticleInteraction: (id) => get(`/article/articles/${id}/interaction`),
  getEditArticle: (id) => get(`/article/articles/${id}/edit`),
  createArticle: (data) => post('/article/articles/publish', data),
  updateArticle: (id, data) => put(`/article/articles/${id}`, data),
  deleteArticle: (id) => del(`/article/articles/${id}`),
  getRandomArticles: (size) => get('/article/articles/random', size ? { size } : null),
  getHotArticles: (size) => get('/article/articles/hot', size ? { size } : null),
  getRelatedArticles: (id, limit) => get(`/article/articles/${id}/related`, limit ? { limit } : null),
  getHotTags: (size) => get('/article/tags/hot', size ? { size } : null),
  getMyArticles: (current = 1, size = 10, status = null, orderBy = 'created_at') => {
    const params = { current, size, orderBy }
    if (status !== null) params.status = status
    return get('/article/my-articles', params)
  },
}

/**
 * 分类相关 API
 */
export const categoryApi = {
  getCategories: () => get('/category'),
}

/**
 * 标签相关 API
 */
export const tagApi = {
  getTags: () => get('/tag'),
  /**
   * 批量获取标签
   * @param {string} ids - 逗号分隔的标签ID，如 "1,2,3"
   * @returns {Promise<Array>} 标签列表
   */
  batchGetTags: (ids) => get('/tag/batch', { ids }),
}

/**
 * 评论相关 API
 */
export const commentApi = {
  getCommentPage: (articleId, current, size) => get(`/comment/comments/article/${articleId}`, { current, size }),
  postComment: (data) => post('/comment/comments', data),
  deleteComment: (id) => del(`/comment/comments/${id}`),
  getIsLikes: (targetIds, targetType) => get('/comment/comments/isLikes', { targetIds: targetIds.join(','), targetType }),
}

/**
 * 互动相关 API
 */
export const interactionApi = {
  toggleLike: (targetId, targetType, isLike, authorId, targetTitle = '', targetContent = '', relatedId = null) => post('/interaction/likes/toggle', { targetId, targetType, isLike, authorId, targetTitle, targetContent, relatedId }),
  toggleCollect: (articleId, isCollection, articleAuthorId) => post('/interaction/collections/toggle', { articleId, isCollection, articleAuthorId }),
  toggleFollow: (followingId, isFollow) => post('/interaction/follows/toggle', { followingId, isFollow }),
  getFollowingUsers: () => get('/interaction/follows/following'),
  checkBatchFollowStatus: (followingIds) => post('/interaction/follows/check-batch', followingIds),
  getBrowseHistory: (current = 1, size = 10) => get(`/interaction/browse-history?current=${current}&size=${size}`),
  getMyCollections: (current = 1, size = 10) => get('/interaction/my-collections', { current, size }),
  getMyLikes: (current = 1, size = 10) => get('/interaction/my-likes', { current, size }),
  getUserCollections: (userId, current = 1, size = 10) => get(`/interaction/collections/users/${userId}/collections`, { current, size }),

  // ========== 通知相关 ==========
  /** 获取通知列表（旧版，兼容） */
  getNotifications: (params) => get('/interaction/notifications', params),
  /** 标记通知已读（旧版，兼容） */
  markNotificationRead: (id) => put(`/interaction/notifications/${id}/read`),
  /** 标记全部已读（旧版，兼容） */
  markAllNotificationsRead: () => put('/interaction/notifications/read-all'),
  /** 删除通知 */
  deleteNotification: (id) => del(`/interaction/notifications/${id}`),
  /** 获取未读通知数 */
  getUnreadCount: () => get('/interaction/notifications/unread-count'),

  // ========== 新版通知 API（基于新表结构） ==========
  /** 获取业务通知列表 */
  getBizNotifications: (params) => post('/interaction/notifications/biz', params),
  /** 标记业务通知已读 */
  markBizNotificationRead: (id) => put(`/interaction/notifications/biz/${id}/read`),
  /** 删除业务通知 */
  deleteBizNotification: (id) => del(`/interaction/notifications/biz/${id}`),
  /** 批量删除业务通知 */
  batchDeleteBizNotifications: (ids) => del('/interaction/notifications/biz/batch', { ids }),
  /** 获取业务通知未读数 */
  getBizUnreadCount: () => get('/interaction/notifications/biz/unread-count'),
   /** 标记所有业务通知已读 */
  markAllBizNotificationsRead: () => put('/interaction/notifications/biz/read-all'),
  /** 获取系统通知列表 */
  /** 获取系统通知列表 */
  getSystemNotifications: (params) => post('/interaction/notifications/system', params),
  /** 标记系统通知已读 */
  markSystemNotificationRead: (id) => put(`/interaction/notifications/system/${id}/read`),
  /** 删除系统通知 */
  deleteSystemNotification: (id) => del(`/interaction/notifications/system/${id}`),
  /** 批量删除系统通知 */
  batchDeleteSystemNotifications: (ids) => del('/interaction/notifications/system/batch', { ids }),
  /** 获取系统通知未读数 */
  getSystemUnreadCount: () => get('/interaction/notifications/system/unread-count'),

  // ========== 私信相关 ==========
  /** 获取私信会话列表 */
  getConversations: () => get('/interaction/messages/conversations'),
  /** 获取与某用户的私信历史 */
  getMessageHistory: (userId, current = 1, size = 20) => get(`/interaction/messages/history/${userId}`, { current, size }),
  /** 发送私信 */
  sendMessage: (receiverId, content) => post('/interaction/messages', { receiverId, content }),
  /** 标记私信已读 */
  markMessagesRead: (userId) => put(`/interaction/messages/read/${userId}`),
  /** 标记所有私信已读 */
  markAllMessagesRead: () => put('/interaction/messages/read-all'),
  /** 删除会话 */
  deleteConversation: (id) => del(`/interaction/messages/conversations/${id}`),
  /** 批量删除会话 */
  batchDeleteConversations: (ids) => del('/interaction/messages/conversations/batch', { ids }),
  /** 获取私信未读数 */
  getPrivateUnreadCount: () => get('/interaction/messages/unread-count'),

  // ========== 一键清除所有未读 ==========
  /** 一键清除所有未读（业务通知 + 系统通知 + 私信） */
  clearAllUnread: () => put('/interaction/notifications/biz/read-all'),
}

/**
 * 问答相关 API
 */
export const questionApi = {
  getQuestionPage: (params) => get('/question/questions', params),
  getQuestionDetail: (id) => get(`/question/questions/${id}`),
  createQuestion: (data) => post('/question/questions', data),
  deleteQuestion: (id) => del(`/question/questions/${id}`),
  getAnswerList: (questionId, params) => get(`/question/questions/${questionId}/answers`, params),
  createAnswer: (questionId, data) => post(`/question/questions/${questionId}/answers`, data),
  deleteAnswer: (answerId) => del(`/question/answers/${answerId}`),
  getHotQuestions: (limit = 10) => get('/question/questions/hot', { limit }),
  /** 按标签获取相关问题（排除当前问题） */
  getRelatedQuestions: (tagId, excludeId, limit = 5) =>
    get('/question/questions', { tagId, size: limit, sortBy: 'newest' }),
  /** 获取我的问题列表 */
  getMyQuestions: (params) => get('/question/my/questions', params),
  /** 获取我的回答列表 */
  getMyAnswers: (params) => get('/question/my/answers', params),
  /** 采纳最佳答案 */
  acceptBestAnswer: (answerId) => post(`/question/answers/${answerId}/accept`),
}

/**
 * 上传相关 API
 */
export const uploadApi = {
  uploadFile: async (file) => {
    const token = getToken()
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await fetch(`${BASE_URL}/upload`, {
      method: 'POST',
      headers: {
        ...(token ? { 'token': token } : {})
      },
      body: formData
    })
    
    const text = await response.text()
    const data = JSONbigString.parse(text)
    
    if (response.status === 401) {
      clearAuth()
      window.location.href = '/'
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    
    if (data.code !== 0) {
      return Promise.reject(new Error(data.msg || '上传失败'))
    }
    
    return data.data
  }
}

/**
 * 搜索相关 API
 */
export const searchApi = {
  /**
   * 全局搜索
   * @param {Object} params - 搜索参数
   * @param {string} params.keyword - 搜索关键词
   * @param {string} params.type - 搜索类型：article-文章，question-问题，author-作者，column-专栏，all-全部
   * @param {number} params.categoryId - 分类ID（文章搜索）
   * @param {Array<number>} params.tagIds - 标签ID列表
   * @param {number} params.authorId - 作者ID
   * @param {string} params.orderBy - 排序：relevance-相关度，time-时间，views-浏览量
   * @param {number} params.current - 当前页码
   * @param {number} params.size - 每页大小
   */
  search: (params) => get('/search', params),
  
  /**
   * 同步文章索引
   */
  syncArticle: (articleId) => post(`/search/article/sync/${articleId}`),
  
  /**
   * 同步问题索引
   */
  syncQuestion: (questionId) => post(`/search/question/sync/${questionId}`),
  
  /**
   * 同步作者索引
   */
  syncAuthor: (authorId) => post(`/search/author/sync/${authorId}`),

  /**
   * 搜索建议（Completion Suggester）
   * @param {string} keyword - 搜索关键词
   * @param {number} size - 返回条数（默认9，每个类型3条）
   */
  suggest: (keyword, size = 9) => get('/search/suggest', { keyword, size }),
}

export const aiApi = {
  /**
   * AI 内容审核
   * @param {string} content - 待审核内容
   * @param {string} contentType - 内容类型: article / comment / question
   */
  moderateContent: (content, contentType = 'article') =>
    post('/ai/moderation/check', { content, contentType }),

  /**
   * AI 生成文章标题
   * @param {string} content - 文章内容（必填）
   * @param {number} maxLength - 标题最大长度，默认 30
   * @param {string} style - 风格：professional / casual / creative
   */
  generateTitle: (content, maxLength = 30, style = 'professional') =>
    post('/ai/article/title', { content, maxLength, style }),

  /**
   * AI 生成文章摘要
   * @param {string} content - 文章内容（必填）
   * @param {string} title - 文章标题（可选，辅助生成）
   * @param {number} maxLength - 摘要最大长度，默认 200
   */
  generateSummary: (content, title = '', maxLength = 200) =>
    post('/ai/article/summary', { content, title, maxLength }),

  /**
   * AI 文章润色
   * @param {string} content - 文章内容（必填）
   * @param {string} title - 文章标题（可选）
   * @param {string} style - 润色风格：professional / friendly / concise
   */
  polishArticle: (content, title = '', style = 'professional') =>
    post('/ai/article/polish', { content, title, style }),

  // ========== AI 聊天相关 ==========
  /**
   * 创建新会话
   */
  createConversation: () => post('/ai/chat/conversations'),

  /**
   * 获取会话列表
   * @param {number} current - 当前页码
   * @param {number} size - 每页大小
   */
  getConversationList: (current = 1, size = 20) =>
    get('/ai/chat/conversations', { current, size }),

  /**
   * 获取会话详情（包含历史消息）
   * @param {string} conversationId - 会话ID
   */
  getConversationDetail: (conversationId) =>
    get(`/ai/chat/conversations/${conversationId}`),

  /**
   * 删除会话
   * @param {string} conversationId - 会话ID
   */
  deleteConversation: (conversationId) =>
    del(`/ai/chat/conversations/${conversationId}`),

  /**
   * 停止对话（通知后端终止流式输出，保存已生成内容）
   * @param {string} sessionId - 会话ID（即 conversationId）
   */
  stopChat: (sessionId) => post(`/ai/chat/stop?sessionId=${sessionId}`, {}),

  /**
   * 发送消息（SSE 流式响应）
   * @param {string} conversationId - 会话ID
   * @param {string} content - 消息内容
   * @returns {EventSource} SSE 连接
   */
  sendMessageStream: (conversationId, content) => {
    const token = getToken()
    const url = `${BASE_URL}/ai/chat/messages`

    return new Promise((resolve, reject) => {
      const controller = new AbortController()
      const signal = controller.signal

      fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          ...(token ? { 'token': token } : {})
        },
        body: JSON.stringify({ conversationId, content }),
        signal
      }).then(response => {
        if (!response.ok) {
          reject(new Error('请求失败'))
          return
        }
        response._abortController = controller
        resolve(response.body)
      }).catch(err => {
        if (err.name === 'AbortError') {
          resolve(null)
        } else {
          reject(err)
        }
      })
    })
  },

  /**
   * 发送消息到 Agent（SSE 流式响应，调用 Python Agent）
   * @param {string} conversationId - 会话ID
   * @param {string} content - 消息内容
   * @returns {ReadableStream} SSE 流
   */
  sendAgentMessageStream: (conversationId, content) => {
    const token = getToken()
    const url = `${BASE_URL}/ai/chat/agent/stream`

    return new Promise((resolve, reject) => {
      const controller = new AbortController()
      const signal = controller.signal

      fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          ...(token ? { 'token': token } : {})
        },
        body: JSON.stringify({ conversationId, content }),
        signal
      }).then(response => {
        if (!response.ok) {
          reject(new Error('请求失败'))
          return
        }
        response._abortController = controller
        resolve(response.body)
      }).catch(err => {
        if (err.name === 'AbortError') {
          resolve(null)
        } else {
          reject(err)
        }
      })
    })
  },

  // ========== AI 写作智能体相关 ==========
  /**
   * 创建并启动写作任务（在数据库中创建记录并调用AI服务开始生成大纲）
   * @param {string} message - 写作需求
   * @returns {Promise<{taskId: string, status: string}>}
   */
  createWritingTask: (message) => 
    post('/ai/writing/create', { message }),

  /**
   * 恢复写作任务（批准或修改大纲）
   * @param {string} taskId - 任务ID
   * @param {string} action - 操作：approve-批准执行, revise-修改大纲
   * @param {string} feedback - 修改意见（仅当action=revise时需要）
   * @returns {Promise<{taskId: string, status: string}>}
   */
  resumeWriting: (taskId, action, feedback = null) => 
    post(`/ai/writing/${taskId}/resume`, { action, feedback }),

  /**
   * 获取写作任务状态
   * @param {string} taskId - 任务ID
   * @returns {Promise<{taskId: string, status: string}>}
   */
  getWritingStatus: (taskId) => 
    get(`/ai/writing/${taskId}/status`),

  /**
   * 获取我的写作任务列表
   * @returns {Promise<Array>} 写作任务列表
   */
  getMyWritingTasks: () =>
    get('/ai/writing/tasks'),

  /**
   * 获取写作任务详情（含计划数据）
   * @param {string} taskId - 任务ID
   * @returns {Promise<Object>} 任务详情（含 plan 数据）
   */
  getWritingTaskDetail: (taskId) =>
    get(`/ai/writing/${taskId}/detail`),

  /**
   * 删除写作任务
   * @param {string} taskId - 任务ID
   * @returns {Promise<void>}
   */
  deleteWritingTask: (taskId) =>
    del(`/ai/writing/${taskId}`),

  /**
   * 获取写作任务草稿
   * @param {string} taskId - 任务ID
   * @returns {Promise<Object>} 草稿数据
   */
  getWritingTaskDraft: (taskId) =>
    get(`/ai/writing/${taskId}/draft`),

  /**
   * 流式获取写作进度（SSE）
   * @param {string} taskId - 任务ID
   * @returns {Promise<ReadableStream>} SSE 流
   */
  streamWriting: (taskId) => {
    const token = getToken()
    const url = `${BASE_URL}/ai/writing/${taskId}/stream`

    return new Promise((resolve, reject) => {
      const controller = new AbortController()
      const signal = controller.signal

      fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'text/event-stream',
          ...(token ? { 'token': token } : {})
        },
        signal
      }).then(response => {
        if (!response.ok) {
          reject(new Error('请求失败'))
          return
        }
        response._abortController = controller
        resolve(response.body)
      }).catch(err => {
        if (err.name === 'AbortError') {
          resolve(null)
        } else {
          reject(err)
        }
      })
    })
  }
}

/**
 * 专栏相关 API
 */
export const columnApi = {
  /**
   * 分页获取专栏列表
   * @param {Object} params - 查询参数
   * @param {number} params.current - 当前页码
   * @param {number} params.size - 每页大小
   * @param {string} params.userId - 用户ID（查看指定用户的专栏）
   * @param {string} params.keyword - 搜索关键词
   * @param {string} params.orderBy - 排序字段
   */
  getColumnPage: (params) => post('/article/columns/list', params),
  
  /**
   * 获取专栏详情
   * @param {string} columnId - 专栏ID
   */
  getColumnDetail: (columnId) => get(`/article/columns/${columnId}`),
  
  /**
   * 获取我的专栏列表
   */
  getMyColumns: () => get('/article/columns/my'),
  
  /**
   * 创建专栏
   * @param {Object} data - 专栏数据
   * @param {string} data.title - 专栏标题
   * @param {string} data.description - 专栏描述
   * @param {string} data.cover - 封面图片URL
   * @param {number} data.status - 状态：0-草稿，1-发布
   */
  createColumn: (data) => post('/article/columns', data),
  
  /**
   * 更新专栏
   * @param {string} columnId - 专栏ID
   * @param {Object} data - 更新数据
   */
  updateColumn: (columnId, data) => put(`/article/columns/${columnId}`, data),
  
  /**
   * 删除专栏
   * @param {string} columnId - 专栏ID
   */
  deleteColumn: (columnId) => del(`/article/columns/${columnId}`),
  
  /**
   * 添加文章到专栏
   * @param {string} columnId - 专栏ID
   * @param {Array<string>} articleIds - 文章ID列表
   */
  addArticles: (columnId, articleIds) => post(`/article/columns/${columnId}/articles`, { articleIds }),
  
  /**
   * 从专栏移除文章
   * @param {string} columnId - 专栏ID
   * @param {Array<string>} articleIds - 文章ID列表
   */
  removeArticles: (columnId, articleIds) => del(`/article/columns/${columnId}/articles`, { articleIds }),
  
  /**
   * 更新专栏文章排序
   * @param {string} columnId - 专栏ID
   * @param {Array<Object>} articleSorts - 排序数据
   */
  updateArticleSort: (columnId, articleSorts) => put(`/article/columns/${columnId}/articles/sort`, { articleSorts }),
  
  /**
   * 获取可添加到专栏的文章
   * @param {string} columnId - 专栏ID
   */
  getAvailableArticles: (columnId) => get(`/article/columns/${columnId}/available-articles`),
  
  /**
   * 获取热门专栏列表
   * 根据订阅量排序，返回前5个
   */
  getHotColumns: () => get('/article/columns/hot'),
  
  /**
   * 订阅专栏
   * @param {string} columnId - 专栏ID
   */
  subscribeColumn: (columnId) => post(`/article/columns/${columnId}/subscribe`),
  
  /**
   * 取消订阅专栏
   * @param {string} columnId - 专栏ID
   */
  unsubscribeColumn: (columnId) => del(`/article/columns/${columnId}/subscribe`),
  
  /**
   * 检查是否已订阅专栏
   * @param {string} columnId - 专栏ID
   */
  checkSubscribed: (columnId) => get(`/article/columns/${columnId}/subscribe`),

  /**
   * 获取当前用户订阅的专栏列表
   * @returns {Promise<Array>} 订阅列表，包含 columnId, columnTitle, columnCover, authorName, articlesCount, subscribedAt
   */
  getSubscriptions: () => get('/article/columns/subscriptions')
}

export default {
  get,
  post,
  put,
  del,
  authApi,
  userApi,
  articleApi,
  categoryApi,
  tagApi,
  commentApi,
  interactionApi,
  questionApi,
  uploadApi,
  searchApi,
  aiApi,
  columnApi,
  getToken,
  getRefreshToken,
  getUserInfo,
  saveAuth,
  saveRefreshToken,
  clearAuth,
  isLoggedIn,
}
