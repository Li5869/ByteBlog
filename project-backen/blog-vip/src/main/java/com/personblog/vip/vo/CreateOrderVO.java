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
 * 创建预订单 VO（精简版，去除前端已知/不关心的字段）
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建预订单结果")
public class CreateOrderVO {

    @Schema(description = "订单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "业务快照JSON")
    private String bizSnapshot;

    @Schema(description = "套餐积分原价")
    private Integer pointsCost;

    @Schema(description = "优惠券名称（null表示未使用）")
    private String couponName;

    @Schema(description = "优惠券减免积分数")
    private Integer couponDiscount;

    @Schema(description = "实际冻结积分 = pointsCost - couponDiscount")
    private Integer actualPoints;

    @Schema(description = "订单状态：0-待确认")
    private Short status;

    @Schema(description = "订单过期时间（创建后30分钟）")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
