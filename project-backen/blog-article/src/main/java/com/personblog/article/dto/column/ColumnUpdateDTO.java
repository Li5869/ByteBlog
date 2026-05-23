package com.personblog.article.dto.column;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新专栏参数
 *
 * @author LSH
 */
@Data
@Schema(description = "更新专栏参数")
public class ColumnUpdateDTO {

    @Schema(description = "专栏标题")
    @Size(max = 100, message = "专栏标题不能超过100字")
    private String title;

    @Schema(description = "专栏描述")
    @Size(max = 500, message = "专栏描述不能超过500字")
    private String description;

    @Schema(description = "专栏封面URL")
    private String cover;

    @Schema(description = "状态：0-草稿，1-发布")
    private Integer status;
}
