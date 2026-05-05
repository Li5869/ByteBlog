package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.dto.AdminLogQueryDTO;
import com.personblog.common.entity.AdminLog;
import com.personblog.common.vo.AdminLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 管理员操作日志表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {

    /**
     * 分页查询操作日志（关联管理员名称）
     */
    Page<AdminLogVO> selectAdminLogPage(Page<AdminLogVO> page, @Param("dto") AdminLogQueryDTO dto);
}
