package com.personblog.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.dto.AdminLogQueryDTO;
import com.personblog.admin.entity.AdminLog;
import com.personblog.admin.vo.AdminLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员操作日志表 Mapper 接口
 *
 * @author LSH
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {

    Page<AdminLogVO> selectAdminLogPage(Page<AdminLogVO> page, @Param("dto") AdminLogQueryDTO dto);
}
