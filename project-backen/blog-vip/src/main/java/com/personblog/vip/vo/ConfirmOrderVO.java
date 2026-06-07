package com.personblog.vip.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 确认购买 VO（TCC Try + Confirm 完成后的响应）
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "确认购买结果")
public class ConfirmOrderVO {

    @Schema(description = "订单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @Schema(description = "订单状态：1-已冻结 2-已完成")
    private Short status;

    @Schema(description = "实际冻结积分")
    private Integer actualPoints;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
