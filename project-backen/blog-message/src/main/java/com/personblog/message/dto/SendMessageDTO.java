package com.personblog.message.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送消息请求参数")
public class SendMessageDTO {

    @NotNull(message = "接收者ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "接收者ID")
    private Long receiverId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    @Schema(description = "消息内容")
    private String content;
}
