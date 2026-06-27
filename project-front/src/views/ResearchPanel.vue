<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue'
import {marked} from 'marked'
import {NEmpty, NProgress, NSpin} from 'naive-ui'
import {aiApi, isLoggedIn} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatRelativeTime} from '@/utils/format'

// ==================== 研究阶段枚举 ====================
const PHASE = {
  INPUT: 'input',
  THINKING: 'thinking',
  CLARIFICATION: 'clarification',
  PLAN: 'plan',
  EXECUTING: 'executing',
  REPORT: 'report'
}

// ==================== 响应式数据 ====================
const currentPhase = ref(PHASE.INPUT)
const researchHistory = ref([])
const currentResearch = ref(null)
const researchInput = ref('')
const isLoading = ref(false)
const isStreaming = ref(false)
const thinkingText = ref('')
const thinkingExpanded = ref(true)
const mainContentRef = ref(null)
const currentReader = ref(null)
const currentAbortController = ref(null)
const sidebarCollapsed = ref(false)

// 澄清阶段
const clarificationQuestions = ref([])
const clarificationAnswers = ref([])
const showClarification = ref(false)

// 计划阶段
const planData = ref(null)

// 执行阶段
const taskList = ref([])
const progressPercent = ref(0)
const stageInsight = ref(null)
const replanNotice = ref(null) // 重规划提示数据

// 报告阶段
const reportData = ref(null)

// ==================== 计算属性 ====================

/** 当前研究状态文本 */
const phaseLabel = computed(() => {
  const map = {
    [PHASE.INPUT]: '输入研究主题',
    [PHASE.THINKING]: 'AI 正在分析...',
    [PHASE.CLARIFICATION]: '需要您的补充信息',
    [PHASE.PLAN]: '研究计划待确认',
    [PHASE.EXECUTING]: '研究进行中',
    [PHASE.REPORT]: '研究报告'
  }
  return map[currentPhase.value] || ''
})

/** 研究是否正在进行中 */
const isResearchActive = computed(() => {
  return [PHASE.THINKING, PHASE.EXECUTING].includes(currentPhase.value)
})

// ==================== 工具方法 ====================

/** 渲染 Markdown */
const renderMarkdown = (content) => {
  if (!content) return ''
  const html = marked(content)
  return html.replace(/<pre>/g, '<pre class="code-block">')
}

/** 滚动到底部 */
const scrollToBottom = () => {
  nextTick(() => {
    if (mainContentRef.value) {
      mainContentRef.value.scrollTop = mainContentRef.value.scrollHeight
    }
  })
}

/** 生成 UUID */
const generateId = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/** 任务状态图标 */
const taskStatusIcon = (status) => {
  const map = {pending: '⏸', executing: '⏳', completed: '✅', failed: '❌'}
  return map[status] || '⏸'
}

/** 任务类型颜色 class */
const agentTypeClass = (type) => {
  const map = {search: 'type-search', knowledge: 'type-knowledge', analysis: 'type-analysis'}
  return map[type] || ''
}

/** 任务类型中文标签 */
const agentTypeLabel = (type) => {
  const map = {search: '搜索', knowledge: '知识库', analysis: '分析'}
  return map[type] || type
}

// ==================== 研究历史 ====================

/** 获取研究历史 */
const fetchHistory = async () => {
  if (!isLoggedIn()) return

  try {
    const data = await aiApi.getResearchHistory(1, 20)
    researchHistory.value = data.records || []
  } catch (error) {
    console.error('获取研究历史失败:', error)
  }
}

/** 选择历史研究项 */
const selectHistory = async (item) => {
  if (item.status === 'completed') {
    // 已完成的研究，直接加载报告
    try {
      const data = await aiApi.getResearchReport(item.id)
      reportData.value = data
      currentPhase.value = PHASE.REPORT
    } catch (error) {
      toast.error('加载研究报告失败')
    }
  } else if (item.status === 'executing') {
    // 进行中的研究，跳转到执行阶段
    currentPhase.value = PHASE.EXECUTING
  }
  currentResearch.value = item
}

/** 删除研究历史项 */
const deleteHistory = (item, event) => {
  event.stopPropagation()
  researchHistory.value = researchHistory.value.filter(h => h.id !== item.id)
  toast.success('已删除')
}

// ==================== SSE 事件处理 ====================

/** 开始研究（核心方法） */
const startResearch = async (message) => {
  const content = message || String(researchInput.value ?? '').trim()
  if (!content) {
    toast.warning('请输入研究主题')
    return
  }

  // 重置所有状态
  resetState()
  researchInput.value = content
  currentPhase.value = PHASE.THINKING
  isStreaming.value = true
  isLoading.value = true
  currentResearch.value = {
    id: generateId(),
    topic: content,
    status: 'executing',
    createdAt: new Date().toISOString()
  }

  // 真实 SSE 请求
  try {
    // 启动研究任务并获取 SSE 流
    const result = await aiApi.startResearch(currentResearch.value.id, content)

    if (!result || !result.body) {
      throw new Error('响应失败')
    }

    currentAbortController.value = result.controller
    const reader = result.body.getReader()
    currentReader.value = reader
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const {done, value} = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, {stream: true})
      buffer += chunk
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const part of parts) {
        if (!part.trim()) continue
        const lines = part.split('\n')
        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine || !trimmedLine.startsWith('data:')) continue

          const jsonStr = trimmedLine.substring(5).trim()
          if (!jsonStr) continue

          try {
            const event = JSON.parse(jsonStr)
            handleSSEEvent(event)
          } catch (e) {
            console.warn('解析SSE数据失败:', e, jsonStr)
          }
        }
      }
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('研究请求失败:', error)
      toast.error('研究请求失败，请稍后重试')
      currentPhase.value = PHASE.INPUT
    }
  } finally {
    cleanup()
  }
}

/** 处理单个 SSE 事件 */
const handleSSEEvent = async (event) => {
  switch (event.type) {
    case 'thinking':
      thinkingText.value += event.data || ''
      thinkingExpanded.value = true
      scrollToBottom()
      break

    case 'planner_thinking':
      // Planner 思考内容流式输出（data 为字符串）
      thinkingText.value += event.data || ''
      thinkingExpanded.value = true
      scrollToBottom()
      break

    case 'clarification':
      clarificationQuestions.value = event.data?.questions || []
      clarificationAnswers.value = clarificationQuestions.value.map(() => '')
      showClarification.value = true
      currentPhase.value = PHASE.CLARIFICATION
      break

    case 'plan_approval':
      planData.value = event.data
      currentPhase.value = PHASE.PLAN
      break

    case 'task_progress':
      // 实时更新子任务进度（搜索/爬取数量、完成状态）
      if (event.data) {
        const taskId = event.data.task_id
        if (!taskId) break

        // 确保在执行阶段
        if (currentPhase.value !== PHASE.EXECUTING) {
          currentPhase.value = PHASE.EXECUTING
        }

        const idx = taskList.value.findIndex(t => t.id === taskId)
        if (idx !== -1) {
          const task = {...taskList.value[idx]}
          // 更新任务状态
          if (event.data.status) {
            task.status = event.data.status
          }
          // 更新进度信息（搜索次数、爬取次数、查询次数）
          if (event.data.search_count !== undefined) task.search_count = event.data.search_count
          if (event.data.scrape_count !== undefined) task.scrape_count = event.data.scrape_count
          if (event.data.query_count !== undefined) task.query_count = event.data.query_count
          // 更新进度消息
          if (event.data.message) task.progress_message = event.data.message
          // 完成时更新摘要
          if (event.data.summary) task.summary = event.data.summary
          taskList.value[idx] = task
        }
        updateProgress()
      }
      break

    case 'phase':
      if (event.data) {
        const phase = event.data.phase

        // planning 阶段保持 THINKING 页面，不跳转
        if (phase === 'planning') {
          break
        }

        // worker 执行完成时才跳转到执行阶段
        if (phase === 'executing' || phase === 'completed' || phase === 'evaluating' || phase === 'worker_completed') {
          // 已经进入报告阶段时，不因 completed 事件回退
          if (currentPhase.value === PHASE.REPORT) break
          if (currentPhase.value !== PHASE.EXECUTING) {
            currentPhase.value = PHASE.EXECUTING
          }
        }

        // 报告生成阶段
        if (phase === 'reporting') {
          currentPhase.value = PHASE.REPORT
        }

        // 更新任务状态
        if (event.data.tasks) {
          taskList.value = event.data.tasks
        } else if (event.data.task_id) {
          const idx = taskList.value.findIndex(t => t.id === event.data.task_id)
          if (idx !== -1) {
            taskList.value[idx] = {...taskList.value[idx], ...event.data}
          }
        }
        updateProgress()
      }
      break

    case 'stage_insight':
      stageInsight.value = event.data
      break

    case 'replan':
      // 处理重规划事件：更新任务列表 + 显示重规划提示
      if (event.data) {
        // 更新任务列表（包含旧任务 + 新增任务）
        if (event.data.updated_task_list) {
          taskList.value = event.data.updated_task_list
        }
        // 显示重规划提示卡片
        replanNotice.value = {
          round: event.data.round,
          reason: event.data.reason,
          addedTasks: event.data.added_tasks || [],
        }
        // 重算进度条
        updateProgress()
      }
      break

    case 'report_ready':
      reportData.value = event.data
      currentPhase.value = PHASE.REPORT
      if (currentResearch.value) {
        currentResearch.value.status = 'completed'
      }
      break

    case 'error':
      toast.error(event.data || '研究过程出现错误')
      break

    case 'done':
      cleanup()
      break
  }
}

/** 更新进度百分比 */
const updateProgress = () => {
  if (taskList.value.length === 0) return
  const completed = taskList.value.filter(t => t.status === 'completed').length
  progressPercent.value = Math.round((completed / taskList.value.length) * 100)
}

/** 停止研究 */
const stopResearch = async () => {
  // 通知后端停止
  if (currentResearch.value) {
    try {
      await aiApi.stopResearch(currentResearch.value.id)
    } catch (e) {
      console.warn('停止研究接口调用失败:', e)
    }
  }

  // 终止 HTTP 连接
  if (currentAbortController.value) {
    try {
      currentAbortController.value.abort()
    } catch (e) { /* ignore */ }
    currentAbortController.value = null
  }

  if (currentReader.value) {
    try {
      currentReader.value.cancel()
    } catch (e) { /* ignore */ }
    currentReader.value = null
  }

  cleanup()
  toast.info('已停止研究')
  currentPhase.value = PHASE.INPUT
}

/** 清理状态 */
const cleanup = () => {
  isStreaming.value = false
  isLoading.value = false
  currentReader.value = null
  currentAbortController.value = null
}

/** 重置所有状态 */
const resetState = () => {
  currentPhase.value = PHASE.INPUT
  thinkingText.value = ''
  thinkingExpanded.value = true
  clarificationQuestions.value = []
  clarificationAnswers.value = []
  showClarification.value = false
  planData.value = null
  taskList.value = []
  progressPercent.value = 0
  stageInsight.value = null
  replanNotice.value = null // 重置重规划提示
  reportData.value = null
}

// ==================== 澄清交互 ====================

/** 提交澄清回答 */
const submitClarification = async () => {
  showClarification.value = false
  currentPhase.value = PHASE.THINKING
  thinkingText.value += '\n\n用户已回答澄清问题，继续研究...\n'

  // 真实请求：将回答提交给后端，并处理 SSE 流
  try {
    const answers = clarificationQuestions.value.map((q, i) => ({
      question: q,
      answer: clarificationAnswers.value[i] || ''
    }))
    await resumeResearchStream(JSON.stringify(answers))
  } catch (error) {
    console.error('提交澄清回答失败:', error)
    toast.error('提交失败')
  }
}

/** 跳过澄清 */
const skipClarification = () => {
  clarificationAnswers.value = clarificationQuestions.value.map(() => '')
  submitClarification()
}

// ==================== 计划交互 ====================

/** 确认并开始执行计划 */
const confirmPlan = async () => {
  currentPhase.value = PHASE.EXECUTING
  taskList.value = planData.value?.tasks || []
  updateProgress()

  try {
    await resumeResearchStream('approve')
  } catch (error) {
    console.error('确认计划失败:', error)
    toast.error('确认计划失败')
  }
}

/** 修改计划 */
const modifyPlan = async () => {
  try {
    await resumeResearchStream('revise')
    currentPhase.value = PHASE.THINKING
  } catch (error) {
    console.error('修改计划失败:', error)
    toast.error('修改计划失败')
  }
}

/** 恢复研究任务的 SSE 流处理（通用方法） */
const resumeResearchStream = async (response) => {
  const result = await aiApi.resumeResearch(currentResearch.value.id, response)

  if (!result || !result.body) {
    throw new Error('响应失败')
  }

  currentAbortController.value = result.controller
  const reader = result.body.getReader()
  currentReader.value = reader
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const {done, value} = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, {stream: true})
    buffer += chunk
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''

    for (const part of parts) {
      if (!part.trim()) continue
      const lines = part.split('\n')
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine || !trimmedLine.startsWith('data:')) continue

        const jsonStr = trimmedLine.substring(5).trim()
        if (!jsonStr) continue

        try {
          const event = JSON.parse(jsonStr)
          handleSSEEvent(event)
        } catch (e) {
          console.warn('解析SSE数据失败:', e, jsonStr)
        }
      }
    }
  }

  cleanup()
}

// ==================== 报告操作 ====================

/** 下载 Markdown 文件 */
const downloadMarkdown = () => {
  if (!reportData.value?.content) return

  const blob = new Blob([reportData.value.content], {type: 'text/markdown;charset=utf-8'})
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${reportData.value.topic || '研究报告'}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  toast.success('已下载研究报告')
}

/** 开始新研究 */
const newResearch = () => {
  resetState()
  researchInput.value = ''
}

// ==================== 生命周期 ====================

onMounted(() => {
  document.title = '深度研究 - ByteBlog'
  fetchHistory()
})

onUnmounted(() => {
  // 组件销毁时清理连接
  if (currentAbortController.value) {
    try { currentAbortController.value.abort() } catch (e) { /* ignore */ }
  }
  if (currentReader.value) {
    try { currentReader.value.cancel() } catch (e) { /* ignore */ }
  }
})

// ==================== Expose ====================
defineExpose({
  startResearch,
  stopResearch
})
</script>

<template>
  <div class="research-page">
    <!-- ==================== 左侧边栏：研究历史 ==================== -->
    <aside class="sidebar" :class="{collapsed: sidebarCollapsed}">
      <!-- 侧边栏头部 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
            </svg>
          </div>
          <span v-if="!sidebarCollapsed" class="logo-text">深度研究</span>
        </div>
        <button class="new-research-btn" @click="newResearch">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          <span v-if="!sidebarCollapsed">新研究</span>
        </button>
      </div>

      <!-- 研究历史列表 -->
      <div v-if="!sidebarCollapsed" class="history-list">
        <div v-if="researchHistory.length === 0" class="empty-state">
          <n-empty description="暂无研究记录" size="small"/>
        </div>
        <template v-else>
          <div
              v-for="item in researchHistory"
              :key="item.id"
              class="history-item"
              :class="{active: currentResearch && currentResearch.id === item.id}"
              @click="selectHistory(item)"
          >
            <div class="history-icon" :class="'status-' + item.status">
              <!-- 研究图标 -->
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
            </div>
            <div class="history-info">
              <div class="history-title">{{ item.topic }}</div>
              <div class="history-meta">
                <span class="status-badge" :class="'badge-' + item.status">
                  {{ item.status === 'completed' ? '已完成' : '进行中' }}
                </span>
                <span>{{ formatRelativeTime(item.createdAt) }}</span>
              </div>
            </div>
            <button class="delete-btn" @click="deleteHistory(item, $event)">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </template>
      </div>

      <!-- 折叠按钮 -->
      <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
        <svg :class="{rotated: sidebarCollapsed}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7"/>
        </svg>
      </button>
    </aside>

    <!-- ==================== 右侧主内容区 ==================== -->
    <main ref="mainContentRef" class="main-content">
      <!-- ======== Phase 1: 输入 ======== -->
      <div v-if="currentPhase === PHASE.INPUT" class="phase-input">
        <div class="welcome-content">
          <div class="welcome-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
            </svg>
          </div>
          <h2>深度研究助手</h2>
          <p>输入你想深入研究的技术主题，AI 将自动搜索站内文章、知识库和技术博客，为你生成全面的研究报告</p>

          <div class="input-card">
            <textarea
                v-model="researchInput"
                placeholder="输入研究主题，例如：Redis 分布式锁实现方案、Spring Boot 4 新特性分析..."
                rows="4"
                @keydown.ctrl.enter="startResearch()"
            ></textarea>
            <div class="input-footer">
              <span class="shortcut-hint">Ctrl + Enter 快速开始</span>
              <button
                  class="start-btn"
                  :disabled="!String(researchInput ?? '').trim()"
                  @click="startResearch()"
              >
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
                开始研究
              </button>
            </div>
          </div>

          <!-- 快捷主题建议 -->
          <div class="quick-topics">
            <button @click="researchInput = 'Redis 分布式锁实现方案'">Redis 分布式锁</button>
            <button @click="researchInput = 'Spring Boot 4 新特性分析'">Spring Boot 4 新特性</button>
            <button @click="researchInput = 'Vue 3 性能优化最佳实践'">Vue 3 性能优化</button>
            <button @click="researchInput = '微服务架构设计模式'">微服务架构</button>
          </div>
        </div>
      </div>

      <!-- ======== Phase 2: 思考 ======== -->
      <div v-if="currentPhase === PHASE.THINKING" class="phase-thinking">
        <div class="phase-header">
          <div class="phase-icon thinking-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
            </svg>
          </div>
          <div>
            <h3>AI 正在分析您的研究需求</h3>
            <p>正在拆解任务、规划研究路径...</p>
          </div>
        </div>

        <!-- 思考面板（可折叠） -->
        <div class="thinking-panel">
          <div class="thinking-header" @click="thinkingExpanded = !thinkingExpanded">
            <div class="thinking-title">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
              </svg>
              <span>思考过程</span>
            </div>
            <svg class="toggle-icon" :class="{expanded: thinkingExpanded}" fill="none" stroke="currentColor"
                 viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </div>
          <div v-show="thinkingExpanded" class="thinking-content">
            <span v-if="!thinkingText" class="thinking-dots">
              <span></span><span></span><span></span>
            </span>
            <template v-else>{{ thinkingText }}</template>
          </div>
        </div>

        <!-- 停止按钮 -->
        <div class="action-bar">
          <button class="stop-research-btn" @click="stopResearch">
            <svg fill="currentColor" viewBox="0 0 24 24">
              <rect x="6" y="6" width="12" height="12" rx="2"/>
            </svg>
            停止研究
          </button>
        </div>
      </div>

      <!-- ======== Phase 3: 澄清（模态覆盖层） ======== -->
      <div v-if="currentPhase === PHASE.CLARIFICATION" class="phase-clarification">
        <div class="clarification-backdrop">
          <div class="clarification-card">
            <div class="clarification-header">
              <div class="clarification-icon">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>
              <h3>需要您补充一些信息</h3>
              <p>为了更好地进行研究，请回答以下问题（可跳过）</p>
            </div>

            <div class="questions-list">
              <div v-for="(q, idx) in clarificationQuestions" :key="idx" class="question-item">
                <label>{{ q }}</label>
                <textarea
                    v-model="clarificationAnswers[idx]"
                    placeholder="请输入您的回答..."
                    rows="2"
                ></textarea>
              </div>
            </div>

            <div class="clarification-actions">
              <button class="skip-btn" @click="skipClarification">跳过</button>
              <button class="submit-btn" @click="submitClarification">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
                提交回答
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ======== Phase 4: 计划审批 ======== -->
      <div v-if="currentPhase === PHASE.PLAN && planData" class="phase-plan">
        <div class="plan-card">
          <div class="plan-header">
            <h3>📋 研究计划</h3>
            <span class="duration-badge">预计 3-5 分钟</span>
          </div>

          <div class="plan-topic">
            <span class="topic-label">研究主题</span>
            <span class="topic-value">{{ planData.topic }}</span>
          </div>

          <div class="plan-tasks">
            <div v-for="task in planData.tasks" :key="task.id" class="plan-task-item">
              <div class="task-header">
                <span class="task-icon">{{ taskStatusIcon(task.status) }}</span>
                <span class="task-desc">{{ task.description }}</span>
                <span class="agent-tag" :class="agentTypeClass(task.agent_type)">
                  {{ agentTypeLabel(task.agent_type) }}
                </span>
              </div>
              <div v-if="task.depends_on && task.depends_on.length > 0" class="task-deps">
                依赖: {{ task.depends_on.map(d => {
                  const dep = planData.tasks.find(t => t.id === d)
                  return dep ? dep.description.substring(0, 10) + '...' : d
                }).join(', ') }}
              </div>
            </div>
          </div>

          <div class="plan-actions">
            <button class="modify-btn" @click="modifyPlan">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              修改计划
            </button>
            <button class="confirm-btn" @click="confirmPlan">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              确认并开始
            </button>
          </div>
        </div>
      </div>

      <!-- ======== Phase 5: 执行进度 ======== -->
      <div v-if="currentPhase === PHASE.EXECUTING" class="phase-executing">
        <div class="executing-header">
          <div class="phase-icon executing-icon">
            <n-spin size="medium"/>
          </div>
          <div>
            <h3>研究进行中</h3>
            <p>{{ researchInput }}</p>
          </div>
        </div>

        <!-- 进度条 -->
        <div class="progress-section">
          <div class="progress-info">
            <span>总体进度</span>
            <span>{{ progressPercent }}%</span>
          </div>
          <n-progress
              type="line"
              :percentage="progressPercent"
              :show-indicator="false"
              :height="8"
              border-radius="4px"
              processing
          />
        </div>

        <!-- 重规划提示卡片 -->
        <div v-if="replanNotice" class="replan-card">
          <div class="replan-header">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            <span>计划已更新 (Round {{ replanNotice.round }})</span>
          </div>
          <p class="replan-reason">{{ replanNotice.reason }}</p>
          <div v-if="replanNotice.addedTasks.length > 0" class="replan-tasks">
            <div v-for="task in replanNotice.addedTasks" :key="task.id" class="replan-task-item">
              <span class="replan-task-icon">+</span>
              <span class="replan-task-desc">{{ task.description }}</span>
              <span class="agent-tag small" :class="agentTypeClass(task.agent_type)">
                {{ agentTypeLabel(task.agent_type) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 任务列表 -->
        <div class="task-list">
          <div
              v-for="task in taskList"
              :key="task.id"
              class="task-item"
              :class="'task-' + task.status"
          >
            <span class="task-status-icon">{{ taskStatusIcon(task.status) }}</span>
            <div class="task-detail">
              <div class="task-name">{{ task.description }}</div>
              <div class="task-meta">
                <span class="agent-tag small" :class="agentTypeClass(task.agent_type)">
                  {{ agentTypeLabel(task.agent_type) }}
                </span>
                <!-- 执行中：显示实时进度消息 -->
                <span v-if="task.status === 'executing' && task.progress_message" class="task-progress">
                  {{ task.progress_message }}
                </span>
                <!-- 已完成：显示简要摘要 -->
                <span v-else-if="task.status === 'completed' && task.summary" class="task-result">
                  {{ task.summary }}
                </span>
                <!-- 失败：显示错误摘要 -->
                <span v-else-if="task.status === 'failed' && task.summary" class="task-error">
                  {{ task.summary }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 阶段洞察卡片 -->
        <div v-if="stageInsight" class="insight-card">
          <div class="insight-header">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <span>阶段洞察 (Round {{ stageInsight.round }})</span>
          </div>
          <p class="insight-text">{{ stageInsight.insight }}</p>
          <div class="insight-stats">
            已完成 {{ stageInsight.completed_tasks }} / {{ stageInsight.total_tasks }} 个任务
          </div>
        </div>

        <!-- 停止按钮 -->
        <div class="action-bar">
          <button class="stop-research-btn" @click="stopResearch">
            <svg fill="currentColor" viewBox="0 0 24 24">
              <rect x="6" y="6" width="12" height="12" rx="2"/>
            </svg>
            停止研究
          </button>
        </div>
      </div>

      <!-- ======== Phase 6: 研究报告 ======== -->
      <div v-if="currentPhase === PHASE.REPORT && !reportData" class="phase-report-loading">
        <div class="report-loading-card">
          <div class="spinner"></div>
          <span>正在生成研究报告...</span>
        </div>
      </div>
      <div v-if="currentPhase === PHASE.REPORT && reportData" class="phase-report">
        <!-- 报告头部 -->
        <div class="report-header">
          <div class="report-title-section">
            <div class="report-icon">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
            <div>
              <h3>{{ reportData.topic }}</h3>
              <p>研究报告已完成</p>
            </div>
          </div>
          <div class="report-actions-top">
            <button class="download-btn" @click="downloadMarkdown">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
              </svg>
              下载 Markdown
            </button>
          </div>
        </div>

        <!-- 摘要 -->
        <div v-if="reportData.summary" class="summary-card">
          <div class="summary-header">📌 核心摘要</div>
          <p>{{ reportData.summary }}</p>
        </div>

        <!-- 关键发现 -->
        <div v-if="reportData.key_findings && reportData.key_findings.length > 0" class="findings-card">
          <div class="findings-header">🔍 关键发现</div>
          <ul class="findings-list">
            <li v-for="(finding, idx) in reportData.key_findings" :key="idx">{{ finding }}</li>
          </ul>
        </div>

        <!-- Markdown 正文 -->
        <div class="report-body markdown-body" v-html="renderMarkdown(reportData.content)"></div>

        <!-- 参考来源 -->
        <div v-if="reportData.sources && reportData.sources.length > 0" class="sources-card">
          <div class="sources-header">📚 参考来源</div>
          <div class="sources-list">
            <a
                v-for="(src, idx) in reportData.sources"
                :key="idx"
                :href="src.url"
                :target="src.url.startsWith('http') ? '_blank' : '_self'"
                class="source-item"
            >
              <span class="source-index">{{ idx + 1 }}</span>
              <span class="source-title">{{ src.title }}</span>
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" class="source-link-icon">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
              </svg>
            </a>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="report-footer">
          <button class="download-btn" @click="downloadMarkdown">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            下载 Markdown
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ==================== 页面布局 ==================== */
.research-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 50%, #f5f7fa 100%);
}

.dark .research-page {
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
  flex-shrink: 0;
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

.new-research-btn {
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

.new-research-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.new-research-btn svg {
  width: 18px;
  height: 18px;
}

/* ==================== 历史列表 ==================== */
.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.empty-state {
  padding: 20px 0;
}

.history-item {
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

.history-item:hover {
  background: rgba(139, 92, 246, 0.05);
}

.history-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
}

.dark .history-item:hover {
  background: rgba(139, 92, 246, 0.1);
}

.dark .history-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(168, 85, 247, 0.15) 100%);
}

.history-icon {
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

.history-icon.status-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.history-icon.status-executing {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.history-icon svg {
  width: 18px;
  height: 18px;
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .history-title {
  color: #f3f4f6;
}

.history-meta {
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.status-badge {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.badge-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.badge-executing {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
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

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

.delete-btn svg {
  width: 16px;
  height: 16px;
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
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* ==================== Phase 1: 输入 ==================== */
.phase-input {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-content {
  text-align: center;
  max-width: 640px;
  width: 100%;
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

.input-card {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.dark .input-card {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}

.input-card:focus-within {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1), 0 4px 16px rgba(0, 0, 0, 0.04);
}

.input-card textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.6;
  color: #1f2937;
  background: transparent;
  font-family: inherit;
}

.dark .input-card textarea {
  color: #f3f4f6;
}

.input-card textarea::placeholder {
  color: #9ca3af;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.shortcut-hint {
  font-size: 12px;
  color: #9ca3af;
}

.start-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
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

.start-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.start-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.start-btn svg {
  width: 18px;
  height: 18px;
}

.quick-topics {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  flex-wrap: wrap;
  justify-content: center;
}

.quick-topics button {
  padding: 8px 16px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 20px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .quick-topics button {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
  color: #9ca3af;
}

.quick-topics button:hover {
  border-color: #8b5cf6;
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.05);
}

/* ==================== Phase 2: 思考 ==================== */
.phase-thinking {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.phase-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.phase-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.thinking-icon {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.phase-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px;
}

.dark .phase-header h3 {
  color: #f3f4f6;
}

.phase-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.dark .phase-header p {
  color: #9ca3af;
}

.thinking-panel {
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: rgba(139, 92, 246, 0.02);
  margin-bottom: 24px;
}

.dark .thinking-panel {
  background: rgba(139, 92, 246, 0.05);
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
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
  font-size: 14px;
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
  padding: 16px 18px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
  white-space: pre-wrap;
  border-top: 1px solid rgba(139, 92, 246, 0.1);
  max-height: 320px;
  overflow-y: auto;
}

.dark .thinking-content {
  color: #9ca3af;
}

/* 思考中的动画点 */
.thinking-dots {
  display: flex;
  gap: 6px;
  padding: 4px 0;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  background: #8b5cf6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.thinking-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
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

.action-bar {
  display: flex;
  justify-content: center;
}

.stop-research-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.stop-research-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
}

.stop-research-btn svg {
  width: 16px;
  height: 16px;
}

/* ==================== Phase 3: 澄清 ==================== */
.phase-clarification {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.clarification-backdrop {
  max-width: 600px;
  width: 100%;
}

.clarification-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.dark .clarification-card {
  background: rgba(30, 41, 59, 0.9);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.clarification-header {
  text-align: center;
  margin-bottom: 28px;
}

.clarification-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.clarification-icon svg {
  width: 28px;
  height: 28px;
}

.clarification-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}

.dark .clarification-header h3 {
  color: #f3f4f6;
}

.clarification-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.dark .clarification-header p {
  color: #9ca3af;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.question-item label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.dark .question-item label {
  color: #e5e7eb;
}

.question-item textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  color: #1f2937;
  background: #f9fafb;
  outline: none;
  resize: none;
  transition: border-color 0.2s ease;
  font-family: inherit;
  box-sizing: border-box;
}

.dark .question-item textarea {
  background: rgba(15, 23, 42, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
  color: #f3f4f6;
}

.question-item textarea:focus {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.clarification-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.skip-btn {
  padding: 10px 24px;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .skip-btn {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: #9ca3af;
}

.skip-btn:hover {
  background: rgba(0, 0, 0, 0.08);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
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

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.submit-btn svg {
  width: 18px;
  height: 18px;
}

/* ==================== Phase 4: 计划审批 ==================== */
.phase-plan {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.plan-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.dark .plan-card {
  background: rgba(30, 41, 59, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.plan-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.dark .plan-header h3 {
  color: #f3f4f6;
}

.duration-badge {
  padding: 4px 12px;
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.plan-topic {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(139, 92, 246, 0.04);
  border-radius: 12px;
  margin-bottom: 24px;
}

.dark .plan-topic {
  background: rgba(139, 92, 246, 0.08);
}

.topic-label {
  font-size: 13px;
  font-weight: 600;
  color: #8b5cf6;
  white-space: nowrap;
}

.topic-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.dark .topic-value {
  color: #f3f4f6;
}

.plan-tasks {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 28px;
}

.plan-task-item {
  padding: 14px 18px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.dark .plan-task-item {
  border-color: rgba(255, 255, 255, 0.06);
}

.task-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.task-desc {
  flex: 1;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.dark .task-desc {
  color: #e5e7eb;
}

.agent-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.agent-tag.small {
  padding: 2px 8px;
  font-size: 11px;
}

.type-search {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.type-knowledge {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.type-analysis {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.task-deps {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
  padding-left: 26px;
}

.plan-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modify-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .modify-btn {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: #9ca3af;
}

.modify-btn:hover {
  background: rgba(0, 0, 0, 0.08);
}

.modify-btn svg {
  width: 18px;
  height: 18px;
}

.confirm-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
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

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.confirm-btn svg {
  width: 18px;
  height: 18px;
}

/* ==================== Phase 5: 执行进度 ==================== */
.phase-executing {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.executing-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.executing-icon {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  color: #8b5cf6;
}

.executing-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px;
}

.dark .executing-header h3 {
  color: #f3f4f6;
}

.executing-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.dark .executing-header p {
  color: #9ca3af;
}

.progress-section {
  margin-bottom: 28px;
}

.progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
}

.dark .progress-info {
  color: #9ca3af;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.dark .task-item {
  border-color: rgba(255, 255, 255, 0.06);
}

.task-item.task-completed {
  background: rgba(16, 185, 129, 0.03);
  border-color: rgba(16, 185, 129, 0.15);
}

.task-item.task-executing {
  background: rgba(139, 92, 246, 0.03);
  border-color: rgba(139, 92, 246, 0.2);
  animation: pulse-border 2s infinite;
}

@keyframes pulse-border {
  0%, 100% {
    border-color: rgba(139, 92, 246, 0.2);
  }
  50% {
    border-color: rgba(139, 92, 246, 0.4);
  }
}

.task-item.task-failed {
  background: rgba(239, 68, 68, 0.03);
  border-color: rgba(239, 68, 68, 0.15);
}

.task-status-icon {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

.task-detail {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.dark .task-name {
  color: #e5e7eb;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.task-result {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.task-progress {
  font-size: 12px;
  color: #8b5cf6;
  font-weight: 500;
  animation: pulse-text 2s infinite;
}

.task-error {
  font-size: 12px;
  color: #ef4444;
  font-weight: 500;
}

@keyframes pulse-text {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 重规划提示卡片 */
.replan-card {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.04) 0%, rgba(217, 119, 6, 0.04) 100%);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 24px;
}

.dark .replan-card {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(217, 119, 6, 0.08) 100%);
}

.replan-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #f59e0b;
  margin-bottom: 10px;
}

.replan-header svg {
  width: 18px;
  height: 18px;
}

.replan-reason {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 12px;
}

.dark .replan-reason {
  color: #9ca3af;
}

.replan-tasks {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.replan-task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(245, 158, 11, 0.06);
  border-radius: 8px;
}

.dark .replan-task-item {
  background: rgba(245, 158, 11, 0.1);
}

.replan-task-icon {
  width: 20px;
  height: 20px;
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.replan-task-desc {
  flex: 1;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.dark .replan-task-desc {
  color: #e5e7eb;
}

/* 阶段洞察 */
.insight-card {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.04) 0%, rgba(168, 85, 247, 0.04) 100%);
  border: 1px solid rgba(139, 92, 246, 0.15);
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 24px;
}

.dark .insight-card {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08) 0%, rgba(168, 85, 247, 0.08) 100%);
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #8b5cf6;
  margin-bottom: 10px;
}

.insight-header svg {
  width: 18px;
  height: 18px;
}

.insight-text {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 10px;
}

.dark .insight-text {
  color: #9ca3af;
}

.insight-stats {
  font-size: 12px;
  color: #9ca3af;
}

/* ==================== Phase 6: 报告 ==================== */
.phase-report-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  width: 100%;
}

.report-loading-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 32px;
  background: rgba(107, 114, 128, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(107, 114, 128, 0.2);
  font-size: 16px;
  color: #9ca3af;
}

.phase-report {
  padding: 40px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.report-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.report-title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.report-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  flex-shrink: 0;
}

.report-icon svg {
  width: 26px;
  height: 26px;
}

.report-title-section h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px;
}

.dark .report-title-section h3 {
  color: #f3f4f6;
}

.report-title-section p {
  font-size: 14px;
  color: #10b981;
  margin: 0;
  font-weight: 500;
}

.report-actions-top {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.download-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
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

.download-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.download-btn svg {
  width: 18px;
  height: 18px;
}

/* 摘要卡片 */
.summary-card {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.04) 0%, rgba(168, 85, 247, 0.04) 100%);
  border: 1px solid rgba(139, 92, 246, 0.15);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.dark .summary-card {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08) 0%, rgba(168, 85, 247, 0.08) 100%);
}

.summary-header {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 10px;
}

.dark .summary-header {
  color: #f3f4f6;
}

.summary-card p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0;
}

.dark .summary-card p {
  color: #9ca3af;
}

/* 关键发现 */
.findings-card {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.dark .findings-card {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.06);
}

.findings-header {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 14px;
}

.dark .findings-header {
  color: #f3f4f6;
}

.findings-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.findings-list li {
  position: relative;
  padding: 10px 14px 10px 32px;
  background: rgba(16, 185, 129, 0.04);
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
}

.dark .findings-list li {
  background: rgba(16, 185, 129, 0.08);
  color: #e5e7eb;
}

.findings-list li::before {
  content: '✓';
  position: absolute;
  left: 12px;
  top: 10px;
  color: #10b981;
  font-weight: 700;
  font-size: 14px;
}

/* 报告正文 */
.report-body {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 28px;
  margin-bottom: 24px;
}

.dark .report-body {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.06);
  color: #e5e7eb;
}

/* 参考来源 */
.sources-card {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.dark .sources-card {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.06);
}

.sources-header {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 14px;
}

.dark .sources-header {
  color: #f3f4f6;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(139, 92, 246, 0.03);
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
}

.source-item:hover {
  background: rgba(139, 92, 246, 0.08);
}

.source-index {
  width: 24px;
  height: 24px;
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.source-title {
  flex: 1;
  font-size: 14px;
  color: #8b5cf6;
  font-weight: 500;
}

.source-link-icon {
  width: 16px;
  height: 16px;
  color: #9ca3af;
  flex-shrink: 0;
}

/* 报告底部 */
.report-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 8px;
}

/* ==================== 滚动条 ==================== */
.main-content::-webkit-scrollbar,
.history-list::-webkit-scrollbar,
.thinking-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-track,
.history-list::-webkit-scrollbar-track,
.thinking-content::-webkit-scrollbar-track {
  background: transparent;
}

.main-content::-webkit-scrollbar-thumb,
.history-list::-webkit-scrollbar-thumb,
.thinking-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.dark .main-content::-webkit-scrollbar-thumb,
.dark .history-list::-webkit-scrollbar-thumb,
.dark .thinking-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}
</style>

<!-- Markdown 样式（非 scoped，复用 AIChat 的 Markdown 主题） -->
<style>
.phase-report .markdown-body {
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.phase-report .markdown-body p {
  margin: 0.5em 0;
}

.phase-report .markdown-body p:first-child {
  margin-top: 0;
}

.phase-report .markdown-body p:last-child {
  margin-bottom: 0;
}

.phase-report .markdown-body h1,
.phase-report .markdown-body h2,
.phase-report .markdown-body h3,
.phase-report .markdown-body h4,
.phase-report .markdown-body h5,
.phase-report .markdown-body h6 {
  font-weight: 600;
  margin: 0.75em 0 0.35em;
  line-height: 1.35;
}

.phase-report .markdown-body h1 {
  font-size: 1.4em;
}

.phase-report .markdown-body h2 {
  font-size: 1.2em;
}

.phase-report .markdown-body h3 {
  font-size: 1.05em;
}

.phase-report .markdown-body ul,
.phase-report .markdown-body ol {
  margin: 0.35em 0;
  padding-left: 1.5em;
}

.phase-report .markdown-body li {
  margin: 0.1em 0;
}

.phase-report .markdown-body pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin: 0.75em 0;
  font-size: 0.85em;
  line-height: 1.5;
  white-space: pre;
  word-break: normal;
  word-wrap: normal;
}

.dark .phase-report .markdown-body pre {
  background: #0f172a;
}

.phase-report .markdown-body pre code {
  background: none;
  padding: 0;
  border-radius: 0;
  color: inherit;
  white-space: pre;
  word-break: normal;
  word-wrap: normal;
}

.phase-report .markdown-body :not(pre) > code {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.125em 0.375em;
  border-radius: 0.25rem;
  font-size: 0.85em;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
}

.dark .phase-report .markdown-body :not(pre) > code {
  background: rgba(255, 255, 255, 0.1);
}

.phase-report .markdown-body blockquote {
  border-left: 3px solid #8b5cf6;
  padding-left: 0.75rem;
  margin: 0.5em 0;
  color: #64748b;
}

.dark .phase-report .markdown-body blockquote {
  color: #94a3b8;
}

.phase-report .markdown-body a {
  color: #8b5cf6;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.phase-report .markdown-body a:hover {
  color: #7c3aed;
}

.phase-report .markdown-body strong {
  font-weight: 600;
}

.phase-report .markdown-body hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 0.75em 0;
}

.dark .phase-report .markdown-body hr {
  border-top-color: #475569;
}

.phase-report .markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.75em 0;
  font-size: 0.85em;
}

.phase-report .markdown-body th,
.phase-report .markdown-body td {
  border: 1px solid #e2e8f0;
  padding: 0.5rem 0.75rem;
  text-align: left;
}

.dark .phase-report .markdown-body th,
.dark .phase-report .markdown-body td {
  border-color: #475569;
}

.phase-report .markdown-body th {
  background: #f8fafc;
  font-weight: 600;
}

.dark .phase-report .markdown-body th {
  background: #334155;
}

.phase-report .markdown-body img {
  max-width: 100%;
  border-radius: 0.5rem;
  margin: 0.5em 0;
}
</style>
