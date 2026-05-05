package com.personblog.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.dto.AdminLogQueryDTO;
import com.personblog.common.entity.AdminLog;
import com.personblog.common.vo.AdminLogVO;

/**
 * <p>
 * 管理员操作日志表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface IAdminLogService extends IService<AdminLog> {

    /**
     * 分页查询操作日志
     */
    Page<AdminLogVO> getAdminLogPage(AdminLogQueryDTO dto);
}
