package com.personblog.common.constant;

/**
 * 业务类型常量（通用订单表 tb_order.biz_type 字段）
 * @author LSH
 */
public class BizType {

    private BizType() {}

    /** VIP 会员，订单号前缀：VIP */
    public static final short VIP = 1;

    /** 付费专栏（预留），订单号前缀：COL */
    public static final short PAID_COLUMN = 2;
}
