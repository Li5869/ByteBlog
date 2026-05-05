package com.personblog.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.api.CategoryAPI.CategoryCacheApi;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.Category;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.mapper.CategoryMapper;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.AdminCategoryVO;
import com.personblog.article.vo.CategoryVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.personblog.common.constant.RedisKeys.CATEGORY_ALL;
import static com.personblog.common.enums.BizCodeEnum.CATEGORY_HAS_ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.CATEGORY_REPEAT;

/**
 * <p>
 * 分类表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService, CategoryCacheApi {


    private final ArticleMapper articleMapper;

    private Cache<String, List<CategoryVO>> categoryCache;

    @PostConstruct
    public void init() {
        categoryCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();
    }

    @Override
    public List<CategoryVO> getCategory() {
        List<CategoryVO> all = categoryCache.getIfPresent(CATEGORY_ALL);
        if (all != null) {
            return all;
        }
        List<Category> categoryList = list();
        List<Category> vos = categoryList.stream().sorted(Comparator.comparing(Category::getSort)).toList();
        List<CategoryVO> categoryVOS = BeanUtil.copyToList(vos, CategoryVO.class);
        categoryCache.put(CATEGORY_ALL, categoryVOS);
        return categoryVOS;
    }

    @Override
    public void updateCategoryCount(Long id, int dealt) {
        categoryCache.invalidate(CATEGORY_ALL);
        lambdaUpdate()
                .eq(Category::getId, id)
                .setSql("articles_count=articles_count+" + dealt)
                .update();
    }

    /**
     * 创建分类 - 清除缓存
     */
    public boolean saveCategory(Category category) {
        boolean result = super.save(category);
        if (result) {
            categoryCache.invalidate(CATEGORY_ALL);
        }
        return result;
    }

    /**
     * 清除缓存
     */
    public void removeById(Long id) {
        categoryCache.invalidate(CATEGORY_ALL);
    }

    // ==================== 管理端接口实现 ====================

    @Override
    public List<AdminCategoryVO> getAdminCategoryList() {
        List<Category> categories = this.lambdaQuery()
                .orderByAsc(Category::getSort)
                .list();

        return categories.stream()
                .map(category -> AdminCategoryVO.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .sort(category.getSort())
                        .articleCount(category.getArticlesCount() != null ? category.getArticlesCount() : 0L)
                        .createdAt(category.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCategoryVO createCategory(Category category) {
        // 检查分类名称是否重复
        boolean exists = this.lambdaQuery()
                .eq(Category::getName, category.getName())
                .exists();
        if (exists) {
            throw new BizException(CATEGORY_REPEAT);
        }

        category.setArticlesCount(0L);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        category.setCreatedAt(LocalDateTime.now());

        this.save(category);
        categoryCache.invalidate(CATEGORY_ALL);

        return AdminCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sort(category.getSort())
                .articleCount(0L)
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryByAdmin(Category category) {
        Category existing = this.getById(category.getId());
        if (existing == null) {
            throw new BizException(BizCodeEnum.CATEGORY_NOT_EXIST);
        }

        // 如果修改了名称，检查是否重复
        if (!existing.getName().equals(category.getName())) {
            boolean exists = this.lambdaQuery()
                    .eq(Category::getName, category.getName())
                    .ne(Category::getId, category.getId())
                    .exists();
            if (exists) {
                throw new BizException(CATEGORY_REPEAT);
            }
        }

        this.updateById(category);
        categoryCache.invalidate(CATEGORY_ALL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryByAdmin(Long id) {
        Category category = this.getById(id);
        if (category == null) {
            throw new BizException(BizCodeEnum.CATEGORY_NOT_EXIST);
        }

        // 检查分类下是否有文章（直接使用 ArticleMapper 查询，避免循环依赖）
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategoryId, id)
                        .eq(Article::getIsDeleted, false)
        );
        if (articleCount > 0) {
            throw new BizException(CATEGORY_HAS_ARTICLE);
        }

        this.removeById(id);
        categoryCache.invalidate(CATEGORY_ALL);
    }
}
