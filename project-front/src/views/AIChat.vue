<script setup>
import {nextTick, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {marked} from 'marked'
import {NButton, NEmpty, NPagination, NSpin} from 'naive-ui'
import {aiApi, isLoggedIn, userApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'
import {useUserStore} from '@/stores/user'
import {AI_USER_ID, DEFAULT_AVATAR} from '@/utils/defaults'
import {formatRelativeTime} from '@/utils/format'
import WritingProgressPanel from '@/components/WritingProgressPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ==================== 响应式数据 ====================
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const isStreaming = ref(false)
const conversationsLoading = ref(false)
const messagesLoading = ref(false)
const expandedThinking = ref({})
const messagesContainer = ref(null)
const currentReader = ref(null)
const currentAbortController = ref(null)
const sidebarCollapsed = ref(false)

// ==================== 写作进度面板 ====================
const showWritingProgress = ref(false)
const writingTaskId = ref('')
const writingProgressRef = ref(null)

/** AI 助手头像 */
const aiAssistantAvatar = ref('')

/** 获取 AI 助手用户信息 */
const fetchAiAssistantInfo = async () => {
  try {
    const data = await userApi.getAuthorInfo(AI_USER_ID)
    aiAssistantAvatar.value = data.avatar || ''
  } catch (error) {
    console.error('获取AI助手信息失败:', error)
  }
}

// 分页配置
const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

// 会话列表滚动容器引用
const conversationsListRef = ref(null)

// ==================== 工具方法 ====================

// 渲染 Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  const html = marked(content)
  return html.replace(/<pre>/g, '<pre class="code-block">')
}

// 代码块复制
const copyCode = (event) => {
  const btn = event.currentTarget
  const pre = btn.closest('.code-block-wrapper')
  if (!pre) return
  const code = pre.querySelector('code')
  if (!code) return
  navigator.clipboard.writeText(code.textContent).then(() => {
    btn.textContent = '已复制'
    btn.classList.add('copied')
    setTimeout(() => {
      btn.textContent = '复制'
      btn.classList.remove('copied')
    }, 2000)
  })
}

// 添加复制按钮
const addCopyButtons = () => {
  nextTick(() => {
    const container = messagesContainer.value
    if (!container) return
    container.querySelectorAll('.code-block:not(.processed)').forEach(pre => {
      pre.classList.add('processed')
      const wrapper = document.createElement('div')
      wrapper.className = 'code-block-wrapper'
      pre.parentNode.insertBefore(wrapper, pre)
      wrapper.appendChild(pre)
      const btn = document.createElement('button')
      btn.className = 'code-copy-btn'
      btn.textContent = '复制'
      btn.addEventListener('click', copyCode)
      wrapper.appendChild(btn)
    })
  })
}

// 格式化完整时间
const formatFullTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// ==================== 会话管理 ====================

// 获取会话列表
const fetchConversations = async () => {
  if (!isLoggedIn()) return
  
  conversationsLoading.value = true
  try {
    const data = await aiApi.getConversationList(pagination.value.current, pagination.value.size)
    conversations.value = data.records || []
    pagination.value.total = data.total || 0
  } catch (error) {
    console.error('获取会话列表失败:', error)
    toast.error('获取会话列表失败')
  } finally {
    conversationsLoading.value = false
  }
}

// 分页切换处理
const handlePageChange = async (page) => {
  pagination.value.current = page
  await fetchConversations()
  // 滚动到列表顶部
  if (conversationsListRef.value) {
    conversationsListRef.value.scrollTop = 0
  }
}

// 获取会话详情
const fetchConversationDetail = async (conversationId) => {
  if (!conversationId) return
  
  messagesLoading.value = true
  try {
    const data = await aiApi.getConversationDetail(conversationId)
    currentConversation.value = data
    messages.value = data.messages || []
    scrollToBottom()
    addCopyButtons()
  } catch (error) {
    console.error('获取会话详情失败:', error)
    toast.error('获取会话详情失败')
  } finally {
    messagesLoading.value = false
  }
}

// 创建新会话
const createNewConversation = async (initialMessage) => {
  if (!isLoggedIn()) {
    toast.warning('请先登录后再使用AI助手')
    return
  }

  // 始终通过后端创建/获取对话，确保后端能清除残留的 Redis 记忆，防止记忆串对话
  let targetConv = null
  try {
    const conversationId = await aiApi.createConversation()
    const existing = conversations.value.find(c => String(c.id) === String(conversationId))
    if (existing) {
      targetConv = existing
    } else {
      targetConv = {
        id: String(conversationId),
        title: '新对话',
        messageCount: 0,
        createdAt: new Date().toISOString()
      }
      conversations.value.unshift(targetConv)
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    toast.error('创建会话失败')
    return
  }

  currentConversation.value = targetConv
  await fetchConversationDetail(targetConv.id)
  syncCurrentConversationToList()

  if (initialMessage && currentConversation.value) {
    inputMessage.value = initialMessage
    await nextTick()
    await sendMessage()
  }
}

// 同步会话列表
const syncCurrentConversationToList = () => {
  if (!currentConversation.value) return
  const currentId = String(currentConversation.value.id)
  const index = conversations.value.findIndex(c => String(c.id) === currentId)
  if (index !== -1) {
    const existing = conversations.value[index]
    const lastMsg = messages.value.length > 0
      ? messages.value[messages.value.length - 1]?.content?.substring(0, 50) || ''
      : existing.lastMessage || ''
    conversations.value[index] = {
      id: existing.id,
      title: currentConversation.value.title || existing.title,
      messageCount: currentConversation.value.messageCount ?? existing.messageCount,
      lastMessage: lastMsg,
      createdAt: existing.createdAt,
      updatedAt: new Date().toISOString()
    }
  }
}

// 选择会话
const selectConversation = async (conversation) => {
  const targetId = String(conversation.id)
  
  if (currentConversation.value && String(currentConversation.value.id) === targetId) {
    return
  }
  
  // 切换对话时关闭写作进度面板
  closeWritingProgress()
  writingTaskId.value = ''
  
  messages.value = []
  messagesLoading.value = true
  currentConversation.value = conversation
  
  await nextTick()
  await fetchConversationDetail(conversation.id)
  syncCurrentConversationToList()
}

// 删除会话
const deleteConversation = async (conversation, event) => {
  event.stopPropagation()
  
  const confirmed = await modal.confirm('确定要删除这个对话吗？删除后无法恢复。', {
    title: '删除对话',
    confirmText: '删除',
    icon: 'error'
  })
  
  if (!confirmed) return
  
  try {
    await aiApi.deleteConversation(conversation.id)
    const targetId = String(conversation.id)
    conversations.value = conversations.value.filter(c => String(c.id) !== targetId)
    if (currentConversation.value && String(currentConversation.value.id) === targetId) {
      currentConversation.value = null
      messages.value = []
    }
    toast.success('删除成功')
  } catch (error) {
    console.error('删除会话失败:', error)
    toast.error('删除失败')
  }
}

// ==================== 消息发送 ====================

// 发送消息
const sendMessage = async () => {
  const content = String(inputMessage.value ?? '').trim()
  if (!content || isStreaming.value) return
  
  if (!currentConversation.value) {
    toast.warning('请先选择或创建一个对话')
    return
  }
  
  const userMessage = {
    id: Date.now().toString(),
    role: 'user',
    content: content,
    createdAt: new Date().toISOString()
  }
  
  messages.value.push(userMessage)
  inputMessage.value = ''
  scrollToBottom()
  
  isStreaming.value = true
  isLoading.value = true
  
  messages.value.push({
    id: (Date.now() + 1).toString(),
    role: 'assistant',
    content: '',
    thinking: '',
    createdAt: new Date().toISOString()
  })
  
  const assistantIdx = messages.value.length - 1
  let reader = null
  
  try {
    const result = await aiApi.sendAgentMessageStream(
      currentConversation.value.id,
      content
    )

    if (!result || !result.body) {
      throw new Error('响应失败')
    }

    currentAbortController.value = result.controller
    reader = result.body.getReader()
    currentReader.value = reader
    const decoder = new TextDecoder()
    let buffer = ''
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      
      for (const part of parts) {
        if (!part.trim()) continue
        
        const lines = part.split('\n')
        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine) continue
          
          if (trimmedLine.startsWith('data:')) {
            const jsonStr = trimmedLine.substring(5).trim()
            if (!jsonStr) continue
            
            try {
              const event = JSON.parse(jsonStr)
              
              if (event.type === 'chunk') {
                if (event.data && typeof event.data === 'string' && event.data.length > 0) {
                  messages.value[assistantIdx].content += event.data
                  await nextTick()
                  scrollToBottom()
                  addCopyButtons()
                }
              } else if (event.type === 'done') {
                break
              } else if (event.type === 'error') {
                if (event.data && typeof event.data === 'string') {
                  messages.value[assistantIdx].content = event.data
                }
                break
              } else if (event.type === 'thinking' || event.type === 'tool_call') {
                if (event.data && typeof event.data === 'string') {
                  // 检测写作进度触发标记（tool_call 结果中包含）
                  const triggerMatch = event.data.match(/<!-- WRITING_TRIGGER: (\{.*\}) -->/)
                  if (triggerMatch) {
                    try {
                      const trigger = JSON.parse(triggerMatch[1])
                      writingTaskId.value = trigger.task_id
                      showWritingProgress.value = true
                      if (trigger.action === 'start_execute') {
                        await nextTick()
                        writingProgressRef.value?.startExecutePhase()
                      }
                      console.log('[WritingProgress] 检测到触发标记:', trigger)
                    } catch (e) {
                      console.warn('[WritingProgress] 解析触发标记失败:', e)
                    }
                  } else {
                    messages.value[assistantIdx].thinking += event.data
                    expandedThinking.value[messages.value[assistantIdx].id] = true
                    await nextTick()
                    scrollToBottom()
                  }
                }
              }
            } catch (e) {
              console.warn('解析SSE数据失败:', e, jsonStr)
            }
          }
        }
      }
    }
    
    if (currentConversation.value.messageCount !== undefined) {
      currentConversation.value.messageCount += 2
    }
    const isFirstMessage = !currentConversation.value.title || currentConversation.value.title === '新对话'
    if (isFirstMessage) {
      currentConversation.value.title = content.substring(0, 20) + (content.length > 20 ? '...' : '')
    }
    syncCurrentConversationToList()
    // 第一次对话后仅刷新当前对话的列表项，获取 MQ 异步更新的对话标题
    if (isFirstMessage) {
      try {
        const data = await aiApi.getConversationList(pagination.value.current, pagination.value.size)
        const updated = (data.records || []).find(c => String(c.id) === String(currentConversation.value.id))
        if (updated) {
          const idx = conversations.value.findIndex(c => String(c.id) === String(updated.id))
          if (idx !== -1) {
            conversations.value[idx] = { ...conversations.value[idx], ...updated }
          }
        }
      } catch (error) {
        console.warn('刷新当前对话标题失败:', error)
      }
    }
    
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('发送消息失败:', error)
      messages.value[assistantIdx].content = '抱歉，发生了错误，请稍后重试。'
      toast.error('发送消息失败')
    }
  } finally {
    currentReader.value = null
    currentAbortController.value = null
    isStreaming.value = false
    isLoading.value = false
  }
}

// 停止流式输出
const stopStreaming = async () => {
  if (!currentConversation.value) return

  try {
    await aiApi.stopChat(String(currentConversation.value.id))
  } catch (e) {
    console.warn('停止对话接口调用失败:', e)
  }

  // 使用 AbortController 终止底层 HTTP 连接，确保服务端停止生成
  if (currentAbortController.value) {
    try {
      currentAbortController.value.abort()
    } catch (e) {
      // ignore
    }
    currentAbortController.value = null
  }

  if (currentReader.value) {
    try {
      currentReader.value.cancel()
    } catch (e) {
      // ignore
    }
    currentReader.value = null
  }

  isStreaming.value = false
  isLoading.value = false
}

// 键盘事件处理
const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 切换思考过程展开
const toggleThinking = (messageId) => {
  expandedThinking.value[messageId] = !expandedThinking.value[messageId]
}

// 判断会话是否活跃
const isActiveConversation = (conversation) => {
  return currentConversation.value && String(currentConversation.value.id) === String(conversation.id)
}

// ==================== 写作进度面板 ====================

const closeWritingProgress = () => {
  showWritingProgress.value = false
}

const viewWritingResult = (taskId) => {
  router.push(`/writing/${taskId}/result`)
  closeWritingProgress()
}

// ==================== 生命周期 ====================

onMounted(async () => {
  document.title = 'AI智能助手 - ByteBlog'
  fetchAiAssistantInfo()
  if (isLoggedIn()) {
    await fetchConversations()
    const searchQuery = route.query.q
    if (searchQuery && typeof searchQuery === 'string' && searchQuery.trim()) {
      await createNewConversation(`帮我搜索${searchQuery.trim()}相关内容`)
    }
  }
})
</script>

<template>
  <div class="ai-chat-page">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- 侧边栏头部 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <span v-if="!sidebarCollapsed" class="logo-text">AI 助手</span>
        </div>
        <button class="new-chat-btn" @click="createNewConversation()">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          <span v-if="!sidebarCollapsed">新建对话</span>
        </button>
      </div>

      <!-- 会话列表 -->
      <div v-if="!sidebarCollapsed" ref="conversationsListRef" class="conversations-list">
        <div v-if="conversationsLoading" class="loading-state">
          <n-spin size="medium" />
        </div>
        <template v-else>
          <div
            v-for="conversation in conversations"
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: isActiveConversation(conversation) }"
            @click="selectConversation(conversation)"
          >
            <div class="conversation-icon">
              <svg fill="none" stroke="currentColor" viewBox="0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div class="conversation-info">
              <div class="conversation-title">{{ conversation.title || '新对话' }}</div>
              <div class="conversation-meta">
                <span>{{ conversation.messageCount || 0 }} 条消息</span>
                <span>·</span>
                <span>{{ formatRelativeTime(conversation.updatedAt || conversation.createdAt) }}</span>
              </div>
            </div>
            <button class="delete-btn" @click="deleteConversation(conversation, $event)">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
          <n-empty v-if="conversations.length === 0" description="暂无对话" />
          <!-- 分页组件 -->
          <div v-if="pagination.total > pagination.size" class="pagination-wrapper">
            <n-pagination
              v-model:page="pagination.current"
              :page-count="Math.ceil(pagination.total / pagination.size)"
              :page-slot="5"
              size="small"
              @update:page="handlePageChange"
            />
          </div>
        </template>
      </div>

      <!-- 折叠按钮 -->
      <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
        <svg :class="{ rotated: sidebarCollapsed }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
        </svg>
      </button>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 未选择会话 -->
      <div v-if="!currentConversation" class="welcome-screen">
        <div class="welcome-content">
          <div class="welcome-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <h2>有什么我可以帮助你的？</h2>
          <p>我是你的智能助手，可以搜索博客文章、解答技术问题、帮你写作和编程</p>
          
          <div class="quick-actions">
            <button @click="createNewConversation('帮我查找关于Spring Boot的文章')">
              <div class="action-icon search">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <span>搜索文章</span>
            </button>
            <button @click="router.push('/ai-research')">
              <div class="action-icon research">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
                </svg>
              </div>
              <span>深度研究</span>
            </button>
            <button @click="createNewConversation('帮我解答一个技术问题')">
              <div class="action-icon qa">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <span>技术问答</span>
            </button>
            <button @click="router.push('/ai-writing')">
              <div class="action-icon write">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </div>
              <span>写作助手</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 聊天界面 -->
      <template v-else>
        <!-- 头部 -->
        <header class="chat-header">
          <div class="header-left">
            <div class="chat-icon">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div class="chat-info">
              <h3>{{ currentConversation.title || '新对话' }}</h3>
              <span>{{ currentConversation.messageCount || 0 }} 条消息</span>
            </div>
          </div>
          <n-button text size="small" @click="createNewConversation()">
            <template #icon>
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </template>
          </n-button>
        </header>

        <!-- 消息列表 -->
        <div ref="messagesContainer" class="messages-container">
          <div v-if="messagesLoading" class="loading-state">
            <n-spin size="medium" />
          </div>
          <template v-else>
            <div
              v-for="message in messages"
              :key="message.id"
              class="message-item"
              :class="message.role"
            >
              <!-- 助手头像 -->
              <div v-if="message.role === 'assistant'" class="avatar assistant-avatar">
                <img :src="aiAssistantAvatar || DEFAULT_AVATAR" alt="AI 助手" class="avatar-img" />
              </div>

              <!-- 消息内容 -->
              <div class="message-content">
                <!-- 打字动画 -->
                <div v-if="isStreaming && !message.content && !message.thinking" class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>

                <!-- 思考过程 -->
                <div v-if="message.thinking" class="thinking-panel">
                  <div class="thinking-header" @click="toggleThinking(message.id)">
                    <div class="thinking-title">
                      <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                      </svg>
                      <span>思考过程</span>
                    </div>
                    <svg class="toggle-icon" :class="{ expanded: expandedThinking[message.id] }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                  <div v-show="expandedThinking[message.id]" class="thinking-content">
                    {{ message.thinking }}
                  </div>
                </div>

                <!-- Markdown 内容 -->
                <div class="message-text markdown-body" v-html="renderMarkdown(message.content)"></div>

                <!-- 时间 -->
                <div class="message-time">{{ formatFullTime(message.createdAt) }}</div>
              </div>

              <!-- 用户头像 -->
              <div v-if="message.role === 'user'" class="avatar user-avatar">
                <img :src="userStore.state.userInfo?.avatar || DEFAULT_AVATAR" alt="用户头像" class="avatar-img" />
              </div>
            </div>
          </template>
        </div>

        <!-- 写作进度面板（固定在输入区域上方） -->
        <WritingProgressPanel
          ref="writingProgressRef"
          :visible="showWritingProgress"
          :task-id="writingTaskId"
          @close="closeWritingProgress"
          @view-result="viewWritingResult"
        />

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-wrapper">
            <!-- 输入框 -->
            <textarea
              v-model="inputMessage"
              @keydown="handleKeyDown"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行..."
              rows="1"
              :disabled="isStreaming"
            ></textarea>

            <!-- 发送/停止按钮 -->
            <button
              v-if="isStreaming"
              class="stop-btn"
              @click="stopStreaming"
            >
              <svg fill="currentColor" viewBox="0 0 24 24">
                <rect x="6" y="6" width="12" height="12" rx="2" />
              </svg>
            </button>
            <button
              v-else
              class="send-btn"
              :disabled="!String(inputMessage ?? '').trim()"
              @click="sendMessage"
            >
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </button>
          </div>
          <p class="disclaimer">AI 助手可能会产生不准确的信息，请注意甄别</p>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
/* ==================== 页面布局 ==================== */
.ai-chat-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 50%, #f5f7fa 100%);
}

.dark .ai-chat-page {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%);
}

/* ==================== 侧边栏 ==================== */
.sidebar {
  width: 280px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  position: relative;
}

.sidebar.collapsed {
  width: 70px;
}

.dark .sidebar {
  background: rgba(30, 41, 59, 0.95);
  border-right-color: rgba(255, 255, 255, 0.05);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.dark .sidebar-header {
  border-bottom-color: rgba(255, 255, 255, 0.05);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.logo-icon svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.dark .logo-text {
  color: #f3f4f6;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.new-chat-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.new-chat-btn svg {
  width: 18px;
  height: 18px;
}

/* ==================== 会话列表 ==================== */
.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.conversation-item:hover {
  background: rgba(139, 92, 246, 0.05);
}

.conversation-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
}

.dark .conversation-item:hover {
  background: rgba(139, 92, 246, 0.1);
}

.dark .conversation-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(168, 85, 247, 0.15) 100%);
}

.conversation-icon {
  width: 36px;
  height: 36px;
  background: rgba(139, 92, 246, 0.1);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8b5cf6;
  flex-shrink: 0;
}

.conversation-icon svg {
  width: 18px;
  height: 18px;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .conversation-title {
  color: #f3f4f6;
}

.conversation-meta {
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.delete-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  background: rgba(239, 68, 68, 0.1);
  border: none;
  border-radius: 8px;
  color: #ef4444;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

.delete-btn svg {
  width: 16px;
  height: 16px;
}

/* ==================== 分页组件 ==================== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
  margin-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.dark .pagination-wrapper {
  border-top-color: rgba(255, 255, 255, 0.05);
}

/* ==================== 折叠按钮 ==================== */
.collapse-btn {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s ease;
  z-index: 10;
}

.dark .collapse-btn {
  background: #374151;
  border-color: rgba(255, 255, 255, 0.1);
  color: #9ca3af;
}

.collapse-btn:hover {
  background: #f3f4f6;
  color: #8b5cf6;
}

.dark .collapse-btn:hover {
  background: #4b5563;
  color: #a78bfa;
}

.collapse-btn svg {
  width: 14px;
  height: 14px;
  transition: transform 0.3s ease;
}

.collapse-btn svg.rotated {
  transform: rotate(180deg);
}

/* ==================== 主内容区 ==================== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* ==================== 欢迎页 ==================== */
.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.3);
}

.welcome-icon svg {
  width: 40px;
  height: 40px;
}

.welcome-content h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.dark .welcome-content h2 {
  color: #f3f4f6;
}

.welcome-content p {
  font-size: 16px;
  color: #6b7280;
  margin-bottom: 32px;
  line-height: 1.6;
}

.dark .welcome-content p {
  color: #9ca3af;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quick-actions button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dark .quick-actions button {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.05);
}

.quick-actions button:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.dark .quick-actions button:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.action-icon.search {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.action-icon.code {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.action-icon.qa {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.action-icon.write {
  background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
}

.action-icon.research {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
}

.action-icon svg {
  width: 24px;
  height: 24px;
}

.quick-actions span {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.dark .quick-actions span {
  color: #e5e7eb;
}

/* ==================== 聊天头部 ==================== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.dark .chat-header {
  background: rgba(30, 41, 59, 0.8);
  border-bottom-color: rgba(255, 255, 255, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.chat-icon svg {
  width: 20px;
  height: 20px;
}

.chat-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.dark .chat-info h3 {
  color: #f3f4f6;
}

.chat-info span {
  font-size: 12px;
  color: #9ca3af;
}

/* ==================== 消息列表 ==================== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar svg {
  width: 18px;
  height: 18px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.assistant-avatar {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
}

.user-avatar {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
}

.message-item.user .user-avatar {
  order: -1;
}

.message-content {
  max-width: 70%;
}

.message-item.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: rgba(139, 92, 246, 0.05);
  border-radius: 12px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #8b5cf6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 思考过程 */
.thinking-panel {
  margin-bottom: 12px;
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: rgba(139, 92, 246, 0.02);
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.thinking-header:hover {
  background: rgba(139, 92, 246, 0.05);
}

.thinking-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #8b5cf6;
}

.thinking-title svg {
  width: 16px;
  height: 16px;
}

.toggle-icon {
  width: 16px;
  height: 16px;
  color: #8b5cf6;
  transition: transform 0.2s ease;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.thinking-content {
  padding: 12px 16px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
  white-space: pre-wrap;
  border-top: 1px solid rgba(139, 92, 246, 0.1);
  max-height: 200px;
  overflow-y: auto;
}

.dark .thinking-content {
  color: #9ca3af;
}

/* 消息文本 */
.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.message-item.assistant .message-text {
  background: rgba(0, 0, 0, 0.02);
  color: #374151;
}

.dark .message-item.assistant .message-text {
  background: rgba(255, 255, 255, 0.05);
  color: #e5e7eb;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
}

.message-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

/* ==================== 输入区域 ==================== */
.input-area {
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 0 0 12px 12px;
}

.dark .input-area {
  background: rgba(30, 41, 59, 0.8);
  border-top-color: rgba(255, 255, 255, 0.05);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 16px;
  padding: 8px;
  transition: all 0.2s ease;
}

.dark .input-wrapper {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.input-wrapper:focus-within {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  min-height: 40px;
  max-height: 120px;
  background: transparent;
  color: #1f2937;
}

.dark .input-wrapper textarea {
  color: #f3f4f6;
}

.input-wrapper textarea::placeholder {
  color: #9ca3af;
}

.send-btn,
.stop-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.stop-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.stop-btn:hover {
  transform: scale(1.05);
}

.send-btn svg,
.stop-btn svg {
  width: 18px;
  height: 18px;
}

.disclaimer {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 12px;
}

/* ==================== 加载状态 ==================== */
.loading-state {
  display: flex;
  justify-content: center;
  padding: 40px;
}

/* ==================== 滚动条 ==================== */
.messages-container::-webkit-scrollbar,
.conversations-list::-webkit-scrollbar,
.thinking-content::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track,
.conversations-list::-webkit-scrollbar-track,
.thinking-content::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb,
.conversations-list::-webkit-scrollbar-thumb,
.thinking-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.dark .messages-container::-webkit-scrollbar-thumb,
.dark .conversations-list::-webkit-scrollbar-thumb,
.dark .thinking-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}
</style>

<!-- Markdown 样式（非 scoped） -->
<style>
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.markdown-body p {
  margin: 0.5em 0;
}

.markdown-body p:first-child {
  margin-top: 0;
}

.markdown-body p:last-child {
  margin-bottom: 0;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  font-weight: 600;
  margin: 0.5em 0 0.25em;
  line-height: 1.35;
}

.markdown-body h1 {
  font-size: 1.2em;
}

.markdown-body h2 {
  font-size: 1.1em;
}

.markdown-body h3 {
  font-size: 1em;
}

.markdown-body ul,
.markdown-body ol {
  margin: 0.25em 0;
  padding-left: 1.5em;
}

.markdown-body li {
  margin: 0.05em 0;
}

.markdown-body pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin: 0.5em 0;
  font-size: 0.85em;
  line-height: 1.5;
  white-space: pre;
  word-break: normal;
  word-wrap: normal;
}

.dark .markdown-body pre {
  background: #0f172a;
}

.markdown-body pre code {
  background: none;
  padding: 0;
  border-radius: 0;
  color: inherit;
  white-space: pre;
  word-break: normal;
  word-wrap: normal;
}

.markdown-body :not(pre) > code {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.125em 0.375em;
  border-radius: 0.25rem;
  font-size: 0.85em;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
}

.dark .markdown-body :not(pre) > code {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-body blockquote {
  border-left: 3px solid #8b5cf6;
  padding-left: 0.75rem;
  margin: 0.5em 0;
  color: #64748b;
}

.dark .markdown-body blockquote {
  color: #94a3b8;
}

.markdown-body a {
  color: #8b5cf6;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-body a:hover {
  color: #7c3aed;
}

.markdown-body strong {
  font-weight: 600;
}

.markdown-body em {
  font-style: italic;
}

.markdown-body hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 0.75em 0;
}

.dark .markdown-body hr {
  border-top-color: #475569;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
  font-size: 0.85em;
}

.markdown-body th,
.markdown-body td {
  border: 1px solid #e2e8f0;
  padding: 0.375rem 0.75rem;
  text-align: left;
}

.dark .markdown-body th,
.dark .markdown-body td {
  border-color: #475569;
}

.markdown-body th {
  background: #f8fafc;
  font-weight: 600;
}

.dark .markdown-body th {
  background: #334155;
}

.markdown-body img {
  max-width: 100%;
  border-radius: 0.5rem;
  margin: 0.25em 0;
}

/* 代码块复制按钮 */
.code-block-wrapper {
  position: relative;
  border-radius: 0.5rem;
  margin: 0.5em 0;
}

.code-block-wrapper pre {
  margin: 0;
}

.code-copy-btn {
  position: absolute;
  top: 0.375rem;
  right: 0.375rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s;
  opacity: 0;
}

.code-block-wrapper:hover .code-copy-btn {
  opacity: 1;
}

.code-copy-btn:hover {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.15);
}

.code-copy-btn.copied {
  color: #4ade80;
  border-color: rgba(74, 222, 128, 0.3);
  opacity: 1;
}
</style>
