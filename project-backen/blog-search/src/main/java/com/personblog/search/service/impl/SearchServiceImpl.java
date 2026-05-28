package com.personblog.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.personblog.common.exception.BizException;
import com.personblog.search.convert.SearchConverter;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.personblog.common.constant.TargetTypeConstant.*;
import static com.personblog.common.enums.BizCodeEnum.ERROR_SEARCH;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchServiceImpl implements SearchService{
    private final ElasticsearchOperations esOperation;
    private final SearchConverter searchConverter;
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
                    .map(searchConverter::convertToArticleVO)
                    .toList();
            resultDTO.setArticles(list);
            resultDTO.setArticleTotal(search.getTotalHits());
        }
        catch (Exception e){
            log.error("搜索文章失败, keyword={}, categoryId={}", queryDTO.getKeyword(), queryDTO.getCategoryId(), e);
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
                    .map(searchConverter::convertToQuestionVO)
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
                    .map(searchConverter::convertToAuthorVO)
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

    // ==================== 专栏搜索 ====================

    private void searchColumns(SearchQueryDTO queryDTO, SearchResultDTO result) {
        try {
            NativeQuery nativeQuery = buildColumnQuery(queryDTO);
            SearchHits<ColumnDocument> searchHits = esOperation.search(nativeQuery, ColumnDocument.class);

            List<ColumnSearchVO> columns = searchHits.getSearchHits().stream()
                    .map(searchConverter::convertToColumnVO)
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
}
