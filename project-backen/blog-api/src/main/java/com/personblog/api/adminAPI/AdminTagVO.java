package com.personblog.api.adminAPI;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端标签VO - 用于API接口返回
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTagVO {

    private Long id;

    private String name;

    private Long usageCount;

    private LocalDateTime createdAt;
}
