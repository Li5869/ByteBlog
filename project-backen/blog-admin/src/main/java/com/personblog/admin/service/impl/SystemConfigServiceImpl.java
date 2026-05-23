package com.personblog.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.admin.entity.SystemConfig;
import com.personblog.admin.mapper.SystemConfigMapper;
import com.personblog.admin.service.ISystemConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置表 服务实现类
 *
 * @author LSH
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements ISystemConfigService {

}
