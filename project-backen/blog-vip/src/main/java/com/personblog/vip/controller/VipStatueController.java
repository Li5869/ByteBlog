package com.personblog.vip.controller;

import com.personblog.common.result.JsonData;
import com.personblog.vip.bizService.VipMembershipBizService;
import com.personblog.vip.vo.VipInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员状态接口
 * @author LSH
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "会员状态", description = "VIP会员状态查询接口")
@RequestMapping("/vip/membership")
public class VipStatueController {

    private final VipMembershipBizService vipMembershipBizService;

    /**
     * 获取当前用户会员信息
     * GET /api/vip/membership
     */
    @GetMapping()
    @Operation(summary = "获取当前会员信息", description = "查询当前登录用户的VIP会员状态")
    public JsonData<VipInfoVO> getMembership() {
        VipInfoVO vo = vipMembershipBizService.getMembership();
        return JsonData.buildSuccess(vo);
    }
}
