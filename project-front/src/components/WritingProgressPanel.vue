<script setup>
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {NProgress} from 'naive-ui'
import {getToken} from '@/utils/request'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  taskId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'viewResult'])

const collapsed = ref(false)
const planReady = ref(false)

// 计划阶段 + 执行阶段的步骤
const steps = reactive({
  planning: { status: 'pending', label: '写作计划' },
  title: { status: 'pending', label: '标题' },
  summary: { status: 'pending', label: '摘要' },
  content: { status: 'pending', label: '正文' },
  tags: { status: 'pending', label: '标签' },
  reflecting: { status: 'pending', label: '质量评估' }
})

// 标记是否已进入执行阶段（approve 后）
const executePhaseStarted = ref(false)

const currentStep = ref('')
const progress = ref(0)
const completed = ref(false)
const error = ref('')
const resultUrl = ref('')

const statusText = computed(() => {
  if (error.value) return `错误: ${error.value}`
  if (completed.value) return '写作完成'
  if (planReady.value) return '计划已就绪，请在对话中确认'
  if (!currentStep.value) return '准备中...'
  const step = steps[currentStep.value]
  return step ? `${step.label}生成中...` : '准备中...'
})

const progressPercent = computed(() => {
  if (completed.value) return 100
  if (error.value) return 0

  // 执行阶段：按已完成步骤数计算（title/summary/content/tags/reflecting）
  if (executePhaseStarted.value) {
    const executeKeys = ['title', 'summary', 'content', 'tags', 'reflecting']
    const totalSteps = executeKeys.length
    const completedCount = executeKeys.filter(key => steps[key].status === 'completed').length
    const isProcessing = executeKeys.some(key => steps[key].status === 'processing')
    // 已完成步骤占满额，当前处理步骤加 10%
    const baseProgress = (completedCount / totalSteps) * 100
    const extraProgress = isProcessing ? 10 : 0
    return Math.min(Math.round(baseProgress + extraProgress), 99)
  }

  // 计划阶段
  if (currentStep.value === 'planning') {
    return planReady.value ? 100 : Math.min(50 + progress.value, 99)
  }

  return 0
})

function getStepStatus(stepKey) {
  return steps[stepKey].status
}

function getStepIcon(stepKey) {
  const status = steps[stepKey].status
  if (status === 'completed') return '✅'
  if (status === 'processing') return '⏳'
  return '⏸️'
}

// 计划阶段的步骤不显示在执行阶段列表中
const executeSteps = computed(() => {
  const { planning, ...rest } = steps
  return rest
})

function resetState() {
  Object.keys(steps).forEach(key => {
    steps[key].status = 'pending'
  })
  currentStep.value = ''
  progress.value = 0
  completed.value = false
  error.value = ''
  resultUrl.value = ''
  planReady.value = false
  executePhaseStarted.value = false
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function closePanel() {
  // 任务进行中：只折叠不终止 SSE
  if (!completed.value && !error.value) {
    collapsed.value = true
    return
  }
  // 任务完成或出错：彻底关闭
  stopSSE()
  emit('close')
}

function viewResult() {
  emit('viewResult', props.taskId)
  closePanel()
}

let eventSource = null

function startSSE() {
  if (!props.taskId || !props.visible) return

  resetState()
  steps.planning.status = 'processing'
  currentStep.value = 'planning'

  const token = getToken()
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  // 通过 Java 后端代理访问 SSE（Java 负责认证和事件解析）
  const url = `${baseUrl}/api/ai/writing/${props.taskId}/stream${token ? `?token=${token}` : ''}`

  eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleEvent(data)
    } catch (e) {
      console.warn('[WritingProgress] 解析 SSE 数据失败:', e)
    }
  }

  eventSource.onerror = (e) => {
    console.error('[WritingProgress] SSE 连接错误:', e)
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (!completed.value && !planReady.value) {
      error.value = '连接中断'
    }
  }
}

function handleEvent(data) {
  const eventType = data.type

  // ========== 计划阶段事件 ==========

  if (eventType === 'plan_ready') {
    // 计划生成完成
    steps.planning.status = 'completed'
    planReady.value = true
    progress.value = 100
    // 计划就绪后 SSE 流结束，等待用户操作
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    return
  }

  // ========== 执行阶段事件 ==========

  if (eventType === 'phase') {
    const phaseData = data.data || {}
    const step = phaseData.step

    // 计划阶段的 phase 事件（step 为 "planning" 或 "generating_plan"），不切换到执行阶段
    if (step === 'planning' || step === 'generating_plan') {
      steps.planning.status = 'processing'
      currentStep.value = 'planning'
      return
    }

    // 首次收到执行阶段事件，标记进入执行阶段
    if (!executePhaseStarted.value) {
      executePhaseStarted.value = true
      steps.planning.status = 'completed'
      planReady.value = false
    }

    if (step && steps[step]) {
      if (currentStep.value && steps[currentStep.value]) {
        steps[currentStep.value].status = 'completed'
      }
      currentStep.value = step
      steps[step].status = 'processing'
    }
  }

  else if (eventType === 'reflection_result') {
    if (currentStep.value && steps[currentStep.value]) {
      steps[currentStep.value].status = 'completed'
    }
    currentStep.value = 'reflecting'
    steps.reflecting.status = 'processing'
    progress.value = 90
  }

  else if (eventType === 'finalize_ready') {
    if (currentStep.value && steps[currentStep.value]) {
      steps[currentStep.value].status = 'completed'
    }
    steps.reflecting.status = 'completed'
    completed.value = true
    progress.value = 100
    resultUrl.value = `/writing/${props.taskId}/result`
  }

  else if (eventType === 'done') {
    completed.value = true
    progress.value = 100
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  else if (eventType === 'error') {
    error.value = data.data || '未知错误'
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }
}

function stopSSE() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

// 外部调用：进入执行阶段（approve 后由父组件触发）
function startExecutePhase() {
  executePhaseStarted.value = true
  steps.planning.status = 'completed'
  planReady.value = false
  currentStep.value = ''
  progress.value = 0

  // 重新订阅 Writing SSE 流获取执行阶段进度
  stopSSE()
  const token = getToken()
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  // 通过 Java 后端代理访问 SSE（Java 负责认证和事件解析）
  const url = `${baseUrl}/api/ai/writing/${props.taskId}/stream${token ? `?token=${token}` : ''}`

  eventSource = new EventSource(url)
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleEvent(data)
    } catch (e) {
      console.warn('[WritingProgress] 解析 SSE 数据失败:', e)
    }
  }
  eventSource.onerror = (e) => {
    console.error('[WritingProgress] SSE 连接错误:', e)
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (!completed.value) {
      error.value = '连接中断'
    }
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal && props.taskId) {
    startSSE()
  } else {
    stopSSE()
  }
})

watch(() => props.taskId, (newVal) => {
  if (newVal && props.visible) {
    startSSE()
  }
})

onMounted(() => {
  if (props.visible && props.taskId) {
    startSSE()
  }
})

onUnmounted(() => {
  stopSSE()
})

defineExpose({
  startSSE,
  stopSSE,
  startExecutePhase
})
</script>

<template>
  <Transition name="slide-down">
    <div v-if="visible" class="writing-progress-panel">
      <div class="panel-header" @click="toggleCollapse">
        <div class="header-left">
          <div class="panel-icon" :class="{ 'plan-icon': !executePhaseStarted && !completed, 'execute-icon': executePhaseStarted || completed }">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </div>
          <div class="header-info">
            <span class="panel-title">
              {{ completed ? '写作完成' : planReady ? '写作计划已就绪' : executePhaseStarted ? '写作进行中' : '正在生成写作计划' }}
            </span>
            <span class="task-id">ID: {{ taskId }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="progress-mini" v-if="!completed">
            <n-progress
              type="circle"
              :percentage="progressPercent"
              :show-indicator="false"
              :stroke-width="16"
              :radius="10"
              :color="error ? '#ef4444' : '#8b5cf6'"
            />
            <span class="progress-mini-text">{{ progressPercent }}%</span>
          </div>
          <div class="completed-icon" v-else>
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <button class="collapse-btn" @click.stop="toggleCollapse">
            <svg :class="{ rotated: collapsed }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <button class="close-btn" @click.stop="closePanel">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <Transition name="expand">
        <div v-show="!collapsed" class="panel-body">
          <div class="progress-bar">
            <n-progress
              type="line"
              :percentage="progressPercent"
              :show-indicator="false"
              :height="6"
              :border-radius="3"
              :color="completed ? '#10b981' : error ? '#ef4444' : '#8b5cf6'"
            />
            <span class="progress-text">{{ progressPercent }}%</span>
          </div>

          <div class="status-text" :class="{ error: error }">{{ statusText }}</div>

          <!-- 计划阶段步骤 -->
          <div v-if="!executePhaseStarted" class="steps-list">
            <div class="step-item" :class="steps.planning.status">
              <span class="step-icon">{{ getStepIcon('planning') }}</span>
              <span class="step-label">{{ steps.planning.label }}</span>
              <span v-if="steps.planning.status === 'processing'" class="step-loading">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
          </div>

          <!-- 执行阶段步骤 -->
          <div v-else class="steps-list">
            <div
              v-for="(step, key) in executeSteps"
              :key="key"
              class="step-item"
              :class="getStepStatus(key)"
            >
              <span class="step-icon">{{ getStepIcon(key) }}</span>
              <span class="step-label">{{ step.label }}</span>
              <span v-if="step.status === 'processing'" class="step-loading">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
          </div>

          <!-- 计划就绪提示 -->
          <div v-if="planReady" class="plan-ready-actions">
            <div class="plan-ready-badge">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>写作计划已生成，请在对话中查看并确认</span>
            </div>
          </div>

          <!-- 写作完成操作 -->
          <div v-if="completed" class="completed-actions">
            <button class="view-result-btn" @click="viewResult">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              查看文章
            </button>
          </div>

          <div v-if="error" class="error-actions">
            <div class="error-badge">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>{{ error }}</span>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
.writing-progress-panel {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 12px 12px 0 0;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: none;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  flex-shrink: 0;
}

.dark .writing-progress-panel {
  background: rgba(30, 41, 59, 0.98);
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.2);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.panel-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.dark .panel-header:hover {
  background: rgba(255, 255, 255, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: background 0.3s ease;
}

.panel-icon.plan-icon {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.panel-icon.execute-icon {
  background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
}

.panel-icon svg {
  width: 18px;
  height: 18px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.dark .panel-title {
  color: #f3f4f6;
}

.task-id {
  font-size: 11px;
  color: #9ca3af;
  font-family: monospace;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-mini {
  position: relative;
  width: 28px;
  height: 28px;
}

.progress-mini-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 9px;
  font-weight: 600;
  color: #8b5cf6;
}

.dark .progress-mini-text {
  color: #a78bfa;
}

.completed-icon {
  width: 28px;
  height: 28px;
  color: #10b981;
}

.completed-icon svg {
  width: 100%;
  height: 100%;
}

.collapse-btn,
.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s ease;
}

.dark .collapse-btn,
.dark .close-btn {
  color: #9ca3af;
}

.collapse-btn:hover,
.close-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #374151;
}

.dark .collapse-btn:hover,
.dark .close-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e5e7eb;
}

.close-btn:hover {
  color: #ef4444;
}

.collapse-btn svg,
.close-btn svg {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
}

.collapse-btn svg.rotated {
  transform: rotate(180deg);
}

.panel-body {
  padding: 0 16px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.dark .panel-body {
  border-top-color: rgba(255, 255, 255, 0.05);
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  margin-bottom: 8px;
}

.progress-bar :deep(.n-progress) {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: #8b5cf6;
  min-width: 36px;
  text-align: right;
}

.dark .progress-text {
  color: #a78bfa;
}

.status-text {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 12px;
}

.dark .status-text {
  color: #9ca3af;
}

.status-text.error {
  color: #ef4444;
}

.steps-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 6px;
  font-size: 12px;
  transition: all 0.2s ease;
}

.dark .step-item {
  background: rgba(255, 255, 255, 0.02);
}

.step-item.processing {
  background: rgba(139, 92, 246, 0.08);
}

.step-item.completed {
  background: rgba(16, 185, 129, 0.08);
}

.step-icon {
  font-size: 12px;
}

.step-label {
  color: #374151;
}

.dark .step-label {
  color: #e5e7eb;
}

.step-loading {
  display: flex;
  gap: 2px;
}

.step-loading .dot {
  width: 3px;
  height: 3px;
  background: #8b5cf6;
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out both;
}

.step-loading .dot:nth-child(1) {
  animation-delay: -0.32s;
}

.step-loading .dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.plan-ready-actions,
.completed-actions,
.error-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.dark .plan-ready-actions,
.dark .completed-actions,
.dark .error-actions {
  border-top-color: rgba(255, 255, 255, 0.05);
}

.plan-ready-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8b5cf6;
  font-size: 12px;
  font-weight: 500;
}

.plan-ready-badge svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.error-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
  font-size: 12px;
}

.error-badge svg {
  width: 16px;
  height: 16px;
}

.view-result-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-result-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.view-result-btn svg {
  width: 14px;
  height: 14px;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
