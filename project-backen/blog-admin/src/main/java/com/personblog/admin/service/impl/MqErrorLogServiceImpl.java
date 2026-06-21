package com.personblog.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.admin.mapper.MqErrorLogMapper;
import com.personblog.admin.service.IMqErrorLogService;
import com.personblog.common.entity.MqErrorLog;
import org.springframework.stereotype.Service;

/**
 * MQ消息死信队列错误日志 服务实现类
 *
 * @author LSH
 */
@Service
public class MqErrorLogServiceImpl extends ServiceImpl<MqErrorLogMapper, MqErrorLog> implements IMqErrorLogService {

}
