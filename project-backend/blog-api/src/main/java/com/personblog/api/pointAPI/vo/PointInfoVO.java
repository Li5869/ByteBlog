package com.personblog.api.pointAPI.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PointInfoVO {
    /** 可用积分 */
    private Long availablePoints;

    /** 冻结积分（积分消费预留） */
    private Long frozenPoints;
}
