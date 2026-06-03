package com.personblog.point.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.point.BizService.PointBizService;
import com.personblog.point.dto.PointLogQueryDTO;
import com.personblog.point.vo.PointBalanceVO;
import com.personblog.point.vo.PointLogVO;
import com.personblog.point.vo.PointRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分系统控制器
 *
 * @author LSH
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
@Tag(name = "积分系统", description = "积分查询、排行榜、流水等接口")
public class PointController {

    private final PointBizService pointBizService;

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
