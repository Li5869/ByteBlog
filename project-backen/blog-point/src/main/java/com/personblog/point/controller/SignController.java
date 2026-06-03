package com.personblog.point.controller;

import com.personblog.common.result.JsonData;
import com.personblog.point.BizService.SignBizService;
import com.personblog.point.vo.SignResultVO;
import com.personblog.point.vo.SignStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到控制器
 *
 * @author LSH
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
@Tag(name = "签到系统", description = "每日签到、签到状态查询等接口")
public class SignController {

    private final SignBizService signBizService;

    /**
     * 执行签到
     */
    @PostMapping
    @Operation(summary = "执行签到", description = "用户每日签到获取积分")
    public JsonData<SignResultVO> doSign() {
        SignResultVO vo = signBizService.signup();
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取签到状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取签到状态", description = "查询当月签到状态和日历")
    public JsonData<SignStatusVO> getSignStatus() {
        SignStatusVO vo = signBizService.getStatus();
        return JsonData.buildSuccess(vo);
    }
}
