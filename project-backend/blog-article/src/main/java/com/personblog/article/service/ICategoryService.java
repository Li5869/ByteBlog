package com.personblog.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.article.entity.Category;
import com.personblog.article.vo.Admin.AdminCategoryVO;
import com.personblog.article.vo.Category.CategoryVO;

import java.util.List;

/**
 * <p>
 * 分类表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface ICategoryService extends IService<Category> {

    List<CategoryVO> getCategory();
    void updateCategoryCount(Long id, int delt);
    boolean saveCategory(Category category);
    void removeById(Long id);

    // ==================== 管理端接口 ====================

    /**
     * 管理端获取分类列表
     * @return 分类列表
     */
    List<AdminCategoryVO> getAdminCategoryList();

    /**
     * 管理端创建分类
     * @param category 分类信息
     * @return 创建结果
     */
    AdminCategoryVO createCategory(Category category);

    /**
     * 管理端更新分类
     * @param category 分类信息
     */
    void updateCategoryByAdmin(Category category);

    /**
     * 管理端删除分类
     * @param id 分类ID
     */
    void deleteCategoryByAdmin(Long id);
}
