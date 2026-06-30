package com.personblog.vip.controller;

import com.personblog.common.result.JsonData;
import com.personblog.vip.bizService.VipPlanBizService;
import com.personblog.vip.vo.VipPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * VIP 套餐接口
 * @author LSH
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "VIP套餐", description = "VIP套餐查询接口")
@RequestMapping("/vip/plans")
public class VipPlanController {

    private final VipPlanBizService vipPlanBizService;

    /**
     * 获取所有上架套餐列表
     * GET /api/vip/plans
     */
    @GetMapping
    @Operation(summary = "获取套餐列表", description = "查询所有上架的VIP套餐，按排序权重升序")
    public JsonData<List<VipPlanVO>> listPlans() {
        List<VipPlanVO> plans = vipPlanBizService.listPlans();
        return JsonData.buildSuccess(plans);
    }
}
