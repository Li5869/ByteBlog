package com.personblog.interaction.controller;

import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.FollowDTO;
import com.personblog.interaction.service.FollowService;
import com.personblog.interaction.vo.FollowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interaction/follows")
@RequiredArgsConstructor
@Tag(name = "关注相关",description = "关注")
public class FollowController {
    private final FollowService followService;
    private final UseApi userApi;

    @PostMapping("/toggle")
    @Operation(description = "关注/取关")
    public JsonData<FollowVO> doFollow(@RequestBody FollowDTO dto){
        FollowVO vo = followService.doFollow(dto);
        return JsonData.buildSuccess(vo);
    }

    @PostMapping("/check-batch")
    @Operation(description = "批量查询关注状态,返回的是已经关注的目标id")
    public JsonData<List<Long>> checkBatchFollowStatus(@RequestBody List<Long> followingIds) {
        if (followingIds == null || followingIds.isEmpty()) {
            return JsonData.buildSuccess(List.of());
        }
        List<Long> result = followService.checkBatchFollowStatus(followingIds);
        return JsonData.buildSuccess(result);
    }

    @GetMapping("/following")
    @Operation(description = "获取当前用户关注的用户列表")
    public JsonData<List<UserDTO>> getFollowingUsers() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return JsonData.buildSuccess(List.of());
        }
        List<Long> followingIds = followService.getFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return JsonData.buildSuccess(List.of());
        }
        List<UserDTO> result = userApi.getUserInfo(followingIds);
        return JsonData.buildSuccess(result);
    }
}
