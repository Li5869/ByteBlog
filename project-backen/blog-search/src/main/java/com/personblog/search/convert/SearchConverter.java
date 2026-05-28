package com.personblog.search.convert;

import com.personblog.common.dto.Search.ArticleSearchDTO;
import com.personblog.common.dto.Search.AuthorSearchDTO;
import com.personblog.common.dto.Search.ColumnSearchDTO;
import com.personblog.common.dto.Search.QuestionSearchDTO;
import com.personblog.search.entity.ArticleDocument;
import com.personblog.search.entity.AuthorDocument;
import com.personblog.search.entity.ColumnDocument;
import com.personblog.search.entity.QuestionDocument;
import com.personblog.search.vo.ArticleSearchVO;
import com.personblog.search.vo.AuthorSearchVO;
import com.personblog.search.vo.ColumnSearchVO;
import com.personblog.search.vo.QuestionSearchVO;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchConverter {

    // ==================== Document -> VO 转换 ====================

    public ArticleSearchVO convertToArticleVO(SearchHit<ArticleDocument> hit) {
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

    public AuthorSearchVO convertToAuthorVO(SearchHit<AuthorDocument> hit) {
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

    public QuestionSearchVO convertToQuestionVO(SearchHit<QuestionDocument> hit) {
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

    public ColumnSearchVO convertToColumnVO(SearchHit<ColumnDocument> hit) {
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

    // ==================== DTO -> Document 转换 ====================

    public ArticleDocument convertToArticleDocument(ArticleSearchDTO dto) {
        ArticleDocument doc = new ArticleDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setSummary(dto.getSummary());
        doc.setCover(dto.getCover());
        doc.setAuthorId(dto.getAuthorId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setCategoryId(dto.getCategoryId());
        doc.setCategoryName(dto.getCategoryName());
        doc.setTags(dto.getTags());
        doc.setViews(dto.getViews());
        doc.setLikes(dto.getLikes());
        doc.setComments(dto.getComments());
        doc.setCollections(dto.getCollections());
        doc.setIsTop(dto.getIsTop());
        doc.setIsHot(dto.getIsHot());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        long weight = (dto.getViews() != null ? dto.getViews() : 0L)
                + (dto.getLikes() != null ? dto.getLikes() : 0L);
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(), weight));
        return doc;
    }

    public QuestionDocument convertToQuestionDocument(QuestionSearchDTO dto) {
        QuestionDocument doc = new QuestionDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent());
        doc.setAuthorId(dto.getAuthorId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setTags(dto.getTags());
        doc.setViews(dto.getViews());
        doc.setAnswers(dto.getAnswers());
        doc.setLikes(dto.getLikes());
        doc.setIsSolved(dto.getIsSolved());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        long weight = (dto.getViews() != null ? dto.getViews() : 0L)
                + (dto.getLikes() != null ? dto.getLikes() : 0L);
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(), weight));
        return doc;
    }

    public AuthorDocument convertToAuthorDocument(AuthorSearchDTO dto) {
        AuthorDocument doc = new AuthorDocument();
        doc.setId(dto.getId());
        doc.setUsername(dto.getUsername());
        doc.setNickname(dto.getNickname());
        doc.setAvatar(dto.getAvatar());
        doc.setBio(dto.getBio());
        doc.setArticlesCount(dto.getArticlesCount());
        doc.setFansCount(dto.getFansCount());
        doc.setLikesCount(dto.getLikesCount());
        doc.setStatus(dto.getStatus());
        doc.setNicknameSuggest(buildNicknameCompletion(dto.getNickname()));
        return doc;
    }

    public ColumnDocument convertToColumnDocument(ColumnSearchDTO dto) {
        ColumnDocument doc = new ColumnDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setDescription(dto.getDescription());
        doc.setCover(dto.getCover());
        doc.setUserId(dto.getUserId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setArticlesCount(dto.getArticlesCount());
        doc.setSubscriptionCount(dto.getSubscriptionCount());
        doc.setViews(dto.getViews());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(), dto.getSubscriptionCount() != null ? dto.getSubscriptionCount() : 0));
        return doc;
    }

    // ==================== 辅助方法 ====================

    private Completion buildTitleCompletion(String title, long weight) {
        List<String> inputs = new ArrayList<>();
        inputs.add(title);
        if (title != null && title.contains(" ")) {
            String[] parts = title.split("\\s+");
            for (String part : parts) {
                if (part.length() >= 2) {
                    inputs.add(part);
                }
            }
        }
        Completion completion = new Completion(inputs.toArray(new String[0]));
        completion.setWeight((int) Math.min(weight, Integer.MAX_VALUE));
        return completion;
    }

    private Completion buildNicknameCompletion(String nickname) {
        Completion completion = new Completion(new String[]{nickname});
        completion.setWeight((int) Math.min((long) 1, Integer.MAX_VALUE));
        return completion;
    }
}