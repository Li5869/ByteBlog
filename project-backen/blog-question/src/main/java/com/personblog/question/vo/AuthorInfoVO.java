package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作者信息 VO
 *
 * @author LSH
 */
@Data
@Schema(description = "作者信息")
public class AuthorInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户昵称")
    private String name;

    @Schema(description = "用户头像URL")
    private String avatar;
}
