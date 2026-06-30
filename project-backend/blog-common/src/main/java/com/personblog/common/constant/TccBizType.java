package com.personblog.common.constant;

/**
 * TCC 分布式事务业务类型常量
 *
 * @author LSH
 * @since 2026-06-08
 */
public class TccBizType {

    private TccBizType() {}

    /** 积分冻结 */
    public static final String POINT_FREEZE = "POINT_FREEZE";

    /** 优惠券冻结 */
    public static final String COUPON_FREEZE = "COUPON_FREEZE";
}
