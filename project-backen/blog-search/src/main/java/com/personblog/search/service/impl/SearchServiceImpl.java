package com.personblog.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.personblog.common.exception.BizException;
import com.personblog.search.dto.SearchQueryDTO;
import com.personblog.search.dto.SearchResultDTO;
import com.personblog.search.entity.ArticleDocument;
import com.personblog.search.entity.AuthorDocument;
import com.personblog.search.entity.ColumnDocument;
import com.personblog.search.entity.QuestionDocument;
import com.personblog.search.service.SearchService;
import com.personblog.search.vo.ArticleSearchVO;
import com.personblog.search.vo.AuthorSearchVO;
import com.personblog.search.vo.ColumnSearchVO;
import com.personblog.search.vo.QuestionSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.personblog.common.constant.TargetTypeConstant.*;
import static com.personblog.common.enums.BizCodeEnum.ERROR_SEARCH;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchServiceImpl implements SearchService{
    private final ElasticsearchOperations esOperation;
    @Override
    public SearchResultDTO search(SearchQueryDTO queryDTO) {
        SearchResultDTO resultDTO = new SearchResultDTO();
        resultDTO.setArticles(new ArrayList<>());
        resultDTO.setAuthors(new ArrayList<>());
        resultDTO.setQuestions(new ArrayList<>());
        resultDTO.setColumns(new ArrayList<>());
        resultDTO.setArticleTotal(0L);
        resultDTO.setAuthorTotal(0L);
        resultDTO.setQuestionTotal(0L);
        resultDTO.setColumnTotal(0L);
        String type = queryDTO.getType();
        if (ARTICLE.equals(type) || ALL.equals(type)) {
            searchArticles(queryDTO, resultDTO);
        }
        if (QUESTION.equals(type) || ALL.equals(type)) {
            searchQuestions(queryDTO, resultDTO);
        }
        if (AUTHOR.equals(type) || ALL.equals(type)) {
            searchAuthors(queryDTO, resultDTO);
        }
        if (COLUMN.equals(type) || ALL.equals(type)) {
            searchColumns(queryDTO, resultDTO);
        }
        return resultDTO;
    }
    private void searchArticles(SearchQueryDTO queryDTO, SearchResultDTO resultDTO) {
        try {
            NativeQuery nativeQuery = buildArticleQuery(queryDTO);
            SearchHits<ArticleDocument> search = esOperation.search(nativeQuery, ArticleDocument.class);
            List<ArticleSearchVO> list = search.getSearchHits()
                    .stream()
                    .map(this::ConvertToArticleVO)
                    .toList();
            resultDTO.setArticles(list);
            resultDTO.setArticleTotal(search.getTotalHits());
        }
        catch (Exception e){
            throw new BizException(ERROR_SEARCH);
        }
    }
    private NativeQuery buildArticleQuery(SearchQueryDTO queryDTO) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        if(queryDTO.getKeyword()!=null&& StrUtil.isNotBlank(queryDTO.getKeyword())){
            boolQuery.must(m->
                    m.multiMatch(mm->mm
                            .fields("title^2","summary")
                            .query(queryDTO.getKeyword())
                    )
            );
        }
            //判断是否分类过滤
            if(queryDTO.getCategoryId()!=null){
                boolQuery.filter(f->f.term(t->t.field("categoryId").value(queryDTO.getCategoryId())));
            }
            if(queryDTO.getAuthorId()!=null){
                boolQuery.filter(f->f.term(t->t.field("authorId").value(queryDTO.getAuthorId())));
            }
            boolQuery.filter(f->f.term(t->t.field("status").value(1)));
            NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolQuery.build()))
                    .withPageable(PageRequest.of(queryDTO.getCurrent() - 1, queryDTO.getSize()));
            String orderBy = queryDTO.getOrderBy();
            if("time".equals(orderBy)){
                nativeQueryBuilder.withSort(Sort.by(Sort.Direction.DESC,"createdAt"));
            }
            else if("views".equals(orderBy)){
                nativeQueryBuilder.withSort(Sort.by(Sort.Direction.DESC,"views"));
            }
            else  if ("relevance".equals(orderBy) && queryDTO.getKeyword() != null) {
                nativeQueryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        return nativeQueryBuilder.build();
    }
    private void searchQuestions(SearchQueryDTO queryDTO, SearchResultDTO result) {
        try {
            NativeQuery nativeQuery = buildQuestionQuery(queryDTO);
            SearchHits<QuestionDocument> searchHits = esOperation.search(nativeQuery, QuestionDocument.class);

            List<QuestionSearchVO> questions = searchHits.getSearchHits().stream()
                    .map(this::convertToQuestionVO)
                    .collect(Collectors.toList());

            result.setQuestions(questions);
            result.setQuestionTotal(searchHits.getTotalHits());
        } catch (Exception e) {
           throw new BizException(ERROR_SEARCH);
        }
    }



    private NativeQuery buildQuestionQuery(SearchQueryDTO queryDTO) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            boolQuery.must(m -> m
                    .multiMatch(mm -> mm
                            .fields("title^2", "content")
                            .query(queryDTO.getKeyword())
                    )
            );
        }

        if (queryDTO.getAuthorId() != null) {
            boolQuery.filter(f -> f.term(t -> t.field("authorId").value(queryDTO.getAuthorId())));
        }

        boolQuery.filter(f -> f.term(t -> t.field("status").value(1)));

        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(queryDTO.getCurrent() - 1, queryDTO.getSize()));

        String orderBy = queryDTO.getOrderBy();
        if ("time".equals(orderBy)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if ("views".equals(orderBy)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "views"));
        } else if ("relevance".equals(orderBy) && queryDTO.getKeyword() != null) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return queryBuilder.build();
    }
    private void searchAuthors(SearchQueryDTO queryDTO, SearchResultDTO result) {
        try {
            NativeQuery nativeQuery = buildAuthorQuery(queryDTO);
            SearchHits<AuthorDocument> searchHits = esOperation.search(nativeQuery, AuthorDocument.class);

            List<AuthorSearchVO> authors = searchHits.getSearchHits().stream()
                    .map(this::convertToAuthorVO)
                    .collect(Collectors.toList());

            result.setAuthors(authors);
            result.setAuthorTotal(searchHits.getTotalHits());
        } catch (Exception e) {
            throw new BizException(ERROR_SEARCH);
        }
    }



    private NativeQuery buildAuthorQuery(SearchQueryDTO queryDTO) {
        var boolQuery = new BoolQuery.Builder();

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            boolQuery.must(m -> m
                    .match(mt -> mt
                            .field("nickname")
                            .query(queryDTO.getKeyword())
                    )
            );
        }

        boolQuery.filter(f -> f.term(t -> t.field("status").value(1)));

        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(queryDTO.getCurrent() - 1, queryDTO.getSize()));

        queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "articlesCount"));

        return queryBuilder.build();
    }
    private AuthorSearchVO convertToAuthorVO(SearchHit<AuthorDocument> hit) {
        AuthorDocument doc = hit.getContent();
        AuthorSearchVO vo = new AuthorSearchVO();
        vo.setId(doc.getId());
        vo.setUsername(doc.getUsername());
        vo.setNickname(doc.getNickname());
        vo.setAvatar(doc.getAvatar());
        vo.setBio(doc.getBio());
        vo.setArticlesCount(doc.getArticlesCount());
        vo.setFansCount(doc.getFansCount());
        vo.setLikesCount(doc.getLikesCount());
        return vo;
    }
    private QuestionSearchVO convertToQuestionVO(SearchHit<QuestionDocument> hit) {
        QuestionDocument doc = hit.getContent();
        QuestionSearchVO vo = new QuestionSearchVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setContent(doc.getContent());
        vo.setAuthorId(doc.getAuthorId());
        vo.setAuthorName(doc.getAuthorName());
        vo.setAuthorAvatar(doc.getAuthorAvatar());
        vo.setTags(doc.getTags());
        vo.setViews(doc.getViews());
        vo.setAnswers(doc.getAnswers());
        vo.setLikes(doc.getLikes());
        vo.setIsSolved(doc.getIsSolved());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }
    //构建VO对象
    private ArticleSearchVO ConvertToArticleVO(SearchHit<ArticleDocument> hit) {
        ArticleDocument doc = hit.getContent();
        ArticleSearchVO vo = new ArticleSearchVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setSummary(doc.getSummary());
        vo.setCover(doc.getCover());
        vo.setAuthorId(doc.getAuthorId());
        vo.setAuthorName(doc.getAuthorName());
        vo.setAuthorAvatar(doc.getAuthorAvatar());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCategoryName(doc.getCategoryName());
        vo.setTags(doc.getTags());
        vo.setViews(doc.getViews());
        vo.setLikes(doc.getLikes());
        vo.setComments(doc.getComments());
        vo.setCollections(doc.getCollections());
        vo.setIsTop(doc.getIsTop());
        vo.setIsHot(doc.getIsHot());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }

    // ==================== 专栏搜索 ====================

    private void searchColumns(SearchQueryDTO queryDTO, SearchResultDTO result) {
        try {
            NativeQuery nativeQuery = buildColumnQuery(queryDTO);
            SearchHits<ColumnDocument> searchHits = esOperation.search(nativeQuery, ColumnDocument.class);

            List<ColumnSearchVO> columns = searchHits.getSearchHits().stream()
                    .map(this::convertToColumnVO)
                    .collect(Collectors.toList());

            result.setColumns(columns);
            result.setColumnTotal(searchHits.getTotalHits());
        } catch (Exception e) {
            throw new BizException(ERROR_SEARCH);
        }
    }

    private NativeQuery buildColumnQuery(SearchQueryDTO queryDTO) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            boolQuery.must(m -> m
                    .multiMatch(mm -> mm
                            .fields("title^2", "description")
                            .query(queryDTO.getKeyword())
                    )
            );
        }

        if (queryDTO.getAuthorId() != null) {
            boolQuery.filter(f -> f.term(t -> t.field("userId").value(queryDTO.getAuthorId())));
        }

        boolQuery.filter(f -> f.term(t -> t.field("status").value(1)));

        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(queryDTO.getCurrent() - 1, queryDTO.getSize()));

        String orderBy = queryDTO.getOrderBy();
        if ("time".equals(orderBy)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if ("views".equals(orderBy)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "views"));
        } else if ("relevance".equals(orderBy) && queryDTO.getKeyword() != null) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return queryBuilder.build();
    }

    private ColumnSearchVO convertToColumnVO(SearchHit<ColumnDocument> hit) {
        ColumnDocument doc = hit.getContent();
        ColumnSearchVO vo = new ColumnSearchVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setDescription(doc.getDescription());
        vo.setCover(doc.getCover());
        vo.setUserId(doc.getUserId());
        vo.setAuthorName(doc.getAuthorName());
        vo.setAuthorAvatar(doc.getAuthorAvatar());
        vo.setArticlesCount(doc.getArticlesCount());
        vo.setSubscriptionCount(doc.getSubscriptionCount());
        vo.setViews(doc.getViews());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }
}
