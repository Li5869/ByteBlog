package com.personblog.api.couponAPI.vo;

import lombok.Data;

@Data
public class BestCouponVO {
    //优惠券id
    private Long CouponId;
    //优惠券名称
    private String CouponName;
    //优惠券优惠价格
    private Long CouponDiscount;
}
