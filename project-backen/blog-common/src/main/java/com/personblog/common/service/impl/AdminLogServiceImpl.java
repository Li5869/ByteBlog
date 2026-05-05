package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.dto.AdminLogQueryDTO;
import com.personblog.common.entity.AdminLog;
import com.personblog.common.mapper.AdminLogMapper;
import com.personblog.common.service.IAdminLogService;
import com.personblog.common.vo.AdminLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理员操作日志表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl extends ServiceImpl<AdminLogMapper, AdminLog> implements IAdminLogService {

    private final AdminLogMapper adminLogMapper;

    @Override
    public Page<AdminLogVO> getAdminLogPage(AdminLogQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);
        Page<AdminLogVO> page = new Page<>(current, size);
        return adminLogMapper.selectAdminLogPage(page, dto);
    }
}
