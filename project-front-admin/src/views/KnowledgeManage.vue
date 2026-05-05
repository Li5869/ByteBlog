<script setup>
import {ref} from 'vue'
import {knowledgeApi} from '@/utils/request'

const loading = ref(false)
const uploadSuccess = ref(false)
const uploadMessage = ref('')

const fileInput = ref(null)
const selectedFile = ref(null)
const fileDragOver = ref(false)

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  validateAndSetFile(file)
}

const handleFileDrop = (event) => {
  event.preventDefault()
  fileDragOver.value = false
  const file = event.dataTransfer.files[0]
  validateAndSetFile(file)
}

const validateAndSetFile = (file) => {
  if (!file) return
  
  const allowedTypes = ['.txt', '.md']
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  
  if (!allowedTypes.includes(fileExtension)) {
    alert('仅支持 .txt 和 .md 文件')
    return
  }
  
  selectedFile.value = file
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const uploadFile = async () => {
  if (!selectedFile.value) {
    alert('请选择文件')
    return
  }
  
  loading.value = true
  uploadSuccess.value = false
  
  try {
    const result = await knowledgeApi.uploadFile(selectedFile.value)
    
    uploadSuccess.value = true
    uploadMessage.value = `文件 "${result.filename}" 上传成功！共生成 ${result.chunk_count} 个文本块`
    
    selectedFile.value = null
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  } catch (error) {
    console.error('上传文件失败:', error)
    alert('上传失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">知识库管理</h1>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">上传文件到知识库</h3>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">支持 .txt 和 .md 文件，上传后自动切片并向量化存储</p>
      </div>

      <div class="p-6 space-y-4">
        <div v-if="uploadSuccess" class="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <p class="text-sm text-green-800 dark:text-green-200">{{ uploadMessage }}</p>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            选择文件 <span class="text-red-500">*</span>
          </label>
          <div
            @dragover.prevent="fileDragOver = true"
            @dragleave.prevent="fileDragOver = false"
            @drop="handleFileDrop"
            @click="$refs.fileInput.click()"
            :class="[
              'border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors',
              fileDragOver
                ? 'border-red-500 bg-red-50 dark:bg-red-900/20'
                : 'border-gray-300 dark:border-gray-600 hover:border-red-400 dark:hover:border-red-500'
            ]"
          >
            <input
              ref="fileInput"
              type="file"
              accept=".txt,.md"
              @change="handleFileSelect"
              class="hidden"
            />
            <svg class="w-12 h-12 mx-auto text-gray-400 dark:text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">
              点击或拖拽文件到此处上传
            </p>
            <p class="text-xs text-gray-500 dark:text-gray-500">
              支持 .txt 和 .md 文件
            </p>
          </div>
        </div>

        <div v-if="selectedFile" class="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <div>
                <p class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedFile.name }}</p>
                <p class="text-xs text-gray-500 dark:text-gray-400">{{ formatFileSize(selectedFile.size) }}</p>
              </div>
            </div>
            <button
              @click="selectedFile = null"
              class="p-1.5 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div class="flex justify-end">
          <button
            @click="uploadFile"
            :disabled="loading || !selectedFile"
            class="px-6 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm font-medium flex items-center gap-2"
          >
            <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ loading ? '上传中...' : '上传到知识库' }}
          </button>
        </div>
      </div>
    </div>

    <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
      <div class="flex gap-3">
        <svg class="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="text-sm text-blue-800 dark:text-blue-200">
          <p class="font-medium mb-1">使用说明</p>
          <ul class="list-disc list-inside space-y-1 text-blue-700 dark:text-blue-300">
            <li>上传的文件会自动进行切片和向量化处理</li>
            <li>Markdown 文件会按标题结构进行智能切片</li>
            <li>上传成功后，内容将可用于 RAG 问答</li>
            <li>建议使用有意义的文件名，方便后续管理</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>
