package com.personblog.point.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.point.entity.SignRecord;
import com.personblog.point.mapper.SignRecordMapper;
import com.personblog.point.service.SignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 签到记录表 服务实现类
 *
 * @author LSH
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignRecordServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements SignRecordService {
}
