<template>
  <span 
    class="online-indicator inline-flex items-center gap-1.5"
    :class="sizeClass"
  >
    <span 
      class="relative flex"
      :class="dotSizeClass"
    >
      <span 
        class="absolute inline-flex h-full w-full rounded-full opacity-75"
        :class="online ? 'bg-green-400 animate-ping' : 'bg-gray-300'"
      ></span>
      <span 
        class="relative inline-flex rounded-full"
        :class="[
          online ? 'bg-green-500' : 'bg-gray-400',
          dotSizeClass
        ]"
      ></span>
    </span>
    <span 
      v-if="showText" 
      class="text-gray-600 dark:text-gray-400"
      :class="textSizeClass"
    >
      {{ online ? '在线' : '离线' }}
    </span>
  </span>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import useOnlineStatus from '@/composables/useOnlineStatus'

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  },
  showText: {
    type: Boolean,
    default: false
  }
})

const { isOnline, queryOnlineStatus, isConnected } = useOnlineStatus()

// userId 保持字符串，不做 Number 转换（后端返回的雪花 ID 超过 JS 安全整数范围会丢失精度）
const normalizedUserId = computed(() => {
  if (props.userId === null || props.userId === undefined) {
    return null
  }
  return String(props.userId)
})

const online = computed(() => {
  if (!normalizedUserId.value) return false
  return isOnline(normalizedUserId.value)
})

const sizeClass = computed(() => {
  const classes = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base'
  }
  return classes[props.size]
})

const dotSizeClass = computed(() => {
  const classes = {
    sm: 'h-2 w-2',
    md: 'h-2.5 w-2.5',
    lg: 'h-3 w-3'
  }
  return classes[props.size]
})

const textSizeClass = computed(() => {
  const classes = {
    sm: 'text-xs',
    md: 'text-xs',
    lg: 'text-sm'
  }
  return classes[props.size]
})

const hasQueried = ref(false)

const queryStatus = () => {
  if (!normalizedUserId.value) return
  
  if (isConnected.value) {
    console.log('[OnlineIndicator] 查询用户在线状态:', normalizedUserId.value)
    queryOnlineStatus([normalizedUserId.value])
    hasQueried.value = true
  } else {
    console.log('[OnlineIndicator] WebSocket 未连接，等待连接...')
  }
}

onMounted(() => {
  queryStatus()
})

watch(isConnected, (newVal) => {
  if (newVal && !hasQueried.value) {
    queryStatus()
  }
})

watch(() => props.userId, () => {
  hasQueried.value = false
  queryStatus()
})
</script>

<style scoped>
.online-indicator {
  user-select: none;
}
</style>
