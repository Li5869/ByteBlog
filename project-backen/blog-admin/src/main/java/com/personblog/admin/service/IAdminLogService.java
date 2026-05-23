package com.personblog.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.admin.dto.AdminLogQueryDTO;
import com.personblog.admin.entity.AdminLog;
import com.personblog.admin.vo.AdminLogVO;

/**
 * 管理员操作日志表 服务类
 *
 * @author LSH
 */
public interface IAdminLogService extends IService<AdminLog> {

    Page<AdminLogVO> getAdminLogPage(AdminLogQueryDTO dto);
}
