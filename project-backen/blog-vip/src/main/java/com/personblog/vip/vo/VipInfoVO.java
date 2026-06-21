package com.personblog.vip.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员状态 VO
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员状态信息")
public class VipInfoVO {

    @Schema(description = "是否为VIP")
    private Boolean isVip;

    @Schema(description = "会员等级：0-普通 1-VIP")
    private Short vipLevel;

    @Schema(description = "会员期开始时间")
    private LocalDateTime startTime;

    @Schema(description = "会员期到期时间")
    private LocalDateTime endTime;

    @Schema(description = "累计开通月数")
    private Integer totalMonths;

    @Schema(description = "剩余天数")
    private Integer remainDays;
}
