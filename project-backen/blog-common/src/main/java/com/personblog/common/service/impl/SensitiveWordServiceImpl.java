package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.entity.SensitiveWord;
import com.personblog.common.mapper.SensitiveWordMapper;
import com.personblog.common.service.ISensitiveWordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 敏感词表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements ISensitiveWordService {

}
