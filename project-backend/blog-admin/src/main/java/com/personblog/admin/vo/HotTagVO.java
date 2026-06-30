package com.personblog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门标签VO
 *
 * @author LSH
 */
@Data
@Schema(description = "热门标签返回对象")
public class HotTagVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "使用次数")
    private Long useCount;
}
