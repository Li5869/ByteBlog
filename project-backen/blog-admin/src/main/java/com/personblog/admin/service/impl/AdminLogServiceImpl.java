package com.personblog.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.admin.dto.AdminLogQueryDTO;
import com.personblog.admin.entity.AdminLog;
import com.personblog.admin.mapper.AdminLogMapper;
import com.personblog.admin.service.IAdminLogService;
import com.personblog.admin.vo.AdminLogVO;
import com.personblog.api.adminAPI.AdminLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理员操作日志表 服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl extends ServiceImpl<AdminLogMapper, AdminLog> implements IAdminLogService, AdminLogApi {

    private final AdminLogMapper adminLogMapper;

    @Override
    public Page<AdminLogVO> getAdminLogPage(AdminLogQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);
        Page<AdminLogVO> page = new Page<>(current, size);
        return adminLogMapper.selectAdminLogPage(page, dto);
    }
}
