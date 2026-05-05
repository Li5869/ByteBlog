package com.personblog.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.mapper.ArticleTagMapper;
import com.personblog.article.service.IArticleTagService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章标签关联表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements IArticleTagService {

}
