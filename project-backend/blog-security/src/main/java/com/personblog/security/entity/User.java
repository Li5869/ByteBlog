package com.personblog.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_user")
public class User {

    /** 用户ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码(加密存储) */
    private String password;

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

    /** 文章数量 */
    private Long articlesCount;

    /** 粉丝数量 */
    private Long fansCount;

    /** 关注数量 */
    private Long followingCount;

    /** 访问量 */
    private Long viewsCount;

    /** 获赞数量 */
    private Long likesCount;

    /** 收藏数量 */
    private Long collectionsCount;

    /** 评论数量 */
    private Long commentsCount;

    /** 状态: 0-封禁, 1-正常 */
    private Short status;

    /** 是否管理员 */
    private Boolean isAdmin;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
