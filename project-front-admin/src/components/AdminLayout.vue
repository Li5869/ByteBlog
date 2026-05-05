<script setup>
import {ref} from 'vue'
import AdminSidebar from './AdminSidebar.vue'
import AdminHeader from './AdminHeader.vue'

const isCollapsed = ref(false)
const sidebarWidth = ref('w-64')

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  sidebarWidth.value = isCollapsed.value ? 'w-16' : 'w-64'
}
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <AdminSidebar 
      :is-collapsed="isCollapsed" 
      :sidebar-width="sidebarWidth"
      @toggle="toggleSidebar"
    />
    <div 
      class="transition-all duration-300"
      :class="isCollapsed ? 'ml-16' : 'ml-64'"
    >
      <AdminHeader />
      <main class="p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
