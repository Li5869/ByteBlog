package com.personblog.interaction.controller;

import com.personblog.common.result.JsonData;
import com.personblog.interaction.bizService.BizLikeService;
import com.personblog.interaction.dto.LikedDTO;
import com.personblog.interaction.vo.LikedVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interaction/likes")
@RequiredArgsConstructor
@Tag(name = "点赞相关",description = "点赞互动相关")
public class LikeController {
    private final BizLikeService likeService;
    @PostMapping("/toggle")
    @Operation(description = "点赞或取消点赞")
    public JsonData<LikedVO> doLike(@RequestBody LikedDTO dto){
       LikedVO vo = likeService.doLike(dto);
       return JsonData.buildSuccess(vo);
    }
}
