package com.personblog.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.vo.TagVO;
import com.personblog.api.searchAPI.ArticleSearchDataApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.mapper.ArticleTagMapper;
import com.personblog.article.service.ICategoryService;
import com.personblog.common.dto.Search.ArticleSearchDTO;
import com.personblog.common.dto.User.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文章搜索数据服务 —— 实现ArticleSearchDataApi，为blog-search提供全量文章数据
 */
@Service
@RequiredArgsConstructor
public class ArticleSearchDataServiceImpl implements ArticleSearchDataApi {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ICategoryService categoryService;
    private final TagApi tagApi;
    private final UseApi useApi;

    @Override
    public List<ArticleSearchDTO> listAllArticlesForSearch() {
        // 只查询已发布且未删除的文章
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, 1)
                        .eq(Article::getIsDeleted, false)
        );
        return convertToArticleSearchDTOList(articles);
    }

    @Override
    public ArticleSearchDTO getArticleForSearch(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getIsDeleted() || article.getStatus() != 1) {
            return null;
        }
        List<ArticleSearchDTO> list = convertToArticleSearchDTOList(Collections.singletonList(article));
        return list.isEmpty() ? null : list.getFirst();
    }

    /**
     * 批量转换Article为ArticleSearchDTO
     */
    private List<ArticleSearchDTO> convertToArticleSearchDTOList(List<Article> articles) {
        if (articles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = articles.stream().map(Article::getId).toList();

        // 批量查询分类名称
        List<Long> categoryIds = articles.stream().map(Article::getCategoryId).distinct().toList();
        Map<Long, String> categoryNameMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 批量查询文章-标签关联
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Map<Long, List<Long>> articleTagMap = articleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())));

        // 批量查询标签名称
        List<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagIds.isEmpty() ? Collections.emptyMap() :
                tagApi.getTagsByIds(tagIds).stream()
                        .collect(Collectors.toMap(TagVO::getId, TagVO::getName));

        // 通过UseApi跨模块查询作者信息
        List<Long> authorIds = articles.stream().map(Article::getAuthorId).distinct().toList();
        Map<Long, UserDTO> userMap = useApi.getUserInfo(authorIds).stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));

        // 组装DTO
        return articles.stream().map(article -> {
            UserDTO author = userMap.get(article.getAuthorId());
            List<String> tagNames = articleTagMap.getOrDefault(article.getId(), Collections.emptyList()).stream()
                    .map(tagNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            return ArticleSearchDTO.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .summary(article.getSummary())
                    .cover(article.getCover())
                    .authorId(article.getAuthorId())
                    .authorName(author != null ? author.getNickname() : null)
                    .authorAvatar(author != null ? author.getAvatar() : null)
                    .categoryId(article.getCategoryId())
                    .categoryName(categoryNameMap.get(article.getCategoryId()))
                    .tags(tagNames)
                    .views(article.getViews())
                    .likes(article.getLikes())
                    .comments(article.getComments())
                    .collections(article.getCollections())
                    .isTop(article.getIsTop())
                    .isHot(article.getIsHot())
                    .status(article.getStatus())
                    .createdAt(article.getCreatedAt())
                    .updatedAt(article.getUpdatedAt())
                    .build();
        }).toList();
    }
}
