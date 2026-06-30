package com.personblog.common.utils;

/**
 * 消息工具类
 * 
 * @author LSH
 */
public class MessageUtil {

    private static final int MAX_CONTENT_LENGTH = 50;

    /**
     * 截断消息内容用于显示
     * 
     * @param content 原始内容
     * @return 截断后的内容
     */
    public static String truncateContent(String content) {
        if (content == null) {
            return "";
        }

        // 移除图片Markdown标记
        String text = content.replaceAll("!\\[image\\]\\(.*?\\)", "[图片]");

        // 截断过长的文本
        if (text.length() > MAX_CONTENT_LENGTH) {
            return text.substring(0, MAX_CONTENT_LENGTH) + "...";
        }

        return text;
    }

    /**
     * 判断消息是否包含图片
     * 
     * @param content 消息内容
     * @return 是否包含图片
     */
    public static boolean containsImage(String content) {
        return content != null && content.contains("![image]");
    }
}