package com.personblog.push.constant;

/**
 * 推送模块常量定义
 *
 * @author LSH
 */
public class PushConstants {

    // ==================== Redis Pub/Sub Topic ====================

    /** Redis Pub/Sub 主题名称，用于跨节点消息分发 */
    public static final String TOPIC_NAME = "byteblog:push";

    // ==================== 推送通道（Channel）类型 ====================

    /** WebSocket 单点推送通道 */
    public static final String CHANNEL_WS = "ws";

    /** SSE 单点推送通道 */
    public static final String CHANNEL_SSE = "sse";

    /** SSE 粉丝广播推送通道 */
    public static final String CHANNEL_SSE_BROADCAST = "sse:broadcast";

    // ==================== WebSocket 消息类型 ====================

    /** 欢迎消息：用户连接成功后推送 */
    public static final String TYPE_WELCOME = "welcome";

    /** 用户上下线通知：通知粉丝某用户上线或下线 */
    public static final String TYPE_USER_ONLINE = "user_online";

    /** 在线状态查询结果：返回一批用户的在线状态 */
    public static final String TYPE_ONLINE_STATUS = "online_status";

    /** 错误消息：消息格式错误等 */
    public static final String TYPE_ERROR = "error";

    /** 查询在线状态：客户端发起的一批用户在线状态查询 */
    public static final String TYPE_QUERY_ONLINE = "query_online";

    /** 私信消息：用户间实时私信推送 */
    public static final String TYPE_PRIVATE_MESSAGE = "private_message";

    /** 未读消息数更新：推送未读数变化 */
    public static final String TYPE_UNREAD_UPDATE = "unread_update";
}