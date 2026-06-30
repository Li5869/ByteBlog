package com.personblog.vip.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情")
public class OrderVO {

    @Schema(description = "订单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "业务类型：1-VIP会员")
    private Short bizType;

    @Schema(description = "业务ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;

    @Schema(description = "业务快照JSON")
    private String bizSnapshot;

    @Schema(description = "积分原价")
    private Integer pointsCost;

    @Schema(description = "优惠券ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券减免积分数")
    private Integer couponDiscount;

    @Schema(description = "实际冻结积分")
    private Integer actualPoints;

    @Schema(description = "订单状态：0-待确认 1-已冻结 2-已完成 3-已取消 4-已关闭")
    private Short status;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "订单时间线")
    private List<TimelineItem> timeline;

    /**
     * 时间线条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时间线条目")
    public static class TimelineItem {

        @Schema(description = "操作时间")
        private LocalDateTime time;
    }
}
