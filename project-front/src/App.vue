<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {darkTheme, NConfigProvider, NMessageProvider} from 'naive-ui'
import Navbar from './components/Navbar.vue'
import MessageNotification from './components/MessageNotification.vue'
import NotificationToast from './components/NotificationToast.vue'
// import Footer from './components/Footer.vue'  // 已移除全局Footer
import wsManager from './utils/websocket'
import sseManager from './utils/sse'
import {isLoggedIn} from './utils/request'

const isDark = ref(false)

const toggleDark = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('darkMode', isDark.value)
}

const initWebSocket = () => {
  if (isLoggedIn()) {
    wsManager.connect()
  }
}

const initSSE = () => {
  if (isLoggedIn()) {
    sseManager.connect()
  }
}

const handleStorageChange = (e) => {
  if (e.key === 'token') {
    if (e.newValue) {
      wsManager.connect()
      sseManager.connect()
    } else {
      wsManager.disconnect()
      sseManager.disconnect()
    }
  }
}

onMounted(() => {
  const savedDark = localStorage.getItem('darkMode')
  if (savedDark === 'true' || (!savedDark && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }

  initWebSocket()
  initSSE()
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  wsManager.disconnect()
  sseManager.disconnect()
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<template>
  <n-config-provider :theme="isDark ? darkTheme : null">
    <n-message-provider>
      <div class="min-h-screen flex flex-col">
        <Navbar :isDark="isDark" @toggleDark="toggleDark" />
        <main class="flex-1">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </main>
        <!-- <Footer /> 已移除全局Footer -->
      </div>
      
      <!-- 消息通知组件 -->
      <MessageNotification />
      
      <!-- SSE 通知弹窗组件 -->
      <NotificationToast />
    </n-message-provider>
  </n-config-provider>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
