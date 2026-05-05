<script setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {MdEditor} from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import 'md-editor-v3/lib/preview.css'
import '@/styles/md-editor-dark.css'
import {uploadApi} from '../utils/request'

const props = defineProps({
  article: {
    type: Object,
    default: () => ({
      title: '',
      summary: '',
      content: '',
      cover: '',
      categoryId: '',
      tags: [],
      status: 'draft'
    })
  },
  categories: {
    type: Array,
    default: () => []
  },
  tags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['submit', 'cancel'])

const form = ref({
  title: '',
  summary: '',
  content: '',
  cover: '',
  categoryId: '',
  tags: [],
  status: 'draft'
})

const errors = ref({})

watch(() => props.article, (newArticle) => {
  if (newArticle) {
    form.value = { ...newArticle }
  }
}, { immediate: true, deep: true })

const validateForm = () => {
  errors.value = {}

  if (!form.value.title.trim()) {
    errors.value.title = '请输入文章标题'
  } else if (form.value.title.length > 100) {
    errors.value.title = '标题不能超过100个字符'
  }

  if (form.value.summary && form.value.summary.length > 200) {
    errors.value.summary = '摘要不能超过200个字符'
  }

  if (!form.value.content.trim()) {
    errors.value.content = '请输入文章内容'
  }

  if (!form.value.categoryId) {
    errors.value.categoryId = '请选择分类'
  }

  return Object.keys(errors.value).length === 0
}

const handleSubmit = (status) => {
  form.value.status = status
  if (validateForm()) {
    emit('submit', { ...form.value })
  }
}

const handleCancel = () => {
  emit('cancel')
}

const toggleTag = (tagId) => {
  const index = form.value.tags.indexOf(tagId)
  if (index > -1) {
    form.value.tags.splice(index, 1)
  } else {
    form.value.tags.push(tagId)
  }
}

const isTagSelected = (tagId) => {
  return form.value.tags.includes(tagId)
}

const selectedCategoryName = computed(() => {
  const cat = props.categories.find(c => c.id === form.value.categoryId)
  return cat ? cat.name : ''
})

const uploadImage = async (files, callback) => {
  const results = []
  
  for (const file of files) {
    try {
      const url = await uploadApi.uploadFile(file)
      results.push(url)
    } catch (e) {
      console.error('图片上传失败:', e)
      throw e
    }
  }
  
  callback(results)
}

const editorTheme = ref(document.documentElement.classList.contains('dark') ? 'dark' : 'light')
let themeObserver = null

onMounted(() => {
  themeObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.attributeName === 'class') {
        editorTheme.value = document.documentElement.classList.contains('dark') ? 'dark' : 'light'
      }
    })
  })
  
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => {
  if (themeObserver) {
    themeObserver.disconnect()
  }
})
</script>

<template>
  <form @submit.prevent class="space-y-6">
    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        文章标题 <span class="text-red-500">*</span>
      </label>
      <input
        v-model="form.title"
        type="text"
        placeholder="请输入文章标题"
        class="w-full px-4 py-2 border rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
        :class="errors.title ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'"
      />
      <p v-if="errors.title" class="mt-1 text-sm text-red-500">{{ errors.title }}</p>
      <p class="mt-1 text-xs text-gray-400">{{ form.title.length }}/100</p>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        文章摘要
      </label>
      <textarea
        v-model="form.summary"
        rows="3"
        placeholder="请输入文章摘要（可选）"
        class="w-full px-4 py-2 border rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors resize-none"
        :class="errors.summary ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'"
      />
      <p v-if="errors.summary" class="mt-1 text-sm text-red-500">{{ errors.summary }}</p>
      <p class="mt-1 text-xs text-gray-400">{{ form.summary.length }}/200</p>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        文章内容 <span class="text-red-500">*</span>
      </label>
      <div 
        :class="errors.content ? 'border border-red-500 rounded-lg' : ''"
        class="md-editor-wrapper"
      >
        <MdEditor
          v-model="form.content"
          :theme="editorTheme"
          :height="500"
          placeholder="请输入文章内容，支持Markdown格式..."
          :onUploadImg="uploadImage"
          language="zh-CN"
          codeTheme="atom"
          previewTheme="default"
        />
      </div>
      <p v-if="errors.content" class="mt-1 text-sm text-red-500">{{ errors.content }}</p>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        封面URL
      </label>
      <input
        v-model="form.cover"
        type="text"
        placeholder="请输入封面图片URL（可选）"
        class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
      />
      <div v-if="form.cover" class="mt-2">
        <img
          :src="form.cover"
          alt="封面预览"
          class="w-32 h-20 object-cover rounded border border-gray-200 dark:border-gray-700"
          @error="$event.target.style.display = 'none'"
        />
      </div>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        分类 <span class="text-red-500">*</span>
      </label>
      <select
        v-model="form.categoryId"
        class="w-full px-4 py-2 border rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
        :class="errors.categoryId ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'"
      >
        <option value="">请选择分类</option>
        <option v-for="cat in categories" :key="cat.id" :value="cat.id">
          {{ cat.name }}
        </option>
      </select>
      <p v-if="errors.categoryId" class="mt-1 text-sm text-red-500">{{ errors.categoryId }}</p>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        标签
      </label>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tag in tags"
          :key="tag.id"
          type="button"
          @click="toggleTag(tag.id)"
          class="px-3 py-1.5 text-sm rounded-full border transition-colors"
          :class="isTagSelected(tag.id)
            ? 'bg-primary-500 text-white border-primary-500'
            : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:border-primary-500 hover:text-primary-500'"
        >
          {{ tag.name }}
        </button>
      </div>
      <p class="mt-2 text-xs text-gray-400">已选择 {{ form.tags.length }} 个标签</p>
    </div>

    <div class="flex flex-col sm:flex-row items-center justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
      <button
        type="button"
        @click="handleCancel"
        class="w-full sm:w-auto px-6 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
      >
        取消
      </button>
      <button
        type="button"
        @click="handleSubmit('draft')"
        class="w-full sm:w-auto px-6 py-2 border border-primary-500 text-primary-500 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors"
      >
        保存草稿
      </button>
      <button
        type="button"
        @click="handleSubmit('published')"
        class="w-full sm:w-auto px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
      >
        发布文章
      </button>
    </div>
  </form>
</template>

<style scoped>
.md-editor-wrapper {
  border-radius: 0.5rem;
  overflow: hidden;
}

:deep(.md-editor) {
  border-radius: 0.5rem !important;
}

:deep(.md-editor-preview) {
  padding: 16px;
}

:deep(.md-editor-input-wrapper) {
  border-right: 1px solid #e5e7eb;
}

.dark :deep(.md-editor) {
  background-color: #1f2937;
}

.dark :deep(.md-editor-header),
.dark :deep(.md-editor-footer) {
  background-color: #111827;
  border-color: #374151;
}

.dark :deep(.md-editor-toolbar-item:hover) {
  background-color: #374151;
}

.dark :deep(.md-editor-toolbar-item.active) {
  background-color: #4b5563;
}

.dark :deep(.md-editor-input-wrapper textarea) {
  background-color: #1f2937;
  color: #f3f4f6;
}

.dark :deep(.md-editor-preview) {
  background-color: #1f2937;
  color: #f3f4f6;
}

.dark :deep(.md-editor-input-wrapper) {
  border-right-color: #374151;
}
</style>
