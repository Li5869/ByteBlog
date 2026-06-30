package com.personblog.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.admin.entity.SensitiveWord;
import com.personblog.admin.mapper.SensitiveWordMapper;
import com.personblog.admin.service.ISensitiveWordService;
import org.springframework.stereotype.Service;

/**
 * 敏感词表 服务实现类
 *
 * @author LSH
 */
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements ISensitiveWordService {

}
