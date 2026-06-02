package com.personblog.point.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.point.BizService.PointBizService;
import com.personblog.point.BizService.SignBizService;
import com.personblog.point.dto.PointLogQueryDTO;
import com.personblog.point.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 积分系统控制器
 *
 * @author LSH
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
@Tag(name = "积分系统", description = "积分签到、查询、排行榜等接口")
public class PointController {
    private final SignBizService signBizService;
    private final PointBizService pointBizService;

    /**
     * 执行签到
     */
    @PostMapping("/sign")
    @Operation(summary = "执行签到", description = "用户每日签到获取积分")
    public JsonData<SignResultVO> doSign() {
        SignResultVO vo = signBizService.signup();
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取签到状态
     */
    @GetMapping("/sign/status")
    @Operation(summary = "获取签到状态", description = "查询当月签到状态和日历")
    public JsonData<SignStatusVO> getSignStatus() {
        SignStatusVO vo = signBizService.getStatus();
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取积分余额
     */
    @GetMapping("/balance")
    @Operation(summary = "获取积分余额", description = "查询用户积分余额和排名")
    public JsonData<PointBalanceVO> getBalance() {
        PointBalanceVO vo = pointBizService.getBalance();
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取积分排行榜
     */
    @GetMapping("/rank")
    @Operation(summary = "获取积分排行榜", description = "查询月度积分排行榜")
    public JsonData<PointRankVO> getRankList(@RequestParam(defaultValue = "10") Integer topN) {
        PointRankVO vo = pointBizService.getRankList(topN);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取积分流水
     */
    @GetMapping("/logs")
    @Operation(summary = "获取积分流水", description = "查询积分变动记录")
    public JsonData<Page<PointLogVO>> getPointLogs(PointLogQueryDTO queryDTO) {
        Page<PointLogVO> page = pointBizService.getPointLogs(queryDTO);
        return JsonData.buildSuccess(page);
    }
}
