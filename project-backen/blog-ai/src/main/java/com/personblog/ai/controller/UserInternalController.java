package com.personblog.ai.controller;

import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 用户信息内部API控制器
 * 供 Python AI 服务调用，用于获取用户信息
 *
 * @author LSH
 */
@Tag(name = "用户信息内部API", description = "供Python AI服务调用的用户信息接口")
@Slf4j
@RestController
@RequestMapping("/ai/internal/user")
@RequiredArgsConstructor
public class UserInternalController {

    private final UseApi useApi;

    @Operation(summary = "获取用户信息", description = "Python服务根据用户ID获取用户详细信息")
    @GetMapping("/info")
    public JsonData<UserDTO> getUserInfo(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        log.debug("[UserInternal] 获取用户信息, userId={}", userId);
        List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(userId));
        if (users == null || users.isEmpty()) {
            log.warn("[UserInternal] 用户不存在, userId={}", userId);
            return JsonData.buildSuccess(null);
        }
        return JsonData.buildSuccess(users.getFirst());
    }
}
