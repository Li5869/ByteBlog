package com.personblog.common.dto.Tag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体 - 用于API接口参数
 *
 * @author LSH
 */
@Data
public class TagDTO {

    private Long id;

    private String name;

    private Long useCount;

    private LocalDateTime createdAt;
}
