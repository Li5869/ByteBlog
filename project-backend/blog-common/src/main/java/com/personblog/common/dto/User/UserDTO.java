package com.personblog.common.dto.User;

import lombok.Data;

@Data
public class UserDTO {
    //用户id
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 性别: 0-未知, 1-男, 2-女 */
    private Short gender;
}
