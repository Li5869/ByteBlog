package com.personblog.article.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅用户信息返回对象
 *
 * @author LSH
 */
@Data
@Builder
@Schema(description = "订阅用户信息返回对象")
public class SubscriberVO {

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "订阅时间")
    private LocalDateTime subscribedAt;
}
