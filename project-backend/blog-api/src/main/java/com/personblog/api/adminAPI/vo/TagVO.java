package com.personblog.api.adminAPI.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 标签VO - 用于API接口返回
 *
 * @author LSH
 */
@Data
@Builder
public class TagVO {

    private Long id;

    private String name;

    private Long useCount;
}
