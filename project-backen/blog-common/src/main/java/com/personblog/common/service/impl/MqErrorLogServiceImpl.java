package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.entity.MqErrorLog;
import com.personblog.common.mapper.MqErrorLogMapper;
import com.personblog.common.service.IMqErrorLogService;
import org.springframework.stereotype.Service;

/**
 * MQ消息死信队列错误日志 服务实现类
 *
 * @author LSH
 */
@Service
public class MqErrorLogServiceImpl extends ServiceImpl<MqErrorLogMapper, MqErrorLog> implements IMqErrorLogService {

}
