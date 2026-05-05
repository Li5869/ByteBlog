package com.personblog.common.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 标签VO
 *
 * @author LSH
 */
@Data
@Builder
public class TagVO {

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    private String name;

    /** 使用次数 */
    private Long useCount;
}
