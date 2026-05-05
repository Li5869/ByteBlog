/**
 * 全局默认资源常量
 * 使用本地 SVG，避免外部依赖，彻底解决网络加载失败问题
 */

import defaultAvatarSvg from '@/assets/default-avatar.svg'
import defaultCoverSvg from '@/assets/default-cover.svg'

/** 默认用户头像（渐变紫色 + 人形轮廓） */
export const DEFAULT_AVATAR = defaultAvatarSvg

/** 默认文章封面（深色科技风） */
export const DEFAULT_COVER = defaultCoverSvg

/** AI 助手用户 ID（与后端 SystemConstants.AI_USER_ID 保持一致） */
export const AI_USER_ID = '-1'

/** AI 助手专用头像（可爱机器人 SVG） */
export const AI_AVATAR = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#8B5CF6"/>
      <stop offset="100%" style="stop-color:#3B82F6"/>
    </linearGradient>
    <linearGradient id="screen" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#C4B5FD"/>
      <stop offset="100%" style="stop-color:#93C5FD"/>
    </linearGradient>
  </defs>
  <rect x="16" y="20" width="96" height="88" rx="20" fill="url(#bg)"/>
  <rect x="60" y="4" width="8" height="20" rx="4" fill="#6366F1"/>
  <circle cx="64" cy="8" r="6" fill="#F472B6"/>
  <circle cx="64" cy="8" r="3" fill="#FCE7F3" opacity="0.6"/>
  <rect x="6" y="44" width="14" height="24" rx="6" fill="#6366F1"/>
  <rect x="108" y="44" width="14" height="24" rx="6" fill="#6366F1"/>
  <rect x="26" y="38" width="76" height="52" rx="10" fill="url(#screen)"/>
  <circle cx="48" cy="56" r="10" fill="#1E1B4B"/>
  <circle cx="52" cy="52" r="4" fill="white" opacity="0.7"/>
  <circle cx="80" cy="56" r="10" fill="#1E1B4B"/>
  <circle cx="84" cy="52" r="4" fill="white" opacity="0.7"/>
  <rect x="46" y="70" width="36" height="8" rx="4" fill="#6366F1"/>
  <rect x="50" y="72" width="6" height="4" rx="1" fill="white"/>
  <rect x="61" y="72" width="6" height="4" rx="1" fill="white"/>
  <rect x="72" y="72" width="6" height="4" rx="1" fill="white"/>
  <rect x="50" y="108" width="28" height="8" rx="4" fill="#4F46E5"/>
  <rect x="42" y="114" width="44" height="10" rx="5" fill="#6366F1"/>
</svg>
`)))

/**
 * 根据用户 ID 获取对应头像
 * AI 用户返回 AI 专属头像，普通用户返回其头像或默认头像
 * @param {string|number} userId
 * @param {string|null} userAvatar - 用户自身的头像 URL
 * @returns {string} 头像 URL
 */
export const getAvatar = (userId, userAvatar) => {
  if (userId && String(userId) === String(AI_USER_ID)) {
    return AI_AVATAR
  }
  return userAvatar || DEFAULT_AVATAR
}
