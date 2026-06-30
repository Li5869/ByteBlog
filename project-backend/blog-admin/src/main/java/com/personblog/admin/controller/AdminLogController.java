package com.personblog.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.dto.AdminLogQueryDTO;
import com.personblog.admin.service.IAdminLogService;
import com.personblog.admin.vo.AdminLogVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端操作日志控制器
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@Tag(name = "管理端-操作日志", description = "管理后台的操作日志查询接口")
public class AdminLogController {

    private final IAdminLogService adminLogService;

    @Operation(summary = "获取操作日志列表", description = "分页查询操作日志，支持关键词搜索和多种筛选条件")
    @PostMapping("/list")
    public JsonData<Page<AdminLogVO>> getLogPage(@RequestBody AdminLogQueryDTO dto) {
        Page<AdminLogVO> page = adminLogService.getAdminLogPage(dto);
        return JsonData.buildSuccess(page);
    }
}
