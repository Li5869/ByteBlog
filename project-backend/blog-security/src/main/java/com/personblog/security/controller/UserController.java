package com.personblog.security.controller;


import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.security.dto.UpdateProfileDTO;
import com.personblog.security.service.IUserService;
import com.personblog.security.vo.ActiveUserVO;
import com.personblog.security.vo.AuthorInfoVO;
import com.personblog.security.vo.UserProfileStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户管理", description = "用户个人中心相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "获取当前用户个人中心聚合信息", description = "返回当前登录用户在「我的」页所需的资料与统计数据")
    @GetMapping("/me")
    public JsonData<UserProfileStatsVO> getUserProfileStats() {
        Long userId = UserContextHolder.getUserId();
        UserProfileStatsVO profileStats = userService.getUserProfileStats(userId);
        return JsonData.buildSuccess(profileStats);
    }

    @Operation(summary = "更新个人资料", description = "更新当前用户的个人资料信息")
    @PutMapping("/profile")
    public JsonData<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = UserContextHolder.getUserId();
        userService.updateProfile(userId, dto);
        return JsonData.buildSuccess();
    }

    /**
     * 获取活跃博主列表
     * 按文章数量排序，用于侧边栏展示
     * @param size 返回数量，默认4，最大20
     * @return 活跃博主列表
     */
    @Operation(summary = "获取活跃博主", description = "获取活跃博主列表，按文章数量排序")
    @GetMapping("/active")
    public JsonData<List<ActiveUserVO>> getActiveUsers(
            @Parameter(description = "返回数量，默认4，最大20")
            @RequestParam(required = false) Integer size) {
        List<ActiveUserVO> users = userService.getActiveUsers(size);
        return JsonData.buildSuccess(users);
    }

    /**
     * 获取作者信息
     * 用于文章详情页展示作者信息
     * @param id 用户ID
     * @return 作者信息
     */
    @Operation(summary = "获取作者信息", description = "获取指定作者的详细信息及统计数据")
    @GetMapping("/users/{id}")
    public JsonData<AuthorInfoVO> getAuthorInfo(
            @Parameter(description = "用户ID")
            @PathVariable Long id) {
        AuthorInfoVO authorInfo = userService.getAuthorInfo(id);
        return JsonData.buildSuccess(authorInfo);
    }

}
