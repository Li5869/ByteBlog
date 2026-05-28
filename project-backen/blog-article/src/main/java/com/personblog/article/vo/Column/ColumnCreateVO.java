package com.personblog.article.vo.Column;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 创建/更新专栏返回对象
 *
 * @author LSH
 */
@Data
@Builder
@Schema(description = "创建/更新专栏返回对象")
public class ColumnCreateVO {

    @Schema(description = "专栏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "专栏标题")
    private String title;

    @Schema(description = "状态：0-草稿，1-已发布")
    private Integer status;
}
