package com.personblog.security.controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.adminLog.RecordLog;
import com.personblog.common.result.JsonData;
import com.personblog.security.dto.AdminUserQueryDTO;
import com.personblog.security.entity.User;
import com.personblog.security.service.IUserService;
import com.personblog.security.vo.AdminUserDetailVO;
import com.personblog.security.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户管理控制器
 * 支持用户的列表查询、详情查看、信息编辑、封禁/解封和删除
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理端-用户管理", description = "管理后台的用户管理接口")
public class AdminUserController {

    private final IUserService userService;

    /**
     * 获取用户列表（分页）
     * 支持关键词搜索和状态筛选
     */
    @Operation(summary = "获取用户列表", description = "分页查询用户列表，支持关键词搜索和状态筛选")
    @PostMapping("/list")
    public JsonData<Page<AdminUserVO>> getUserPage(@RequestBody AdminUserQueryDTO dto) {
        Page<AdminUserVO> page = userService.getAdminUserPage(dto);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "获取指定用户的完整信息")
    @GetMapping("/{id}")
    public JsonData<AdminUserDetailVO> getUserDetail(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        AdminUserDetailVO detail = userService.getAdminUserDetail(id);
        return JsonData.buildSuccess(detail);
    }

    /**
     * 更新用户信息
     */
    @RecordLog(Type = "update", businessType = "user", description = "更新用户信息")
    @Operation(summary = "更新用户信息", description = "管理员编辑用户的资料信息")
    @PutMapping("/{id}")
    public JsonData<Void> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody User user) {
        userService.updateUserByAdmin(id, user);
        return JsonData.buildSuccess();
    }

    /**
     * 封禁/解封用户
     */
    @RecordLog(Type = "update", businessType = "user", description = "封禁/解封用户")
    @Operation(summary = "封禁/解封用户", description = "管理员封禁或解封指定用户")
    @PutMapping("/{id}/status")
    public JsonData<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody User user) {
        userService.updateUserStatusByAdmin(id, user.getStatus());
        return JsonData.buildSuccess();
    }

    /**
     * 删除用户
     */
    @RecordLog(Type = "delete", businessType = "user", description = "删除用户")
    @Operation(summary = "删除用户", description = "管理员删除指定用户")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return JsonData.buildSuccess();
    }
}
