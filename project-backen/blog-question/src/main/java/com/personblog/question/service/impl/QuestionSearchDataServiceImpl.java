package com.personblog.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.searchAPI.QuestionSearchDataApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Search.QuestionSearchDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.entity.Tag;
import com.personblog.common.mapper.TagMapper;
import com.personblog.question.entity.Question;
import com.personblog.question.entity.QuestionTag;
import com.personblog.question.mapper.QuestionMapper;
import com.personblog.question.service.IQuestionTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 问题搜索数据服务 —— 实现QuestionSearchDataApi，为blog-search提供全量问题数据
 */
@Service
@RequiredArgsConstructor
public class QuestionSearchDataServiceImpl implements QuestionSearchDataApi {

    private final QuestionMapper questionMapper;
    private final IQuestionTagService questionTagService;
    private final TagMapper tagMapper;
    private final UseApi useApi;

    @Override
    public List<QuestionSearchDTO> listAllQuestionsForSearch() {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getIsDeleted, false)
        );
        return convertToQuestionSearchDTOList(questions);
    }

    @Override
    public QuestionSearchDTO getQuestionForSearch(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getIsDeleted()) {
            return null;
        }
        List<QuestionSearchDTO> list = convertToQuestionSearchDTOList(Collections.singletonList(question));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 批量转换Question为QuestionSearchDTO
     */
    private List<QuestionSearchDTO> convertToQuestionSearchDTOList(List<Question> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        // 批量查询问题-标签关联
        List<QuestionTag> questionTags = questionTagService.lambdaQuery()
                .in(QuestionTag::getQuestionId, questionIds)
                .list();
        Map<Long, List<Long>> questionTagMap = questionTags.stream()
                .collect(Collectors.groupingBy(QuestionTag::getQuestionId,
                        Collectors.mapping(QuestionTag::getTagId, Collectors.toList())));

        // 批量查询标签名称
        List<Long> tagIds = questionTags.stream().map(QuestionTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagIds.isEmpty() ? Collections.emptyMap() :
                tagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Tag::getName));

        // 通过UseApi跨模块查询作者信息
        List<Long> authorIds = questions.stream().map(Question::getAuthorId).distinct().toList();
        Map<Long, UserDTO> userMap = useApi.getUserInfo(authorIds).stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));

        // 组装DTO
        return questions.stream().map(question -> {
            UserDTO author = userMap.get(question.getAuthorId());
            List<String> tagNames = questionTagMap.getOrDefault(question.getId(), Collections.emptyList()).stream()
                    .map(tagNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            return QuestionSearchDTO.builder()
                    .id(question.getId())
                    .title(question.getTitle())
                    .content(question.getContent())
                    .authorId(question.getAuthorId())
                    .authorName(author != null ? author.getNickname() : null)
                    .authorAvatar(author != null ? author.getAvatar() : null)
                    .tags(tagNames)
                    .views(question.getViews())
                    .answers(question.getAnswers())
                    .likes(question.getLikes())
                    .isSolved(question.getIsSolved())
                    .status(1) // 非删除即为正常
                    .createdAt(question.getCreatedAt())
                    .updatedAt(question.getUpdatedAt())
                    .build();
        }).toList();
    }
}
