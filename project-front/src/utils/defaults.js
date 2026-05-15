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

/**
 * 根据用户 ID 获取对应头像
 * @param {string|number} userId
 * @param {string|null} userAvatar - 用户自身的头像 URL
 * @returns {string} 头像 URL
 */
export const getAvatar = (userId, userAvatar) => {
  return userAvatar || DEFAULT_AVATAR
}
