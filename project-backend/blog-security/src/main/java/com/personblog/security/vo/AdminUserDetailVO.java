package com.personblog.security.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 管理端用户详情VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户详情信息")
public class AdminUserDetailVO extends AdminUserVO {

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "是否管理员")
    private Boolean isAdmin;
}
