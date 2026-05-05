package com.personblog.security.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.security.dto.AdminUserQueryDTO;
import com.personblog.security.dto.UpdateProfileDTO;
import com.personblog.security.entity.User;
import com.personblog.security.vo.*;

import java.util.List;

public interface IUserService extends IService<User> {

    UserProfileStatsVO getUserProfileStats(Long userId);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    /**
     * 获取活跃博主列表
     * 按文章数量排序，用于侧边栏展示
     * @param size 返回数量
     * @return 活跃博主列表
     */
    List<ActiveUserVO> getActiveUsers(Integer size);

    /**
     * 获取作者信息
     * 用于文章详情页展示作者信息
     * @param userId 作者ID
     * @return 作者信息
     */
    AuthorInfoVO getAuthorInfo(Long userId);

    // ==================== 管理端接口 ====================

    /**
     * 管理端 - 分页查询用户列表
     */
    Page<AdminUserVO> getAdminUserPage(AdminUserQueryDTO dto);

    /**
     * 管理端 - 获取用户详情
     */
    AdminUserDetailVO getAdminUserDetail(Long id);

    /**
     * 管理端 - 更新用户信息
     */
    void updateUserByAdmin(Long id, User user);

    /**
     * 管理端 - 封禁/解封用户
     */
    void updateUserStatusByAdmin(Long id, Short status);

    /**
     * 管理端 - 删除用户
     */
    void deleteUserByAdmin(Long id);
}
