package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用订单表 Mapper 接口
 * @author LSH
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
