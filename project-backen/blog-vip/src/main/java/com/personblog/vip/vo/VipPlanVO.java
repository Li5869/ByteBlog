package com.personblog.vip.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VIP 套餐展示 VO
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "VIP套餐信息")
public class VipPlanVO {

    @Schema(description = "套餐ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "套餐编码：MONTH/QUARTER/YEAR")
    private String planCode;

    @Schema(description = "套餐名称：月卡/季卡/年卡")
    private String planName;

    @Schema(description = "会员时长（月）")
    private Integer durationMonths;

    @Schema(description = "积分价格")
    private Integer pointsPrice;

    @Schema(description = "排序权重（数值越小越靠前）")
    private Integer sortOrder;
}
