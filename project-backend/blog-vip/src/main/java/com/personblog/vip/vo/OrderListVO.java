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
 * 订单列表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单列表项")
public class OrderListVO {

    @Schema(description = "订单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "业务快照JSON（套餐名称等）")
    private String bizSnapshot;

    @Schema(description = "实际冻结积分")
    private Integer actualPoints;

    @Schema(description = "订单状态：0-待确认 1-已冻结 2-已完成 3-已取消 4-已关闭")
    private Short status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
