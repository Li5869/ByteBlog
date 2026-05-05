package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标签信息 VO
 *
 * @author LSH
 */
@Data
@Schema(description = "标签信息")
public class TagInfo {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;
}
