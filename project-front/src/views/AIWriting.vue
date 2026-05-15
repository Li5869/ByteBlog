<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {marked} from 'marked'
import {toast} from '@/utils/toast'
import {aiApi, articleApi, categoryApi, tagApi, uploadApi} from '@/utils/request'

const router = useRouter()
const route = useRoute()

// ==================== 阶段状态 ====================
// idle → planning → plan_ready → executing → reflecting → finalize_ready → done
const phase = ref('idle')

// ==================== Step 进度 ====================
const steps = [
  { key: 'plan', label: '规划', icon: '📋', desc: '分析需求生成计划' },
  { key: 'execute', label: '创作', icon: '✍️', desc: '标题/摘要/内容/标签' },
  { key: 'reflect', label: '反思', icon: '🔍', desc: '质量评估与微调' },
  { key: 'finalize', label: '完成', icon: '🎉', desc: '存草稿或发布' },
]

const currentStepIndex = computed(() => {
  const map = { idle: -1, planning: 0, plan_ready: 0, executing: 1, reflecting: 2, finalize_ready: 3, done: 3 }
  return map[phase.value] ?? -1
})

// ==================== 写作输入 ====================
const userInput = ref('')

// ==================== 任务ID ====================
const taskId = ref('')

// ==================== Plan 数据 ====================
const plan = ref(null)
const planFeedback = ref('')

// ==================== Execute 进度（支持多步并行）= ====================
const executeStepLabels = { title: '生成标题', summary: '撰写摘要', content: '撰写内容', tags: '选择标签' }

// 多步状态：每个步骤独立跟踪进行中/已完成，支持 title+tags 并行执行
const executeSteps = ref({
  title: { status: 'pending', content: '' },
  summary: { status: 'pending', content: '' },
  content: { status: 'pending', content: '' },
  tags: { status: 'pending', content: '' },
})

// 令牌步骤队列：当多个 phase 事件先于 token 到达时，
// 将步骤名入队，token 到达时出队确定所属步骤
const tokenStepQueue = ref([])

const streamingContent = ref('')

// 计算当前激活的步骤列表（可能有多个并行）
const activeSteps = computed(() =>
  Object.entries(executeSteps.value)
    .filter(([, v]) => v.status === 'active')
    .map(([k]) => k)
)
// 计算已完成的步骤列表
const completedSteps = computed(() =>
  Object.entries(executeSteps.value)
    .filter(([, v]) => v.status === 'completed')
    .map(([k]) => k)
)

// ==================== 写作结果 ====================
const writingResult = ref(null)

// ==================== 反思结果 ====================
const reflection = ref(null)

// ==================== 最终操作 ====================
const isSubmitting = ref(false)

// ==================== 封面上传 ====================
const coverInputRef = ref(null)
const coverUrl = ref('')
const coverFileName = ref('')
const isUploadingCover = ref(false)
const isDraggingCover = ref(false)

// ==================== SSE 连接管理 ====================
let currentStreamReader = null
let currentAbortController = null

// ==================== 分类和标签数据 ====================
const categories = ref([])
const tags = ref([])

// ==================== 工具方法 ====================

const escapeHtml = (str) => str
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#039;')

const unescapeHtml = (str) => str
  .replace(/&lt;/g, '<')
  .replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"')
  .replace(/&#039;/g, "'")
  .replace(/&amp;/g, '&')

// 重置执行步骤状态（用于开始新写作/批准计划/重新规划）
const resetExecuteSteps = () => {
  executeSteps.value = {
    title: { status: 'pending', content: '' },
    summary: { status: 'pending', content: '' },
    content: { status: 'pending', content: '' },
    tags: { status: 'pending', content: '' },
  }
  tokenStepQueue.value = []
}

// ==================== 加载分类和标签 ====================

const loadCategoriesAndTags = async () => {
  try {
    const [categoriesData, tagsData] = await Promise.all([
      categoryApi.getCategories(),
      tagApi.getTags()
    ])
    categories.value = categoriesData || []
    tags.value = tagsData || []
  } catch (error) {
    console.error('加载分类和标签失败:', error)
  }
}

// ==================== SSE 事件处理 ====================

const handleSSEEvent = (event) => {
  switch (event.type) {
    case 'phase':
      handlePhaseEvent(event.data)
      break
    case 'plan_ready':
      handlePlanReadyEvent(event.data)
      break
    case 'token':
      handleTokenEvent(event.data)
      break
    case 'reflection_result':
      handleReflectionEvent(event.data)
      break
    case 'finalize_ready':
      handleFinalizeEvent(event.data)
      break
    case 'done':
      handleDoneEvent()
      break
    case 'error':
      handleErrorEvent(event.data)
      break
  }
}

const handlePhaseEvent = (data) => {
  const phaseMap = {
    'planning': 'planning',
    'executing': 'executing',
    'reflecting': 'reflecting',
    'finalizing': 'finalize_ready'
  }
  
  if (phaseMap[data.phase]) {
    phase.value = phaseMap[data.phase]
  }
  
  // 多步并行支持：收到 step 事件时标记该步骤为进行中，
  // 并将步骤名加入 tokenStepQueue，后续 token 到达时通过出队确定所属步骤
  if (data.step && executeSteps.value[data.step]) {
    executeSteps.value[data.step].status = 'active'
    tokenStepQueue.value.push(data.step)
  }
}

const handlePlanReadyEvent = (data) => {
  phase.value = 'plan_ready'
  plan.value = data
  toast.success('写作计划已生成')
}

const handleTokenEvent = (data) => {
  if (typeof data === 'string') {
    streamingContent.value += data
  }
  // 从 tokenStepQueue 出队确定当前 token 所属步骤，标记为已完成并存储内容
  const step = tokenStepQueue.value.shift()
  if (step && executeSteps.value[step]) {
    executeSteps.value[step].status = 'completed'
    executeSteps.value[step].content = data
  }
}

const handleReflectionEvent = (data) => {
  reflection.value = data
}

const handleFinalizeEvent = (data) => {
  phase.value = 'finalize_ready'
  writingResult.value = data
  toast.success('文章创作完成')
}

const handleDoneEvent = () => {
  if (currentStreamReader) {
    currentStreamReader.cancel()
    currentStreamReader = null
  }
}

const handleErrorEvent = (data) => {
  toast.error(data || '写作过程中发生错误')
  phase.value = 'idle'
  if (currentStreamReader) {
    currentStreamReader.cancel()
    currentStreamReader = null
  }
}

// ==================== SSE 流读取 ====================

const readSSEStream = async (stream) => {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  currentStreamReader = reader

  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }

      const text = decoder.decode(value, { stream: true })
      buffer += text
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        if (trimmed.startsWith('data:')) {
          try {
            const jsonStr = trimmed.substring(5).trim()
            if (jsonStr) {
              const event = JSON.parse(jsonStr)
              handleSSEEvent(event)
            }
          } catch (e) {
            console.warn('[SSE] 解析事件失败:', e, line)
          }
        }
      }
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('[SSE] 流读取错误:', error)
      toast.error('连接中断，请重试')
    }
  } finally {
    reader.releaseLock()
    currentStreamReader = null
  }
}

// ==================== 操作方法 ====================

const handleStartWriting = async () => {
  if (!userInput.value.trim()) {
    toast.error('请输入写作需求')
    return
  }

  try {
    phase.value = 'planning'
    streamingContent.value = ''
    writingResult.value = null
    reflection.value = null
    plan.value = null
    resetExecuteSteps()

    const createResult = await aiApi.createWritingTask(userInput.value)
    taskId.value = createResult.taskId

    const stream = await aiApi.streamWriting(createResult.taskId)
    await readSSEStream(stream)

  } catch (error) {
    console.error('启动写作任务失败:', error)
    toast.error(error.message || '启动写作任务失败')
    phase.value = 'idle'
  }
}

const handleApprovePlan = async () => {
  try {
    phase.value = 'executing'
    streamingContent.value = ''
    resetExecuteSteps()

    await aiApi.resumeWriting(taskId.value, 'approve')

    const stream = await aiApi.streamWriting(taskId.value)
    await readSSEStream(stream)

  } catch (error) {
    console.error('批准计划失败:', error)
    toast.error(error.message || '批准计划失败')
    phase.value = 'plan_ready'
  }
}

const handleRevisePlan = async () => {
  if (!planFeedback.value.trim()) {
    toast.error('请输入修改意见')
    return
  }

  try {
    phase.value = 'planning'
    
    await aiApi.resumeWriting(taskId.value, 'revise', planFeedback.value)
    
    const stream = await aiApi.streamWriting(taskId.value)
    await readSSEStream(stream)
    
    planFeedback.value = ''
    toast.success('计划已更新')

  } catch (error) {
    console.error('修改计划失败:', error)
    toast.error(error.message || '修改计划失败')
    phase.value = 'plan_ready'
  }
}

const handleCancelPlan = () => {
  phase.value = 'idle'
  plan.value = null
  planFeedback.value = ''
  taskId.value = ''
  
  if (currentStreamReader) {
    currentStreamReader.cancel()
    currentStreamReader = null
  }
}

const handleSaveDraft = async () => {
  if (!writingResult.value) return

  isSubmitting.value = true
  try {
    const category = categories.value.find(c => c.name === writingResult.value.categoryName)
    
    const articleId = await articleApi.createArticle({
      title: writingResult.value.title,
      summary: writingResult.value.summary,
      content: writingResult.value.content,
      cover: coverUrl.value,
      categoryId: category?.id || null,
      tagIds: writingResult.value.tagIds || null,
      tagNames: writingResult.value.tagNames || null,
      status: 0, // 草稿
      taskId: taskId.value || null
    })

    toast.success('草稿已保存')
    phase.value = 'done'
  } catch (error) {
    console.error('保存草稿失败:', error)
    toast.error(error.message || '保存草稿失败')
  } finally {
    isSubmitting.value = false
  }
}

const handlePublish = async () => {
  if (!writingResult.value) return

  isSubmitting.value = true
  try {
    const category = categories.value.find(c => c.name === writingResult.value.categoryName)
    
    const articleId = await articleApi.createArticle({
      title: writingResult.value.title,
      summary: writingResult.value.summary,
      content: writingResult.value.content,
      cover: coverUrl.value,
      categoryId: category?.id || null,
      tagIds: writingResult.value.tagIds || null,
      tagNames: writingResult.value.tagNames || null,
      status: 1, // 发布
      taskId: taskId.value || null
    })

    toast.success('文章已发布')
    phase.value = 'done'
  } catch (error) {
    console.error('发布文章失败:', error)
    toast.error(error.message || '发布文章失败')
  } finally {
    isSubmitting.value = false
  }
}

const handleReviseFromStart = () => {
  phase.value = 'idle'
  plan.value = null
  writingResult.value = null
  reflection.value = null
  streamingContent.value = ''
  planFeedback.value = ''
  coverUrl.value = ''
  coverFileName.value = ''
  taskId.value = ''
  resetExecuteSteps()
  
  if (currentStreamReader) {
    currentStreamReader.cancel()
    currentStreamReader = null
  }
}

const triggerCoverInput = () => {
  coverInputRef.value?.click()
}

const handleCoverUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    toast.error('请上传图片文件')
    return
  }
  
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过5MB')
    return
  }
  
  isUploadingCover.value = true
  coverFileName.value = file.name
  
  try {
    const url = await uploadApi.uploadFile(file)
    coverUrl.value = url
    toast.success('封面上传成功')
  } catch (error) {
    console.error('上传封面失败:', error)
    toast.error('封面上传失败，请重试')
    coverUrl.value = ''
    coverFileName.value = ''
  } finally {
    isUploadingCover.value = false
    if (coverInputRef.value) {
      coverInputRef.value.value = ''
    }
  }
}

const handleCoverDrop = async (event) => {
  event.preventDefault()
  isDraggingCover.value = false
  
  const file = event.dataTransfer.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    toast.error('请上传图片文件')
    return
  }
  
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过5MB')
    return
  }
  
  isUploadingCover.value = true
  coverFileName.value = file.name
  
  try {
    const url = await uploadApi.uploadFile(file)
    coverUrl.value = url
    toast.success('封面上传成功')
  } catch (error) {
    console.error('上传封面失败:', error)
    toast.error('封面上传失败，请重试')
    coverUrl.value = ''
    coverFileName.value = ''
  } finally {
    isUploadingCover.value = false
  }
}

const handleCoverDragOver = (event) => {
  event.preventDefault()
  isDraggingCover.value = true
}

const handleCoverDragLeave = () => {
  isDraggingCover.value = false
}

const removeCover = () => {
  coverUrl.value = ''
  coverFileName.value = ''
  if (coverInputRef.value) {
    coverInputRef.value.value = ''
  }
}

const goBack = () => {
  router.back()
}

// ==================== Markdown 渲染 ====================
const renderMarkdown = (content) => {
  if (!content) return ''
  return marked.parse(unescapeHtml(content))
}

// ==================== 从任务列表恢复任务 ====================

const resumeFromTask = async (rawTaskId) => {
  try {
    const detail = await aiApi.getWritingTaskDetail(rawTaskId)
    if (!detail) {
      toast.error('任务不存在或无权访问')
      return
    }

    // 设置 ref 中的 taskId
    taskId.value = detail.id

    // 根据状态跳转到对应阶段
    if (detail.status === 'finalized') {
      if (detail.finalAction) {
        // 已发布/存草稿 → 跳转文章页
        if (detail.articleId) {
          router.replace(`/article/${detail.articleId}`)
        } else {
          toast.info('该任务已完成')
          phase.value = 'done'
        }
      } else {
        // AI 写完但用户未操作 → 获取草稿数据展示 finalize 面板
        toast.success('文章已就绪，请选择发布或存草稿')
        try {
          const draft = await aiApi.getWritingTaskDraft(rawTaskId)
          if (draft) {
            // WritingDraft 实体中的 tagNames/tagIds 是逗号分隔字符串，需要转为数组
            const newTagNames = (draft.tagNames || draft.tag_names || '')
              .split(',')
              .map(s => s.trim())
              .filter(Boolean)

            const existingTagIds = (draft.tagIds || draft.tag_ids || '')
              .split(',')
              .map(s => s.trim())
              .filter(Boolean)

            // 查询已有标签的名称
            let existingTagNames = []
            if (existingTagIds.length > 0) {
              try {
                const tags = await tagApi.batchGetTags(existingTagIds.join(','))
                existingTagNames = (tags || []).map(t => t.name).filter(Boolean)
              } catch (e) {
                console.warn('[Resume] 查询标签名称失败:', e)
              }
            }

            writingResult.value = {
              title: draft.title || '',
              summary: draft.summary || '',
              content: draft.content || '',
              categoryName: draft.category_name || draft.categoryName,
              categoryId: draft.category_id || draft.categoryId,
              tagNames: [...existingTagNames, ...newTagNames],
              tagIds: existingTagIds
            }
          }
        } catch (e) {
          console.warn('[Resume] 获取草稿失败:', e)
        }
        phase.value = 'finalize_ready'
      }
      return
    }

    if (detail.status === 'plan_ready' && detail.topic) {
      // 计划已就绪，展示计划面板
      plan.value = detail
      phase.value = 'plan_ready'
      toast.success('已加载写作计划')
      return
    }

    if (detail.status === 'error') {
      phase.value = 'idle'
      toast.error('该任务已失败，请重新创建')
      return
    }

    // planning / executing / reflecting 状态 - 连接 SSE 流查看实时进度
    const phaseMap = { planning: 'planning', executing: 'executing', reflecting: 'reflecting' }
    phase.value = phaseMap[detail.status] || 'idle'

    // 连接 SSE 流，获取实时进度
    try {
      const stream = await aiApi.streamWriting(rawTaskId)
      await readSSEStream(stream)
    } catch (e) {
      console.warn('[Resume] SSE 流连接失败，任务可能已结束:', e)
    }

  } catch (error) {
    console.error('恢复任务失败:', error)
    toast.error(error.message || '恢复任务失败')
  }
}

// ==================== 生命周期 ====================

onMounted(async () => {
  document.title = 'AI智能写作 - 个人博客'
  loadCategoriesAndTags()

  // 检查是否有 taskId 查询参数（从任务列表页跳转过来）
  const queryTaskId = route.query.taskId
  if (queryTaskId) {
    await resumeFromTask(queryTaskId)
  }
})

onUnmounted(() => {
  if (currentStreamReader) {
    currentStreamReader.cancel()
    currentStreamReader = null
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <!-- 顶部 Banner -->
    <div class="bg-gradient-to-r from-violet-600 via-purple-600 to-indigo-600 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 relative">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button 
              @click="goBack"
              class="p-2.5 bg-white/20 backdrop-blur-sm text-white rounded-xl hover:bg-white/30 transition-all border border-white/20"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold text-white flex items-center gap-2">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                AI 智能写作助手
              </h1>
              <p class="text-white/70 text-sm mt-0.5">描述需求，AI 帮你完成从规划到发布的全流程</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      <!-- Step 进度条 -->
      <div v-if="phase !== 'idle'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-5">
        <div class="flex items-center justify-between">
          <template v-for="(step, idx) in steps" :key="step.key">
            <!-- Step 节点 -->
            <div class="flex items-center gap-3">
              <div 
                :class="[
                  'w-10 h-10 rounded-xl flex items-center justify-center text-lg transition-all duration-300',
                  idx < currentStepIndex ? 'bg-green-100 dark:bg-green-900/30' : 
                  idx === currentStepIndex ? 'bg-violet-100 dark:bg-violet-900/30 ring-2 ring-violet-400 ring-offset-2 dark:ring-offset-gray-800' : 
                  'bg-gray-100 dark:bg-gray-700'
                ]"
              >
                <span v-if="idx < currentStepIndex">✅</span>
                <span v-else>{{ step.icon }}</span>
              </div>
              <div class="hidden sm:block">
                <p :class="['text-sm font-semibold', idx <= currentStepIndex ? 'text-gray-900 dark:text-white' : 'text-gray-400 dark:text-gray-500']">
                  {{ step.label }}
                </p>
                <p class="text-xs text-gray-400 dark:text-gray-500">{{ step.desc }}</p>
              </div>
            </div>
            <!-- 连接线 -->
            <div v-if="idx < steps.length - 1" class="flex-1 mx-2">
              <div 
                :class="[
                  'h-0.5 rounded-full transition-all duration-500',
                  idx < currentStepIndex ? 'bg-green-400' : 'bg-gray-200 dark:bg-gray-700'
                ]"
              ></div>
            </div>
          </template>
        </div>
      </div>

      <!-- ==================== 阶段1：写作需求输入 ==================== -->
      <div v-if="phase === 'idle'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
        <div class="text-center mb-6">
          <div class="w-16 h-16 mx-auto bg-violet-100 dark:bg-violet-900/30 rounded-2xl flex items-center justify-center mb-4">
            <svg class="w-8 h-8 text-violet-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </div>
          <h2 class="text-lg font-bold text-gray-900 dark:text-white">描述你的写作需求</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">AI 会帮你规划、撰写、审核，最终产出高质量文章</p>
        </div>
        
        <div class="max-w-2xl mx-auto">
          <div class="relative">
            <textarea
              v-model="userInput"
              rows="4"
              class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-600 rounded-xl text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-violet-400 focus:border-transparent resize-none transition-all"
              placeholder="例如：帮我写一篇关于 Vue3 组合式 API 的入门教程，面向有 Vue2 基础的前端开发者..."
            ></textarea>
          </div>
          
          <div class="mt-4 flex items-center justify-between">
            <p class="text-xs text-gray-400 dark:text-gray-500">💡 描述越具体，AI 产出的文章越贴合你的需求</p>
            <button
              @click="handleStartWriting"
              class="px-6 py-2.5 bg-gradient-to-r from-violet-500 to-purple-600 text-white rounded-xl hover:from-violet-600 hover:to-purple-700 transition-all text-sm font-semibold shadow-lg shadow-violet-500/20 flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              开始写作
            </button>
          </div>
        </div>
      </div>

      <!-- ==================== 阶段2：Planning 旋转 ==================== -->
      <div v-if="phase === 'planning'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <div class="flex flex-col items-center justify-center py-8">
          <div class="relative w-16 h-16 mb-4">
            <div class="absolute inset-0 rounded-full border-4 border-violet-200 dark:border-violet-800 opacity-30"></div>
            <div class="absolute inset-0 rounded-full border-4 border-transparent border-t-violet-500 animate-spin"></div>
          </div>
          <p class="text-lg font-semibold text-gray-800 dark:text-white">正在分析你的写作需求...</p>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">AI 正在生成写作计划，通常需要 10-20 秒</p>
        </div>
      </div>

      <!-- ==================== 阶段3：Plan 面板 ==================== -->
      <div v-if="phase === 'plan_ready' && plan" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
        <div class="bg-gradient-to-r from-violet-50 to-purple-50 dark:from-violet-900/20 dark:to-purple-900/20 px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <span>📋</span> 写作计划
          </h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">审核 AI 生成的计划，确认后开始创作</p>
        </div>

        <div class="p-6 space-y-5">
          <!-- 主题 -->
          <div>
            <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">主题</label>
            <p class="mt-1 text-base font-semibold text-gray-900 dark:text-white">{{ plan.topic }}</p>
          </div>

          <!-- 目标读者 + 风格 -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">目标读者</label>
              <p class="mt-1 text-sm text-gray-700 dark:text-gray-300">{{ plan.targetAudience || plan.target_audience }}</p>
            </div>
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">写作风格</label>
              <span class="inline-block mt-1 px-2.5 py-1 bg-violet-100 dark:bg-violet-900/30 text-violet-700 dark:text-violet-300 rounded-lg text-xs font-medium">{{ plan.writingStyle || plan.writing_style }}</span>
            </div>
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">预计篇幅</label>
              <span class="inline-block mt-1 px-2.5 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded-lg text-xs font-medium">{{ plan.estimatedLength || plan.estimated_length }}</span>
            </div>
          </div>

          <!-- 核心要点 -->
          <div>
            <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">核心要点</label>
            <div class="mt-2 space-y-1.5">
              <div v-for="(point, idx) in (plan.keyPoints || plan.key_points)" :key="idx" class="flex items-start gap-2">
                <span class="w-5 h-5 rounded-md bg-violet-100 dark:bg-violet-900/30 text-violet-600 dark:text-violet-400 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">{{ idx + 1 }}</span>
                <p class="text-sm text-gray-700 dark:text-gray-300">{{ point }}</p>
              </div>
            </div>
          </div>

          <!-- 文章结构 -->
          <div>
            <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">文章结构</label>
            <div class="mt-2 flex flex-wrap gap-2">
              <template v-for="(section, idx) in plan.structure" :key="idx">
                <div class="flex items-center gap-1.5">
                  <span class="w-6 h-6 rounded-lg bg-gradient-to-r from-violet-500 to-purple-500 text-white text-xs font-bold flex items-center justify-center">{{ idx + 1 }}</span>
                  <span class="text-sm text-gray-700 dark:text-gray-300">{{ section }}</span>
                  <svg v-if="idx < plan.structure.length - 1" class="w-4 h-4 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </template>
            </div>
          </div>

          <!-- 参考关键词 -->
          <div>
            <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">参考关键词</label>
            <div class="mt-2 flex flex-wrap gap-2">
              <span v-for="kw in (plan.referenceKeywords || plan.reference_keywords)" :key="kw" class="px-2.5 py-1 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded-lg text-xs">{{ kw }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="pt-4 border-t border-gray-100 dark:border-gray-700 flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <button
              @click="handleApprovePlan"
              class="flex-1 px-5 py-2.5 bg-gradient-to-r from-violet-500 to-purple-600 text-white rounded-xl hover:from-violet-600 hover:to-purple-700 transition-all text-sm font-semibold shadow-lg shadow-violet-500/20 flex items-center justify-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              批准计划，开始创作
            </button>
            <button
              @click="planFeedback ? handleRevisePlan() : (planFeedback = ' ')"
              class="flex-1 px-5 py-2.5 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-600 transition-all text-sm font-medium flex items-center justify-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              修改计划
            </button>
            <button
              @click="handleCancelPlan"
              class="px-5 py-2.5 text-gray-500 dark:text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors text-sm font-medium flex items-center justify-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              取消
            </button>
          </div>

          <!-- 修改意见输入 -->
          <div v-if="planFeedback !== ''" class="pt-3 border-t border-gray-100 dark:border-gray-700">
            <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2 block">修改意见</label>
            <textarea
              v-model="planFeedback"
              rows="3"
              class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-600 rounded-xl text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-violet-400 focus:border-transparent resize-none transition-all text-sm"
              placeholder="例如：请增加性能优化方面的内容..."
            ></textarea>
          </div>
        </div>
      </div>

      <!-- ==================== 阶段4：Executing 进度 ==================== -->
      <div v-if="phase === 'executing'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
        <div class="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <span>✍️</span> 正在创作
          </h3>
        </div>
        <div class="p-6">
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div v-for="step in ['title', 'summary', 'content', 'tags']" :key="step"
              :class="[
                'flex items-center gap-2.5 px-4 py-3 rounded-xl transition-all',
                executeSteps[step].status === 'active' ? 'bg-violet-50 dark:bg-violet-900/20 ring-2 ring-violet-300 dark:ring-violet-600' : 
                executeSteps[step].status === 'completed' ? 'bg-green-50 dark:bg-green-900/20' : 
                'bg-gray-50 dark:bg-gray-900/30'
              ]"
            >
              <!-- 进行中：旋转动画 -->
              <div v-if="executeSteps[step].status === 'active'" class="relative w-5 h-5">
                <div class="absolute inset-0 rounded-full border-2 border-violet-300 dark:border-violet-600 opacity-30"></div>
                <div class="absolute inset-0 rounded-full border-2 border-transparent border-t-violet-500 animate-spin"></div>
              </div>
              <!-- 已完成：绿色对勾 -->
              <svg v-else-if="executeSteps[step].status === 'completed'" class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              <!-- 待处理：空心圆 -->
              <div v-else class="w-5 h-5 rounded-full border-2 border-gray-300 dark:border-gray-600"></div>
              <span :class="['text-sm font-medium', executeSteps[step].status === 'active' ? 'text-violet-700 dark:text-violet-300' : executeSteps[step].status === 'completed' ? 'text-green-700 dark:text-green-300' : 'text-gray-500 dark:text-gray-400']">
                {{ executeStepLabels[step] }}
              </span>
            </div>
          </div>
          <div class="mt-4 text-center">
            <p class="text-sm text-gray-500 dark:text-gray-400">
              <!-- 并行执行时展示多个进行中的步骤 -->
              正在
              <template v-for="(step, idx) in activeSteps" :key="step">
                <template v-if="idx > 0 && idx === activeSteps.length - 1">和</template>
                <template v-else-if="idx > 0">、</template>
                <span class="font-medium text-violet-600 dark:text-violet-400">{{ executeStepLabels[step] }}</span>
              </template>
              <template v-if="activeSteps.length === 0">处理中</template>
              ...
            </p>
          </div>
        </div>
      </div>

      <!-- ==================== 阶段5：Reflecting 旋转 ==================== -->
      <div v-if="phase === 'reflecting'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <div class="flex flex-col items-center justify-center py-8">
          <div class="relative w-16 h-16 mb-4">
            <div class="absolute inset-0 rounded-full border-4 border-blue-200 dark:border-blue-800 opacity-30"></div>
            <div class="absolute inset-0 rounded-full border-4 border-transparent border-t-blue-500 animate-spin"></div>
          </div>
          <p class="text-lg font-semibold text-gray-800 dark:text-white">正在审核文章质量...</p>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">AI 正在从内容完整性、结构合理性、表达质量等维度评估</p>
        </div>
      </div>

      <!-- ==================== 阶段6：Finalize 面板 ==================== -->
      <div v-if="phase === 'finalize_ready' && writingResult" class="space-y-6">
        <!-- 提示信息 -->
        <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl p-4 flex items-start gap-3">
          <svg class="w-5 h-5 text-amber-500 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <p class="text-sm font-medium text-amber-800 dark:text-amber-200">温馨提示</p>
            <p class="text-sm text-amber-700 dark:text-amber-300 mt-0.5">若要更改具体内容，可保存草稿后自行更改</p>
          </div>
        </div>

        <!-- 文章预览 -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
          <div class="bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 px-6 py-4 border-b border-gray-100 dark:border-gray-700">
            <h3 class="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <span>📄</span> 写作成果
            </h3>
          </div>

          <div class="p-6 space-y-5">
            <!-- 标题 -->
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">标题</label>
              <p class="mt-1 text-xl font-bold text-gray-900 dark:text-white">{{ writingResult.title }}</p>
            </div>

            <!-- 摘要 -->
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">摘要</label>
              <p class="mt-1 text-sm text-gray-600 dark:text-gray-300 leading-relaxed">{{ writingResult.summary }}</p>
            </div>

            <!-- 标签和分类 -->
            <div class="flex flex-wrap items-center gap-3">
              <div>
                <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">分类</label>
                <span class="ml-2 inline-block px-3 py-1 bg-violet-100 dark:bg-violet-900/30 text-violet-700 dark:text-violet-300 rounded-lg text-sm font-medium">{{ writingResult.categoryName || writingResult.category_name }}</span>
              </div>
              <div class="flex flex-wrap items-center gap-1.5">
                <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">标签</label>
                <span v-for="tag in (writingResult.tagNames || writingResult.tag_names)" :key="tag" class="px-2.5 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded-lg text-xs font-medium">{{ tag }}</span>
              </div>
            </div>

            <!-- 封面上传 -->
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 block">封面图片（可选）</label>
              <input 
                ref="coverInputRef"
                type="file"
                accept="image/*"
                @change="handleCoverUpload"
                class="hidden"
              />
              <div v-if="!coverUrl">
                <div 
                  @click="triggerCoverInput"
                  @drop="handleCoverDrop"
                  @dragover="handleCoverDragOver"
                  @dragleave="handleCoverDragLeave"
                  class="border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all"
                  :class="isDraggingCover 
                    ? 'border-violet-500 bg-violet-50 dark:bg-violet-900/20' 
                    : 'border-gray-300 dark:border-gray-600 hover:border-violet-500 dark:hover:border-violet-500 hover:bg-gray-50 dark:hover:bg-gray-700/30'"
                >
                  <div v-if="isUploadingCover" class="flex flex-col items-center">
                    <div class="relative w-10 h-10 mb-3">
                      <div class="absolute inset-0 rounded-full border-3 border-violet-200 dark:border-violet-800 opacity-30"></div>
                      <div class="absolute inset-0 rounded-full border-3 border-transparent border-t-violet-500 animate-spin"></div>
                    </div>
                    <p class="text-sm text-gray-500 dark:text-gray-400">正在上传...</p>
                  </div>
                  <div v-else>
                    <div class="w-12 h-12 mx-auto mb-3 rounded-xl bg-gradient-to-br from-violet-400 to-purple-500 flex items-center justify-center shadow-lg shadow-violet-500/20">
                      <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                    </div>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">点击或拖拽上传封面</p>
                    <p class="text-xs text-gray-400">支持 JPG、PNG、GIF，最大 5MB</p>
                  </div>
                </div>
              </div>
              <div v-else class="relative group inline-block">
                <img 
                  :src="coverUrl" 
                  alt="封面预览"
                  class="w-full max-w-md h-40 object-cover rounded-xl border border-gray-200 dark:border-gray-700 shadow-lg"
                />
                <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl flex items-center justify-center">
                  <button 
                    @click="removeCover"
                    class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors font-medium flex items-center gap-2"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    删除封面
                  </button>
                </div>
                <p v-if="coverFileName" class="text-sm text-gray-500 dark:text-gray-400 mt-2 flex items-center gap-2">
                  <svg class="w-4 h-4 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                  {{ coverFileName }}
                </p>
              </div>
            </div>

            <!-- 正文预览 -->
            <div>
              <label class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2 block">正文预览</label>
              <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5 max-h-96 overflow-y-auto border border-gray-100 dark:border-gray-700">
                <div class="markdown-body" v-html="renderMarkdown(writingResult.content)"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 反思评估 -->
        <div v-if="reflection" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
          <div class="bg-gradient-to-r from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/20 px-6 py-4 border-b border-gray-100 dark:border-gray-700">
            <h3 class="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <span>🔍</span> 质量评估
            </h3>
          </div>

          <div class="p-6 space-y-5">
            <!-- 总分 -->
            <div class="flex items-center gap-4">
              <div class="relative w-20 h-20">
                <svg class="w-20 h-20 -rotate-90" viewBox="0 0 80 80">
                  <circle cx="40" cy="40" r="34" fill="none" :stroke="reflection.score >= 7 ? '#10b981' : '#f59e0b'" stroke-width="6" opacity="0.2" />
                  <circle cx="40" cy="40" r="34" fill="none" :stroke="reflection.score >= 7 ? '#10b981' : '#f59e0b'" stroke-width="6" :stroke-dasharray="`${reflection.score / 10 * 213.6} 213.6`" stroke-linecap="round" />
                </svg>
                <div class="absolute inset-0 flex items-center justify-center">
                  <span class="text-xl font-bold" :class="reflection.score >= 7 ? 'text-green-600 dark:text-green-400' : 'text-amber-600 dark:text-amber-400'">{{ reflection.score }}</span>
                </div>
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900 dark:text-white">综合评分</p>
                <p class="text-xs text-gray-500 dark:text-gray-400">{{ reflection.score >= 7 ? '质量达标，可以发布' : '建议优化后再发布' }}</p>
              </div>
            </div>

            <!-- 分项评分 -->
            <div class="grid grid-cols-5 gap-3">
              <div v-for="(item, key) in [
                { label: '完整性', value: reflection.completeness },
                { label: '结构', value: reflection.structure },
                { label: '表达', value: reflection.expression },
                { label: '实用性', value: reflection.practicality },
                { label: '格式', value: reflection.format }
              ]" :key="key" class="text-center">
                <div class="text-lg font-bold" :class="item.value >= 8 ? 'text-green-600 dark:text-green-400' : item.value >= 6 ? 'text-amber-600 dark:text-amber-400' : 'text-red-600 dark:text-red-400'">
                  {{ item.value }}
                </div>
                <div class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ item.label }}</div>
              </div>
            </div>

            <!-- 优缺点 -->
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label class="text-xs font-semibold text-green-600 dark:text-green-400 uppercase tracking-wider">✅ 优点</label>
                <ul class="mt-1.5 space-y-1">
                  <li v-for="s in reflection.strengths" :key="s" class="text-sm text-gray-600 dark:text-gray-300 flex items-start gap-1.5">
                    <span class="text-green-400 mt-1">•</span>{{ s }}
                  </li>
                </ul>
              </div>
              <div>
                <label class="text-xs font-semibold text-amber-600 dark:text-amber-400 uppercase tracking-wider">⚠️ 不足</label>
                <ul class="mt-1.5 space-y-1">
                  <li v-for="w in reflection.weaknesses" :key="w" class="text-sm text-gray-600 dark:text-gray-300 flex items-start gap-1.5">
                    <span class="text-amber-400 mt-1">•</span>{{ w }}
                  </li>
                </ul>
              </div>
              <div>
                <label class="text-xs font-semibold text-blue-600 dark:text-blue-400 uppercase tracking-wider">💡 建议</label>
                <ul class="mt-1.5 space-y-1">
                  <li v-for="s in reflection.suggestions" :key="s" class="text-sm text-gray-600 dark:text-gray-300 flex items-start gap-1.5">
                    <span class="text-blue-400 mt-1">•</span>{{ s }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- 最终操作 -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-5">
          <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <button
              @click="handleSaveDraft"
              :disabled="isSubmitting"
              class="flex-1 px-5 py-3 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-200 dark:hover:bg-gray-600 transition-all text-sm font-semibold flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
              </svg>
              存为草稿
            </button>
            <button
              @click="handlePublish"
              :disabled="isSubmitting"
              class="flex-1 px-5 py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-xl hover:from-green-600 hover:to-emerald-700 transition-all text-sm font-semibold shadow-lg shadow-green-500/20 flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
              </svg>
              发布文章
            </button>
            <button
              @click="handleReviseFromStart"
              class="flex-1 px-5 py-3 border border-gray-200 dark:border-gray-600 text-gray-500 dark:text-gray-400 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-all text-sm font-medium flex items-center justify-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              重新规划
            </button>
          </div>
        </div>
      </div>

      <!-- ==================== 阶段7：完成 ==================== -->
      <div v-if="phase === 'done'" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <div class="flex flex-col items-center justify-center py-8 text-center">
          <div class="w-20 h-20 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mb-4">
            <svg class="w-10 h-10 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2">🎉 文章处理完成</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">你的文章已成功保存</p>
          <div class="flex items-center gap-3">
            <button
              @click="router.push('/mine')"
              class="px-5 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-200 dark:hover:bg-gray-600 transition-all text-sm font-medium"
            >
              前往我的主页
            </button>
            <button
              @click="handleReviseFromStart"
              class="px-5 py-2.5 bg-gradient-to-r from-violet-500 to-purple-600 text-white rounded-xl hover:from-violet-600 hover:to-purple-700 transition-all text-sm font-semibold shadow-lg shadow-violet-500/20"
            >
              再写一篇
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* Markdown 预览样式（非 scoped，v-html 需要） */
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
}
.dark .markdown-body {
  color: #d1d5db;
}
.markdown-body h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #e5e7eb;
}
.dark .markdown-body h2 {
  border-bottom-color: #374151;
}
.markdown-body h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-top: 1.2em;
  margin-bottom: 0.4em;
}
.markdown-body p {
  margin-bottom: 0.8em;
}
.markdown-body ul, .markdown-body ol {
  padding-left: 1.5em;
  margin-bottom: 0.8em;
}
.markdown-body li {
  margin-bottom: 0.3em;
}
.markdown-body code {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.85em;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  background: #f3f4f6;
}
.dark .markdown-body code {
  background: #1f2937;
}
.markdown-body pre {
  white-space: pre;
  overflow-x: auto;
  padding: 1em;
  border-radius: 8px;
  background: #f8f9fa;
  margin-bottom: 1em;
}
.dark .markdown-body pre {
  background: #111827;
}
.markdown-body pre code {
  background: transparent;
  padding: 0;
}
.markdown-body blockquote {
  border-left: 3px solid #8b5cf6;
  padding-left: 1em;
  margin: 1em 0;
  color: #6b7280;
}
.dark .markdown-body blockquote {
  color: #9ca3af;
}
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1em;
}
.markdown-body th, .markdown-body td {
  border: 1px solid #e5e7eb;
  padding: 0.5em 0.8em;
  text-align: left;
}
.dark .markdown-body th, .dark .markdown-body td {
  border-color: #374151;
}
.markdown-body th {
  background: #f9fafb;
  font-weight: 600;
}
.dark .markdown-body th {
  background: #1f2937;
}
.markdown-body strong {
  font-weight: 700;
}
</style>
