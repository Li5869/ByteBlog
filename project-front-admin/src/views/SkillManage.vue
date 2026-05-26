<script setup>
import {computed, onMounted, ref} from 'vue'
import {skillApi} from '@/utils/request'

const loading = ref(false)
const rebuilding = ref(false)
const stats = ref(null)
const skillList = ref([])
const pagination = ref({current: 1, size: 10, total: 0})
const errorMsg = ref('')

const totalSkills = computed(() => stats.value?.totalSkills || 0)
const totalChunks = computed(() => stats.value?.totalChunks || 0)
const isEmpty = computed(() => !loading.value && skillList.value.length === 0)

const fetchStats = async () => {
  try {
    const data = await skillApi.getStats()
    stats.value = data
  } catch (e) {
    errorMsg.value = e.message || '查询统计失败'
  }
}

const fetchSkillList = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await skillApi.getList({
      current: pagination.value.current,
      size: pagination.value.size
    })
    skillList.value = data.records || []
    pagination.value.total = data.total || 0
    pagination.value.current = data.current || 1
    pagination.value.size = data.size || 10
  } catch (e) {
    errorMsg.value = e.message || '查询列表失败'
  } finally {
    loading.value = false
  }
}

const fetchAll = async () => {
  await Promise.all([fetchStats(), fetchSkillList()])
}

const rebuildIndex = async () => {
  if (rebuilding.value) return
  rebuilding.value = true
  errorMsg.value = ''
  try {
    await skillApi.rebuildIndex()
    await fetchAll()
  } catch (e) {
    errorMsg.value = e.message || '重建索引失败'
  } finally {
    rebuilding.value = false
  }
}

const handlePageChange = (page) => {
  pagination.value.current = page
  fetchSkillList()
}

onMounted(() => {
  fetchAll()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-5xl mx-auto space-y-6">

      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Agent 技能管理</h1>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">管理 AI 技能文档的向量索引，支持查看状态和一键重建</p>
        </div>
        <div class="flex items-center gap-3">
          <button
            @click="fetchAll"
            :disabled="loading"
            class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 transition-colors"
          >
            <svg class="w-4 h-4" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            刷新
          </button>
          <button
            @click="rebuildIndex"
            :disabled="rebuilding"
            class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-500 rounded-lg hover:bg-red-600 disabled:opacity-50 transition-colors"
          >
            <svg class="w-4 h-4" :class="{ 'animate-spin': rebuilding }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
            {{ rebuilding ? '重建中...' : '重建索引' }}
          </button>
        </div>
      </div>

      <div v-if="errorMsg" class="p-4 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg">
        <div class="flex items-center gap-2">
          <svg class="w-5 h-5 text-red-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <span class="text-sm text-red-700 dark:text-red-300">{{ errorMsg }}</span>
        </div>
      </div>

      <div v-if="loading && skillList.length === 0" class="flex items-center justify-center py-20">
        <svg class="w-8 h-8 text-red-500 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        <span class="ml-3 text-gray-500 dark:text-gray-400">加载中...</span>
      </div>

      <template v-else>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-5">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-red-100 dark:bg-red-900/30 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
              </div>
              <div>
                <p class="text-sm text-gray-500 dark:text-gray-400">已索引 Skill</p>
                <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ totalSkills }} <span class="text-sm font-normal text-gray-400">个</span></p>
              </div>
            </div>
          </div>
          <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-5">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                </svg>
              </div>
              <div>
                <p class="text-sm text-gray-500 dark:text-gray-400">切片总数</p>
                <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ totalChunks }} <span class="text-sm font-normal text-gray-400">个</span></p>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 class="text-base font-semibold text-gray-900 dark:text-white">Skill 索引详情</h2>
          </div>

          <div v-if="isEmpty" class="py-16 text-center">
            <svg class="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
            </svg>
            <p class="text-gray-500 dark:text-gray-400 mb-2">暂无索引数据</p>
            <p class="text-sm text-gray-400 dark:text-gray-500">点击"重建索引"按钮创建索引</p>
          </div>

          <div v-else class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-gray-50 dark:bg-gray-700/50">
                  <th class="px-5 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider w-16">#</th>
                  <th class="px-5 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Skill 名称</th>
                  <th class="px-5 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider w-32">切片数</th>
                  <th class="px-5 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider w-32">状态</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
                <tr v-for="(skill, index) in skillList" :key="skill.name" class="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors">
                  <td class="px-5 py-3 text-sm text-gray-500 dark:text-gray-400">{{ (pagination.current - 1) * pagination.size + index + 1 }}</td>
                  <td class="px-5 py-3">
                    <code class="text-sm font-mono text-gray-900 dark:text-white bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">{{ skill.name }}</code>
                  </td>
                  <td class="px-5 py-3 text-sm text-gray-900 dark:text-white">{{ skill.chunkCount }}</td>
                  <td class="px-5 py-3">
                    <span class="inline-flex items-center gap-1 text-xs font-medium text-green-700 dark:text-green-400 bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded-full">
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 8 8">
                        <circle cx="4" cy="4" r="3"/>
                      </svg>
                      {{ skill.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="pagination.total > pagination.size" class="px-5 py-4 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <p class="text-sm text-gray-500 dark:text-gray-400">
              共 {{ pagination.total }} 条
            </p>
            <div class="flex items-center gap-2">
              <button
                @click="handlePageChange(pagination.current - 1)"
                :disabled="pagination.current <= 1"
                class="px-3 py-1 text-sm text-gray-600 dark:text-gray-400 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                上一页
              </button>
              <span class="text-sm text-gray-600 dark:text-gray-400">{{ pagination.current }} / {{ Math.ceil(pagination.total / pagination.size) }}</span>
              <button
                @click="handlePageChange(pagination.current + 1)"
                :disabled="pagination.current >= Math.ceil(pagination.total / pagination.size)"
                class="px-3 py-1 text-sm text-gray-600 dark:text-gray-400 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                下一页
              </button>
            </div>
          </div>
        </div>

        <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-xl p-5">
          <div class="flex gap-3">
            <svg class="w-5 h-5 text-blue-500 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <div class="space-y-1">
              <p class="text-sm font-medium text-blue-800 dark:text-blue-300">Skill 维护说明</p>
              <p class="text-sm text-blue-700 dark:text-blue-400">Skill 文档位于 codebase 的 <code class="bg-blue-100 dark:bg-blue-900/50 px-1 rounded">project-ai-agent/skills/</code> 目录下，每个 Skill 对应一个子目录中的 <code class="bg-blue-100 dark:bg-blue-900/50 px-1 rounded">SKILL.md</code> 文件。</p>
              <p class="text-sm text-blue-700 dark:text-blue-400">修改 SKILL.md 后请执行"重建索引"以更新向量库，索引重建期间 AI 对话仍可正常使用（自动降级为完整文档加载）。</p>
            </div>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>