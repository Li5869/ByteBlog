package com.personblog.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.admin.entity.MqErrorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ消息死信队列错误日志 Mapper 接口
 *
 * @author LSH
 */
@Mapper
public interface MqErrorLogMapper extends BaseMapper<MqErrorLog> {

}
