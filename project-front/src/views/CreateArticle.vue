<script setup>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import {aiApi, articleApi, categoryApi, columnApi, tagApi, uploadApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'

const router = useRouter()
const route = useRoute()

// 监听暗黑模式变化
const isDarkMode = ref(document.documentElement.classList.contains('dark'))

// 监听 DOM 变化以响应暗黑模式切换
const observeDarkMode = () => {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach(() => {
      isDarkMode.value = document.documentElement.classList.contains('dark')
      if (vditorInstance.value) {
        vditorInstance.value.setTheme(isDarkMode.value ? 'dark' : 'classic')
        // 同时更新内容主题
        vditorInstance.value.setContentTheme(isDarkMode.value ? 'dark' : 'light')
      }
    })
  })
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class']
  })
}
const editArticleId = computed(() => route.query.articleId)
const isEditMode = computed(() => !!editArticleId.value)

const categories = ref([])
const availableTags = ref([])
const myColumns = ref([])

const tagColors = [
  'from-green-400 to-emerald-500',
  'from-cyan-400 to-blue-500',
  'from-orange-400 to-red-500',
  'from-green-500 to-teal-500',
  'from-blue-400 to-indigo-500',
  'from-red-400 to-rose-500',
  'from-blue-500 to-cyan-400',
  'from-blue-600 to-indigo-600',
]

const article = reactive({
  title: '',
  summary: '',
  content: '',
  cover: '',
  coverFile: null,
  categoryId: '',
  tags: [],
  columnIds: []
})
const customTagInput = ref('')
const customTags = ref([])

const errors = reactive({
  title: '',
  content: ''
})

const isSubmitting = ref(false)
const isGeneratingTitle = ref(false)
const isGeneratingSummary = ref(false)
const coverFileName = ref('')

// AI 生成标题 - 配置 & 结果
const titleAiPanelOpen = ref(false)
const titleAiStyle = ref('professional')
const titleAiMaxLength = ref(30)
const titleAiCandidates = ref([])   // 候选列表
const titleAiShowPicker = ref(false) // 是否展示候选卡片

const titleStyleOptions = [
  { value: 'professional', label: '专业严谨' },
  { value: 'casual', label: '轻松易读' },
  { value: 'creative', label: '创意吸睛' },
]

// AI 生成摘要 - 配置 & 面板
const summaryAiPanelOpen = ref(false)
const summaryAiMaxLength = ref(200)

// AI 文章润色 - 配置
const polishPanelOpen = ref(false)
const isPolishing = ref(false)
const polishStyle = ref('professional')

const polishStyleOptions = [
  { value: 'professional', label: '专业严谨', desc: '正式书面用语，适合技术文档' },
  { value: 'friendly', label: '轻松友好', desc: '亲切自然，适合个人博客' },
  { value: 'concise', label: '简洁精炼', desc: '删繁就简，适合快速阅读' },
]
const fileInputRef = ref(null)
const mdFileInputRef = ref(null)
const isDragging = ref(false)
const vditorRef = ref(null)
const vditorInstance = ref(null)
let vditorReady = false

const toggleFormat = (prefix, suffix) => {
  if (!vditorInstance.value) return
  
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) return
  
  const range = selection.getRangeAt(0)
  const selectedText = range.toString()
  
  if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix)) {
    const unwrappedText = selectedText.slice(prefix.length, -suffix.length)
    document.execCommand('insertText', false, unwrappedText)
  } else {
    vditorInstance.value.insertValue(prefix + selectedText + suffix)
  }
}

const initVditor = () => {
  if (!vditorRef.value) return
  
  vditorInstance.value = new Vditor(vditorRef.value, {
    height: 500,
    mode: 'ir',
    placeholder: '开始创作你的文章...',
    theme: document.documentElement.classList.contains('dark') ? 'dark' : 'classic',
    icon: 'ant',
    lang: 'zh_CN',
    value: article.content,
    cache: {
      enable: false
    },
    // 使用自定义上传处理函数
    upload: {
      handler: async (files) => {
        const file = files[0]
        if (!file) {
          toast.error('没有选择文件')
          return
        }
        
        try {
          const token = localStorage.getItem('token') || ''
          const formData = new FormData()
          formData.append('file', file)
          
          const response = await fetch('/api/upload', {
            method: 'POST',
            headers: {
              ...(token ? { 'token': token } : {})
            },
            body: formData
          })
          
          const data = await response.json()
          
          if (data.code === 0 && data.data) {
            // 直接插入图片 Markdown 到编辑器
            const imageUrl = data.data
            const fileName = file.name || 'image'
            // 根据扩展名判断是否是图片
            const isImage = /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName)
            
            if (isImage) {
              vditorInstance.value.insertValue(`![${fileName}](${imageUrl})`)
              toast.success('图片上传成功')
            } else {
              vditorInstance.value.insertValue(`[${fileName}](${imageUrl})`)
              toast.success('文件上传成功')
            }
          } else {
            toast.error(data.msg || '上传失败')
          }
        } catch (error) {
          console.error('上传错误:', error)
          toast.error('上传失败，请重试')
        }
      },
      error(msg) {
        console.error('上传错误:', msg)
        toast.error(msg || '图片上传失败')
      },
      success(editor, msg) {
      }
    },
    toolbar: [
      'headings',
      {
        name: 'bold',
        tip: '粗体',
        icon: '<svg><use xlink:href="#vditor-icon-bold"></use></svg>',
        click: () => toggleFormat('**', '**')
      },
      {
        name: 'italic',
        tip: '斜体',
        icon: '<svg><use xlink:href="#vditor-icon-italic"></use></svg>',
        click: () => toggleFormat('*', '*')
      },
      {
        name: 'strike',
        tip: '删除线',
        icon: '<svg><use xlink:href="#vditor-icon-strike"></use></svg>',
        click: () => toggleFormat('~~', '~~')
      },
      '|',
      'line',
      'quote',
      '|',
      'list',
      'ordered-list',
      'check',
      '|',
      'code',
      {
        name: 'inline-code',
        tip: '行内代码',
        icon: '<svg><use xlink:href="#vditor-icon-code"></use></svg>',
        click: () => toggleFormat('`', '`')
      },
      '|',
      'link',
      'upload',  // 添加图片上传按钮
      'table',
      '|',
      'undo',
      'redo',
      '|',
      'edit-mode',
      {
        name: 'more',
        toolbar: [
          'both',
          'code-theme',
          'content-theme',
          'export',
          'outline',
          'preview',
          'devtools'
        ]
      }
    ],
    toolbarConfig: {
      hide: false,
      pin: true
    },
    counter: {
      enable: true
    },
    resize: {
      enable: true
    },
    input: (value) => {
      article.content = value
    },
    after: () => {
      vditorReady = true
      if (article.content) {
        vditorInstance.value.setValue(article.content)
      }
    }
  })
}

const handleMdFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.name.endsWith('.md') && !file.name.endsWith('.markdown')) {
    toast.error('请上传 Markdown 文件（.md 或 .markdown）')
    return
  }
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    const content = e.target.result
    if (article.content) {
      const confirmed = await modal.confirm('是否覆盖现有内容？点击"确定"覆盖，点击"取消"追加到末尾', {
        title: '导入确认',
        confirmText: '覆盖',
        cancelText: '追加'
      })
      if (confirmed) {
        article.content = content
      } else {
        article.content += '\n\n' + content
      }
    } else {
      article.content = content
    }
    
    if (vditorInstance.value) {
      vditorInstance.value.setValue(article.content)
    }
    
    const titleMatch = content.match(/^#\s+(.+)$/m)
    if (titleMatch && !article.title) {
      article.title = titleMatch[1].trim()
    }
  }
  reader.readAsText(file)
  
  if (mdFileInputRef.value) {
    mdFileInputRef.value.value = ''
  }
}

const triggerMdFileInput = () => {
  mdFileInputRef.value?.click()
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
  
  article.coverFile = file
  coverFileName.value = file.name
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    article.cover = e.target.result
    
    try {
      const url = await uploadApi.uploadFile(file)
      article.cover = url
    } catch (error) {
      console.error('上传封面失败:', error)
      toast.error('封面上传失败，请重试')
      removeCover()
    }
  }
  reader.readAsDataURL(file)
}

const handleDrop = (event) => {
  event.preventDefault()
  isDragging.value = false
  
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
  
  article.coverFile = file
  coverFileName.value = file.name
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    article.cover = e.target.result
    
    try {
      const url = await uploadApi.uploadFile(file)
      article.cover = url
    } catch (error) {
      console.error('上传封面失败:', error)
      toast.error('封面上传失败，请重试')
      removeCover()
    }
  }
  reader.readAsDataURL(file)
}

const handleDragOver = (event) => {
  event.preventDefault()
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const removeCover = () => {
  article.cover = ''
  article.coverFile = null
  coverFileName.value = ''
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const validateForm = () => {
  let isValid = true
  errors.title = ''
  errors.content = ''

  if (!article.title.trim()) {
    errors.title = '请输入文章标题'
    isValid = false
  } else if (article.title.length > 200) {
    errors.title = '标题不能超过200个字符'
    isValid = false
  }

  if (!article.content.trim()) {
    errors.content = '请输入文章内容'
    isValid = false
  }

  return isValid
}

const buildSubmitPayload = (status) => {
  return {
    title: article.title.trim(),
    summary: article.summary.trim(),
    content: article.content.trim(),
    cover: article.cover || '',
    categoryId: article.categoryId || null,
    tagIds: article.tags,
    tagNames: customTags.value,
    status
  }
}

const fillEditForm = (data) => {
  article.title = data.title || ''
  article.summary = data.summary || ''
  article.content = data.content || ''
  article.cover = data.cover || ''
  article.categoryId = data.categoryId || ''
  article.tags = data.tagIds || []
  customTags.value = []
  if (vditorReady && vditorInstance.value) {
    vditorInstance.value.setValue(article.content)
  }
}

const toggleTag = (tagId) => {
  const index = article.tags.indexOf(tagId)
  if (index > -1) {
    article.tags.splice(index, 1)
  } else {
    if (article.tags.length + customTags.value.length >= 10) {
      toast.error('标签最多只能选择或输入10个')
      return
    }
    article.tags.push(tagId)
  }
}

const addCustomTag = () => {
  const value = customTagInput.value.trim()
  if (!value) return
  if (value.length > 20) {
    toast.error('标签长度不能超过20个字符')
    return
  }
  const existingTag = availableTags.value.find(item => item.name === value)
  if (existingTag) {
    if (!article.tags.includes(existingTag.id)) {
      article.tags.push(existingTag.id)
    }
    customTagInput.value = ''
    return
  }
  if (customTags.value.includes(value)) {
    customTagInput.value = ''
    return
  }
  if (article.tags.length + customTags.value.length >= 10) {
    toast.error('标签最多只能选择或输入10个')
    return
  }
  customTags.value.push(value)
  customTagInput.value = ''
}

const removeCustomTag = (tagName) => {
  customTags.value = customTags.value.filter(item => item !== tagName)
}

const handlePublish = async () => {
  if (!validateForm()) return
  if (isSubmitting.value) return
  isSubmitting.value = true
  try {
    const payload = buildSubmitPayload(1)
    const res = isEditMode.value
      ? await articleApi.updateArticle(editArticleId.value, payload)
      : await articleApi.createArticle(payload)
    toast.success('发布成功，文章正在审核中，审核通过后将对外展示')
    router.push(`/article/${res.id}`)
  } catch (error) {
    console.error('发布文章失败:', error)
    toast.error(error.message || '发布失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleSaveDraft = async () => {
  if (!article.title.trim()) {
    errors.title = '请输入文章标题'
    return
  }
  if (isSubmitting.value) return
  isSubmitting.value = true
  try {
    const payload = buildSubmitPayload(0)
    if (isEditMode.value) {
      await articleApi.updateArticle(editArticleId.value, payload)
    } else {
      await articleApi.createArticle(payload)
    }
    toast.success('草稿已保存')
  } catch (error) {
    console.error('保存草稿失败:', error)
    toast.error(error.message || '保存草稿失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

const generateAITitle = async () => {
  const content = article.content.trim()
  if (!content) {
    toast.error('请先输入文章内容，再生成标题')
    return
  }
  isGeneratingTitle.value = true
  titleAiShowPicker.value = false
  titleAiCandidates.value = []
  try {
    const res = await aiApi.generateTitle(content, titleAiMaxLength.value, titleAiStyle.value)
    const titleData = res?.data ?? res
    const list = titleData?.titles || []
    const best = titleData?.bestTitle || ''
    const merged = best ? [best, ...list.filter(t => t !== best)] : list
    if (merged.length > 0) {
      titleAiCandidates.value = merged
      titleAiShowPicker.value = true
    } else {
      toast.error('AI 未返回有效标题，请重试')
    }
  } catch (error) {
    console.error('AI 生成标题失败:', error)
    toast.error(error.message || 'AI 生成失败，请稍后重试')
  } finally {
    isGeneratingTitle.value = false
  }
}

const pickTitle = (title) => {
  article.title = title
  titleAiShowPicker.value = false
  titleAiPanelOpen.value = false
  toast.success('已应用标题')
}

const generateAISummary = async () => {
  const content = article.content.trim()
  if (!content) {
    toast.error('请先输入文章内容，再生成摘要')
    return
  }
  isGeneratingSummary.value = true
  try {
    const res = await aiApi.generateSummary(content, article.title.trim(), summaryAiMaxLength.value)
    const summaryData = res?.data ?? res
    const picked = summaryData?.summary || ''
    if (picked) {
      article.summary = picked
      summaryAiPanelOpen.value = false
      toast.success('AI 摘要生成成功')
    } else {
      toast.error('AI 未返回有效摘要，请重试')
    }
  } catch (error) {
    console.error('AI 生成摘要失败:', error)
    toast.error(error.message || 'AI 生成失败，请稍后重试')
  } finally {
    isGeneratingSummary.value = false
  }
}

const handlePolish = async () => {
  const content = article.content.trim()
  if (!content) {
    toast.error('请先输入文章内容，再进行润色')
    return
  }
  isPolishing.value = true
  try {
    const res = await aiApi.polishArticle(content, article.title.trim(), polishStyle.value)
    const polished = res?.polishedContent || ''
    if (polished) {
      vditorInstance.value.setValue(polished)
      toast.success('润色完成，已填入编辑器，可按 Ctrl+Z 撤回')
    } else {
      toast.error('AI 未返回有效内容，请重试')
    }
  } catch (error) {
    console.error('AI 润色失败:', error)
    toast.error(error.message || '润色失败，请稍后重试')
  } finally {
    isPolishing.value = false
  }
}

const goBack = () => {
  router.back()
}

const fetchCategories = async () => {
  try {
    const data = await categoryApi.getCategories()
    categories.value = (data || []).map(item => ({
      id: item.id,
      name: item.name,
      icon: '📁'
    }))
  } catch (error) {
    console.error('加载分类失败:', error)
    toast.error('加载分类失败')
  }
}

const fetchTags = async () => {
  try {
    const data = await tagApi.getTags()
    availableTags.value = (data || []).map((item, index) => ({
      id: item.id,
      name: item.name,
      color: tagColors[index % tagColors.length]
    }))
  } catch (error) {
    console.error('加载标签失败:', error)
    toast.error('加载标签失败')
  }
}

const fetchMyColumns = async () => {
  try {
    const data = await columnApi.getMyColumns()
    myColumns.value = data || []
  } catch (error) {
    console.error('加载专栏失败:', error)
  }
}

const showCreateColumnModal = ref(false)
const newColumnName = ref('')
const newColumnDescription = ref('')
const isCreatingColumn = ref(false)

const openCreateColumnModal = () => {
  newColumnName.value = ''
  newColumnDescription.value = ''
  showCreateColumnModal.value = true
}

const handleCreateColumn = async () => {
  if (!newColumnName.value.trim()) {
    toast.error('请输入专栏名称')
    return
  }
  
  isCreatingColumn.value = true
  try {
    const data = await columnApi.createColumn({
      title: newColumnName.value,
      description: newColumnDescription.value,
      status: 0
    })
    
    const newColumn = {
      id: data.id,
      title: newColumnName.value,
      description: newColumnDescription.value
    }
    
    myColumns.value.push(newColumn)
    article.columnIds.push(data.id)
    showCreateColumnModal.value = false
    toast.success('专栏创建成功')
  } catch (error) {
    console.error('创建专栏失败:', error)
    toast.error('创建专栏失败')
  } finally {
    isCreatingColumn.value = false
  }
}

const toggleColumn = (columnId) => {
  const index = article.columnIds.indexOf(columnId)
  if (index > -1) {
    article.columnIds.splice(index, 1)
  } else {
    if (article.columnIds.length >= 3) {
      toast.warning('最多只能选择3个专栏')
      return
    }
    article.columnIds.push(columnId)
  }
}

const fetchEditArticle = async () => {
  if (!isEditMode.value) return
  try {
    const data = await articleApi.getEditArticle(editArticleId.value)
    fillEditForm(data || {})
  } catch (error) {
    console.error('加载编辑内容失败:', error)
    toast.error(error.message || '加载文章失败')
    router.push('/mine')
  }
}

onMounted(() => {
  nextTick(() => {
    initVditor()
  })
  fetchCategories()
  fetchTags()
  fetchMyColumns()
  fetchEditArticle()
  // 监听暗黑模式切换
  observeDarkMode()
})

onUnmounted(() => {
  if (vditorInstance.value) {
    vditorInstance.value.destroy()
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800 relative">
    <div class="bg-gradient-to-r from-primary-500 via-primary-600 to-primary-700 relative overflow-hidden">
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
              <h1 class="text-xl sm:text-2xl font-bold text-white">{{ isEditMode ? '编辑文章' : '创作文章' }}</h1>
              <p class="text-white/70 text-sm mt-0.5">{{ isEditMode ? '更新你的文章内容' : '分享你的技术见解' }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2 sm:gap-3 relative">
            <button 
              @click="router.push('/ai-writing')"
              class="px-3 sm:px-4 py-2.5 bg-gradient-to-r from-violet-500 to-purple-500 backdrop-blur-sm text-white border border-violet-400/30 rounded-xl hover:from-violet-600 hover:to-purple-600 transition-all text-sm font-medium flex items-center gap-2 shadow-lg shadow-violet-500/20"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
              <span class="hidden sm:inline">AI生成文章</span>
            </button>
            <button 
              @click="triggerMdFileInput"
              class="px-3 sm:px-4 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/20 rounded-xl hover:bg-white/30 transition-all text-sm font-medium flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <span class="hidden sm:inline">导入MD</span>
            </button>
            <button 
              @click="handleSaveDraft"
              :disabled="isSubmitting"
              class="px-3 sm:px-4 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/20 rounded-xl hover:bg-white/30 transition-all text-sm font-medium flex items-center gap-2 disabled:opacity-50"
            >
              {{ isEditMode ? '保存修改' : '保存草稿' }}
            </button>
            <button 
              @click="handlePublish"
              :disabled="isSubmitting"
              class="px-4 sm:px-6 py-2.5 bg-white text-primary-600 rounded-xl hover:bg-white/90 transition-all text-sm font-semibold shadow-lg hover:shadow-xl disabled:opacity-50"
            >
              {{ isEditMode ? '更新并发布' : '发布文章' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 -mt-2">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
        <div class="p-5 sm:p-8 space-y-6 sm:space-y-8">
          <div>
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              文章标题 <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="article.title"
              type="text"
              maxlength="200"
              placeholder="输入一个吸引人的标题..."
              class="w-full px-5 py-4 border-2 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-0 focus:border-primary-500 focus:bg-white dark:focus:bg-gray-700 transition-all text-lg font-medium"
              :class="errors.title ? 'border-red-300 dark:border-red-500' : 'border-gray-200 dark:border-gray-600'"
            />
            <div class="flex justify-between items-center mt-2">
              <span v-if="errors.title" class="text-sm text-red-500 flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {{ errors.title }}
              </span>
              <button
                type="button"
                @click="titleAiPanelOpen = !titleAiPanelOpen"
                class="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border transition-all duration-200 border-violet-200 dark:border-violet-700 bg-violet-50 dark:bg-violet-900/20 text-violet-600 dark:text-violet-400 hover:bg-violet-100 dark:hover:bg-violet-900/40"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                AI 生成标题
                <svg class="w-3 h-3 transition-transform duration-200" :class="titleAiPanelOpen ? 'rotate-180' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              <span class="text-xs text-gray-400 ml-auto" :class="{ 'text-orange-500': article.title.length > 180 }">{{ article.title.length }}/200</span>
            </div>

            <!-- AI 标题配置面板 -->
            <Transition
              enter-active-class="transition-all duration-200 ease-out"
              enter-from-class="opacity-0 -translate-y-1"
              enter-to-class="opacity-100 translate-y-0"
              leave-active-class="transition-all duration-150 ease-in"
              leave-from-class="opacity-100 translate-y-0"
              leave-to-class="opacity-0 -translate-y-1"
            >
              <div v-if="titleAiPanelOpen" class="mt-3 p-4 rounded-xl border border-violet-200 dark:border-violet-700 bg-violet-50/60 dark:bg-violet-900/10">
                <div class="flex flex-wrap gap-3 items-end">
                  <!-- 风格选择 -->
                  <div class="flex-1 min-w-[140px]">
                    <label class="block text-xs font-medium text-violet-700 dark:text-violet-400 mb-1.5">标题风格</label>
                    <div class="flex gap-1.5">
                      <button
                        v-for="opt in titleStyleOptions"
                        :key="opt.value"
                        type="button"
                        @click="titleAiStyle = opt.value"
                        class="flex-1 px-2.5 py-1.5 text-xs rounded-lg border transition-all duration-150 font-medium"
                        :class="titleAiStyle === opt.value
                          ? 'border-violet-500 bg-violet-500 text-white'
                          : 'border-violet-200 dark:border-violet-600 text-violet-600 dark:text-violet-400 hover:border-violet-400 hover:bg-violet-100 dark:hover:bg-violet-900/30'"
                      >
                        {{ opt.label }}
                      </button>
                    </div>
                  </div>
                  <!-- 最大长度 -->
                  <div class="min-w-[110px]">
                    <label class="block text-xs font-medium text-violet-700 dark:text-violet-400 mb-1.5">最大长度</label>
                    <div class="flex items-center gap-1.5">
                      <input
                        v-model.number="titleAiMaxLength"
                        type="number"
                        min="10"
                        max="100"
                        class="w-16 px-2.5 py-1.5 text-xs border rounded-lg bg-white dark:bg-gray-700 border-violet-200 dark:border-violet-600 text-gray-900 dark:text-white focus:ring-0 focus:border-violet-400 text-center"
                      />
                      <span class="text-xs text-violet-500 dark:text-violet-400">字</span>
                    </div>
                  </div>
                  <!-- 生成按钮 -->
                  <button
                    type="button"
                    @click="generateAITitle"
                    :disabled="isGeneratingTitle"
                    class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all duration-200 flex items-center gap-1.5"
                    :class="isGeneratingTitle
                      ? 'bg-violet-300 dark:bg-violet-700 text-white cursor-not-allowed'
                      : 'bg-violet-500 hover:bg-violet-600 text-white shadow-sm hover:shadow-md shadow-violet-500/30'"
                  >
                    <svg v-if="isGeneratingTitle" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    {{ isGeneratingTitle ? '生成中...' : '立即生成' }}
                  </button>
                </div>

                <!-- 候选标题列表 -->
                <Transition
                  enter-active-class="transition-all duration-200 ease-out"
                  enter-from-class="opacity-0 translate-y-1"
                  enter-to-class="opacity-100 translate-y-0"
                >
                  <div v-if="titleAiShowPicker && titleAiCandidates.length > 0" class="mt-3 space-y-1.5">
                    <p class="text-xs font-medium text-violet-600 dark:text-violet-400 mb-2 flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                      点击选择一个标题（共 {{ titleAiCandidates.length }} 个）
                    </p>
                    <button
                      v-for="(t, idx) in titleAiCandidates"
                      :key="idx"
                      type="button"
                      @click="pickTitle(t)"
                      class="w-full text-left px-3.5 py-2.5 text-sm rounded-lg border transition-all duration-150 flex items-center gap-2 group"
                      :class="idx === 0
                        ? 'border-violet-300 dark:border-violet-600 bg-white dark:bg-gray-700/80 hover:border-violet-500 hover:bg-violet-50 dark:hover:bg-violet-900/30'
                        : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700/50 hover:border-violet-400 hover:bg-violet-50/60 dark:hover:bg-violet-900/20'"
                    >
                      <span v-if="idx === 0" class="shrink-0 inline-flex items-center px-1.5 py-0.5 text-[10px] font-bold rounded bg-violet-500 text-white leading-none">推荐</span>
                      <span v-else class="shrink-0 w-4 h-4 flex items-center justify-center text-[10px] font-bold rounded-full border border-gray-300 dark:border-gray-500 text-gray-400 dark:text-gray-500 leading-none">{{ idx + 1 }}</span>
                      <span class="text-gray-800 dark:text-gray-200 group-hover:text-violet-700 dark:group-hover:text-violet-300 transition-colors">{{ t }}</span>
                    </button>
                  </div>
                </Transition>
              </div>
            </Transition>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              文章摘要
            </label>
            <textarea 
              v-model="article.summary"
              maxlength="500"
              rows="3"
              placeholder="简要描述文章内容，让读者快速了解..."
              class="w-full px-5 py-4 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-0 focus:border-primary-500 focus:bg-white dark:focus:bg-gray-700 transition-all resize-none"
            />
            <div class="flex justify-between items-center mt-2">
              <button
                type="button"
                @click="summaryAiPanelOpen = !summaryAiPanelOpen"
                class="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border transition-all duration-200 border-violet-200 dark:border-violet-700 bg-violet-50 dark:bg-violet-900/20 text-violet-600 dark:text-violet-400 hover:bg-violet-100 dark:hover:bg-violet-900/40"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                AI 生成摘要
                <svg class="w-3 h-3 transition-transform duration-200" :class="summaryAiPanelOpen ? 'rotate-180' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              <div class="flex items-center gap-3 ml-auto">
                <span class="text-xs text-gray-400">可选，不填写将自动从正文提取</span>
                <span class="text-xs text-gray-400" :class="{ 'text-orange-500': article.summary.length > 450 }">{{ article.summary.length }}/500</span>
              </div>
            </div>

            <!-- AI 摘要配置面板 -->
            <Transition
              enter-active-class="transition-all duration-200 ease-out"
              enter-from-class="opacity-0 -translate-y-1"
              enter-to-class="opacity-100 translate-y-0"
              leave-active-class="transition-all duration-150 ease-in"
              leave-from-class="opacity-100 translate-y-0"
              leave-to-class="opacity-0 -translate-y-1"
            >
              <div v-if="summaryAiPanelOpen" class="mt-3 p-4 rounded-xl border border-violet-200 dark:border-violet-700 bg-violet-50/60 dark:bg-violet-900/10">
                <div class="flex flex-wrap gap-3 items-end">
                  <!-- 最大长度 -->
                  <div>
                    <label class="block text-xs font-medium text-violet-700 dark:text-violet-400 mb-1.5">摘要最大长度</label>
                    <div class="flex items-center gap-1.5">
                      <input
                        v-model.number="summaryAiMaxLength"
                        type="number"
                        min="50"
                        max="500"
                        step="50"
                        class="w-20 px-2.5 py-1.5 text-xs border rounded-lg bg-white dark:bg-gray-700 border-violet-200 dark:border-violet-600 text-gray-900 dark:text-white focus:ring-0 focus:border-violet-400 text-center"
                      />
                      <span class="text-xs text-violet-500 dark:text-violet-400">字</span>
                      <!-- 快捷长度 -->
                      <div class="flex gap-1 ml-2">
                        <button v-for="len in [100, 200, 300]" :key="len" type="button"
                          @click="summaryAiMaxLength = len"
                          class="px-2 py-1 text-[10px] font-medium rounded border transition-all"
                          :class="summaryAiMaxLength === len
                            ? 'border-violet-500 bg-violet-500 text-white'
                            : 'border-violet-200 dark:border-violet-600 text-violet-500 dark:text-violet-400 hover:border-violet-400 hover:bg-violet-100 dark:hover:bg-violet-900/30'"
                        >{{ len }}</button>
                      </div>
                    </div>
                  </div>
                  <!-- 生成按钮 -->
                  <button
                    type="button"
                    @click="generateAISummary"
                    :disabled="isGeneratingSummary"
                    class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all duration-200 flex items-center gap-1.5"
                    :class="isGeneratingSummary
                      ? 'bg-violet-300 dark:bg-violet-700 text-white cursor-not-allowed'
                      : 'bg-violet-500 hover:bg-violet-600 text-white shadow-sm hover:shadow-md shadow-violet-500/30'"
                  >
                    <svg v-if="isGeneratingSummary" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    {{ isGeneratingSummary ? '生成中...' : '立即生成' }}
                  </button>
                </div>
                <p class="mt-2 text-xs text-violet-500/70 dark:text-violet-400/60">
                  若已填写标题，AI 将结合标题内容生成更准确的摘要
                </p>
              </div>
            </Transition>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              文章内容 <span class="text-red-500">*</span>
            </label>
            
            <input 
              ref="mdFileInputRef"
              type="file"
              accept=".md,.markdown"
              @change="handleMdFileUpload"
              class="hidden"
            />
            
            <div 
              class="border-2 rounded-xl overflow-hidden transition-colors editor-wrapper"
              :class="errors.content ? 'border-red-300 dark:border-red-500' : 'border-gray-200 dark:border-gray-600'"
            >
              <div ref="vditorRef" class="vditor-container"></div>
            </div>
            <div class="flex justify-between mt-2">
              <span v-if="errors.content" class="text-sm text-red-500 flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {{ errors.content }}
              </span>
              <span class="text-xs text-gray-400 ml-auto flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                支持 Markdown 实时渲染
              </span>
            </div>

            <!-- AI 润色区域 -->
            <div class="mt-4 p-4 rounded-xl border border-violet-200 dark:border-violet-700/50 bg-violet-50/50 dark:bg-violet-900/10">
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <svg class="w-4 h-4 text-violet-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  <span class="text-sm font-medium text-gray-700 dark:text-gray-300">AI 文章润色</span>
                </div>
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <div class="flex gap-2">
                  <button
                    v-for="opt in polishStyleOptions"
                    :key="opt.value"
                    type="button"
                    @click="polishStyle = opt.value"
                    class="px-3 py-1.5 text-xs rounded-lg border transition-all duration-150 font-medium"
                    :class="polishStyle === opt.value
                      ? 'border-violet-500 bg-violet-500 text-white'
                      : 'border-violet-200 dark:border-violet-600 text-violet-600 dark:text-violet-400 hover:border-violet-400 hover:bg-violet-100 dark:hover:bg-violet-900/30'"
                  >
                    {{ opt.label }}
                  </button>
                </div>
                <button
                  @click="handlePolish"
                  :disabled="isPolishing"
                  class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5"
                  :class="isPolishing
                    ? 'bg-violet-300 dark:bg-violet-700 text-white cursor-not-allowed'
                    : 'bg-violet-500 hover:bg-violet-600 text-white shadow-sm hover:shadow-md shadow-violet-500/30'"
                >
                  <svg v-if="isPolishing" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  {{ isPolishing ? '润色中...' : '开始润色' }}
                </button>
                <span class="text-xs text-gray-400">{{ polishStyleOptions.find(o => o.value === polishStyle)?.desc }}</span>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              封面图片
            </label>
            <input 
              ref="fileInputRef"
              type="file"
              accept="image/*"
              @change="handleCoverUpload"
              class="hidden"
            />
            <div 
              v-if="!article.cover"
              @click="triggerFileInput"
              @drop="handleDrop"
              @dragover="handleDragOver"
              @dragleave="handleDragLeave"
              class="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all"
              :class="isDragging 
                ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' 
                : 'border-gray-300 dark:border-gray-600 hover:border-primary-500 dark:hover:border-primary-500 hover:bg-gray-50 dark:hover:bg-gray-700/30'"
            >
              <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-primary-400 to-primary-500 flex items-center justify-center shadow-lg shadow-primary-500/20">
                <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <p class="text-base font-medium text-gray-700 dark:text-gray-300 mb-1">点击或拖拽上传封面图片</p>
              <p class="text-sm text-gray-400">支持 JPG、PNG、GIF 格式，最大 5MB</p>
            </div>
            <div v-else class="relative group">
              <img 
                :src="article.cover" 
                alt="封面预览"
                class="w-full max-w-md h-48 object-cover rounded-xl border border-gray-200 dark:border-gray-700 shadow-lg"
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
              <p v-if="coverFileName" class="text-sm text-gray-500 dark:text-gray-400 mt-3 flex items-center gap-2">
                <svg class="w-4 h-4 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                {{ coverFileName }}
              </p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 sm:gap-8">
            <div>
              <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                文章分类
              </label>
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
                <button
                  v-for="cat in categories" 
                  :key="cat.id"
                  @click="article.categoryId = cat.id"
                  type="button"
                  class="px-4 py-3 rounded-xl border-2 text-sm font-medium transition-all duration-200 flex items-center justify-center gap-2"
                  :class="article.categoryId === cat.id 
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400' 
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:border-primary-300 dark:hover:border-primary-700 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
                >
                  <span>{{ cat.icon }}</span>
                  <span>{{ cat.name }}</span>
                </button>
              </div>
            </div>

            <div>
              <div class="flex items-center justify-between mb-3">
                <label class="text-sm font-semibold text-gray-700 dark:text-gray-300">
                  添加到专栏
                </label>
                <button
                  type="button"
                  @click="openCreateColumnModal"
                  class="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium flex items-center gap-1"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  新建专栏
                </button>
              </div>
              
              <div v-if="myColumns.length > 0" class="space-y-2">
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="column in myColumns" 
                    :key="column.id"
                    @click="toggleColumn(column.id)"
                    type="button"
                    class="px-3 py-2 text-sm font-medium rounded-xl border-2 transition-all duration-200 flex items-center gap-2"
                    :class="article.columnIds.includes(column.id) 
                      ? 'border-purple-500 bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400' 
                      : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:border-purple-300 dark:hover:border-purple-700 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                    <span class="line-clamp-1">{{ column.title }}</span>
                    <span v-if="column.status === 0" class="text-xs text-yellow-500">(草稿)</span>
                  </button>
                </div>
                <p class="text-xs text-gray-400 flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  已选择 {{ article.columnIds.length }} 个专栏（最多3个）
                </p>
              </div>
              
              <div v-else class="text-center py-6 bg-gray-50 dark:bg-gray-700/30 rounded-xl">
                <svg class="w-10 h-10 mx-auto text-gray-300 dark:text-gray-600 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
                <p class="text-sm text-gray-400 dark:text-gray-500 mb-3">暂无专栏</p>
                <button
                  type="button"
                  @click="openCreateColumnModal"
                  class="text-sm text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium"
                >
                  创建第一个专栏
                </button>
              </div>
            </div>
          </div>

          <div class="mt-6">
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              文章标签
            </label>
              <div class="flex flex-wrap gap-2">
                <button 
                  v-for="tag in availableTags" 
                  :key="tag.id"
                  @click="toggleTag(tag.id)"
                  type="button"
                  class="px-3 py-2 text-sm font-medium rounded-xl border-2 transition-all duration-200"
                  :class="article.tags.includes(tag.id) 
                    ? `border-transparent bg-gradient-to-r ${tag.color} text-white shadow-lg` 
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:border-primary-300 dark:hover:border-primary-700 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
                >
                  {{ tag.name }}
                </button>
              </div>
              <div class="mt-3 flex items-center gap-2">
                <input
                  v-model="customTagInput"
                  type="text"
                  maxlength="20"
                  placeholder="输入新标签后回车"
                  @keydown.enter.prevent="addCustomTag"
                  class="flex-1 px-3 py-2 text-sm border border-gray-200 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-0 focus:border-primary-500"
                />
                <button
                  type="button"
                  @click="addCustomTag"
                  class="px-3 py-2 text-sm rounded-xl bg-primary-500 text-white hover:bg-primary-600 transition-colors"
                >
                  添加
                </button>
              </div>
              <div v-if="customTags.length" class="mt-2 flex flex-wrap gap-2">
                <span
                  v-for="tag in customTags"
                  :key="tag"
                  class="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 text-sm"
                >
                  {{ tag }}
                  <button
                    type="button"
                    @click="removeCustomTag(tag)"
                    class="text-gray-500 hover:text-red-500"
                  >
                    x
                  </button>
                </span>
              </div>
              <p class="text-xs text-gray-400 mt-3 flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
                已选择/输入 {{ article.tags.length + customTags.length }} 个标签
              </p>
            </div>
        </div>

        <div class="flex items-center justify-end gap-3 px-5 sm:px-8 py-5 border-t border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
          <button 
            @click="goBack"
            class="px-6 py-2.5 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors font-medium"
          >
            取消
          </button>
          <button 
            @click="handleSaveDraft"
            :disabled="isSubmitting"
            class="px-6 py-2.5 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-600 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors disabled:opacity-50 font-medium"
          >
            {{ isEditMode ? '保存修改' : '保存草稿' }}
          </button>
          <button 
            @click="handlePublish"
            :disabled="isSubmitting"
            class="px-8 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl hover:from-primary-600 hover:to-primary-700 transition-all disabled:opacity-50 font-semibold shadow-lg shadow-primary-500/30 hover:shadow-xl hover:shadow-primary-500/40"
          >
            {{ isEditMode ? '更新并发布' : '发布文章' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showCreateColumnModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4" @click.self="showCreateColumnModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
          <h3 class="text-lg font-bold text-gray-900 dark:text-white">新建专栏</h3>
          <button @click="showCreateColumnModal = false" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              专栏名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="newColumnName"
              type="text"
              maxlength="100"
              placeholder="请输入专栏名称"
              class="w-full px-4 py-2.5 text-sm border border-gray-200 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              专栏描述
            </label>
            <textarea
              v-model="newColumnDescription"
              rows="3"
              maxlength="500"
              placeholder="请输入专栏描述（可选）"
              class="w-full px-4 py-2.5 text-sm border border-gray-200 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            ></textarea>
          </div>
        </div>
        
        <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700/50 flex items-center justify-end gap-3">
          <button
            @click="showCreateColumnModal = false"
            class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors font-medium text-sm"
          >
            取消
          </button>
          <button
            @click="handleCreateColumn"
            :disabled="isCreatingColumn || !newColumnName.trim()"
            class="px-6 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl hover:from-purple-600 hover:to-pink-600 transition-all disabled:opacity-50 font-medium text-sm shadow-lg"
          >
            {{ isCreatingColumn ? '创建中...' : '创建专栏' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.editor-wrapper {
  position: relative;
  overflow: visible;
}

.editor-wrapper:hover {
  overflow: visible;
}

.vditor-container {
  --border-radius: 0;
}

.vditor {
  border: none !important;
  overflow: visible !important;
}

.vditor--dark {
  background-color: #374151 !important;
}

.vditor-toolbar {
  border-bottom: 1px solid #e5e7eb !important;
  background-color: #f9fafb !important;
  padding: 10px 16px !important;
  position: relative;
  z-index: 100;
  overflow: visible !important;
}

.vditor--dark .vditor-toolbar {
  border-bottom-color: #4b5563 !important;
  background-color: #1f2937 !important;
}

.vditor-toolbar__item {
  color: #4b5563 !important;
  position: relative;
}

.vditor--dark .vditor-toolbar__item {
  color: #9ca3af !important;
}

.vditor-toolbar__item:hover {
  background-color: #e5e7eb !important;
  border-radius: 8px !important;
}

.vditor--dark .vditor-toolbar__item:hover {
  background-color: #374151 !important;
}

.vditor-toolbar__item--current {
  background-color: #e5e7eb !important;
  border-radius: 8px !important;
}

.vditor--dark .vditor-toolbar__item--current {
  background-color: #374151 !important;
}

.vditor-toolbar__svg {
  position: relative;
  z-index: 101;
}

.vditor-panel {
  position: absolute !important;
  z-index: 1000 !important;
  top: 100% !important;
  margin-top: 4px !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15) !important;
}

.vditor-ir {
  background-color: #ffffff !important;
}

.vditor--dark .vditor-ir {
  background-color: #374151 !important;
}

.vditor-ir pre.vditor-reset {
  color: #1f2937 !important;
  font-size: 15px !important;
  line-height: 1.8 !important;
  padding: 20px !important;
}

.vditor--dark .vditor-ir pre.vditor-reset {
  color: #f3f4f6 !important;
}

.vditor-ir pre.vditor-reset:focus {
  outline: none !important;
}

.vditor-ir pre.vditor-reset::-webkit-scrollbar {
  width: 6px;
}

.vditor-ir pre.vditor-reset::-webkit-scrollbar-thumb {
  background-color: #d1d5db;
  border-radius: 3px;
}

.vditor--dark .vditor-ir pre.vditor-reset::-webkit-scrollbar-thumb {
  background-color: #4b5563;
}

.vditor-ir pre.vditor-reset::-webkit-scrollbar-track {
  background-color: transparent;
}

.vditor-counter {
  color: #6b7280 !important;
  padding: 6px 16px !important;
}

.vditor--dark .vditor-counter {
  color: #9ca3af !important;
}

.vditor-h1 {
  font-size: 1.75rem !important;
  font-weight: 700 !important;
  border-bottom: 2px solid #e5e7eb !important;
  padding-bottom: 0.4rem !important;
  margin: 1.5rem 0 0.75rem !important;
}

.vditor--dark .vditor-h1 {
  border-bottom-color: #4b5563 !important;
}

.vditor-h2 {
  font-size: 1.5rem !important;
  font-weight: 700 !important;
  border-bottom: 1px solid #e5e7eb !important;
  padding-bottom: 0.3rem !important;
  margin: 1.25rem 0 0.5rem !important;
}

.vditor--dark .vditor-h2 {
  border-bottom-color: #4b5563 !important;
}

.vditor-h3 {
  font-size: 1.25rem !important;
  font-weight: 600 !important;
  margin: 1rem 0 0.5rem !important;
}

.vditor-h4 {
  font-size: 1.125rem !important;
  font-weight: 600 !important;
  margin: 0.75rem 0 0.5rem !important;
}

.vditor-strong {
  font-weight: 700 !important;
}

.vditor-em {
  font-style: italic !important;
}

.vditor-link {
  color: #6366f1 !important;
  text-decoration: underline !important;
}

.vditor code:not(.hljs):not(.highlight-chroma) {
  background-color: rgba(99, 102, 241, 0.15) !important;
  color: #6366f1 !important;
  padding: 0.2rem 0.5rem !important;
  border-radius: 6px !important;
  font-size: 0.875em !important;
}

.vditor--dark code:not(.hljs):not(.highlight-chroma) {
  background-color: rgba(99, 102, 241, 0.25) !important;
}

.vditor pre code {
  background: none !important;
}

.vditor blockquote {
  border-left: 4px solid #6366f1 !important;
  padding: 0.75rem 0 0.75rem 1.25rem !important;
  margin: 0.75rem 0 !important;
  color: #6b7280 !important;
  background-color: rgba(99, 102, 241, 0.05) !important;
  border-radius: 0 8px 8px 0 !important;
}

.vditor--dark blockquote {
  background-color: rgba(99, 102, 241, 0.1) !important;
}

.vditor-hr {
  border-top: 2px solid #e5e7eb !important;
  margin: 1.5rem 0 !important;
}

.vditor--dark .vditor-hr {
  border-top-color: #4b5563 !important;
}

.vditor table {
  border-collapse: collapse !important;
  margin: 1rem 0 !important;
  border-radius: 8px !important;
  overflow: hidden !important;
}

.vditor table th,
.vditor table td {
  border: 1px solid #e5e7eb !important;
  padding: 0.75rem 1rem !important;
}

.vditor--dark table th,
.vditor--dark table td {
  border-color: #4b5563 !important;
}

.vditor table th {
  background-color: #f3f4f6 !important;
  font-weight: 600 !important;
}

.vditor--dark table th {
  background-color: #1f2937 !important;
}
</style>
