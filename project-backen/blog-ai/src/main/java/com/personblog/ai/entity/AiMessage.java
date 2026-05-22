package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI消息表
 *
 * @author LSH
 */
@Data
@TableName("tb_ai_message")
public class AiMessage {

    /** 消息ID */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 会话ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    /** 角色 */
    private String role;

    /** 思考内容 */
    private String thinking;

    /** 消息内容 */
    private String content;

    /** 工具调用信息*/
    private String toolCalls;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
