package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.entity.TccTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC 分布式事务记录表 Mapper
 *
 * @author LSH
 * @since 2026-06-08
 */
@Mapper
public interface TccTransactionMapper extends BaseMapper<TccTransaction> {
}
