package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.entity.SystemConfig;
import com.personblog.common.mapper.SystemConfigMapper;
import com.personblog.common.service.ISystemConfigService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统配置表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements ISystemConfigService {

}
