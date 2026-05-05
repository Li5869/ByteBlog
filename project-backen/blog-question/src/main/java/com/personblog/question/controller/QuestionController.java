package com.personblog.question.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.question.dto.*;
import com.personblog.question.service.IQuestionService;
import com.personblog.question.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答模块 Controller
 *
 * @author LSH
 */
@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
@Tag(name = "问答接口", description = "问答社区相关接口")
public class QuestionController {

    private final IQuestionService questionService;

    /**
     * 获取问题列表
     * 支持分页查询、状态筛选（全部/待回答/已解决/热门）、排序方式（最新/最热）、标签筛选、关键词搜索
     *
     * @param queryDTO 查询参数
     * @return 分页问题列表
     */
    @Operation(summary = "获取问题列表", description = "获取问题列表，支持分页、状态筛选、排序、标签筛选和关键词搜索")
    @GetMapping("/questions")
    public JsonData<Page<QuestionListVO>> getQuestionPage(QuestionQueryDTO queryDTO) {
        Page<QuestionListVO> page = questionService.getQuestionPage(queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取问题详情
     * 包含完整问题描述、作者信息、标签列表，调用时自动增加浏览量
     *
     * @param id 问题ID
     * @return 问题详情
     */
    @Operation(summary = "获取问题详情", description = "获取指定问题的完整详情，含作者信息、标签、统计数据，访问后自动+1浏览量")
    @GetMapping("/questions/{id}")
    public JsonData<QuestionDetailVO> getQuestionDetail(
            @Parameter(description = "问题ID")
            @PathVariable Long id) {
        QuestionDetailVO detail = questionService.getQuestionDetail(id);
        return JsonData.buildSuccess(detail);
    }

    /**
     * 发布问题
     * 用户登录后可发布技术问题到问答社区
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @Operation(summary = "发布问题", description = "用户发布新的技术问题到问答社区")
    @PostMapping("/questions")
    public JsonData<QuestionCreateVO> createQuestion(@RequestBody QuestionCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        QuestionCreateVO vo = questionService.createQuestion(userId, dto);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 删除问题
     * 仅限提问者本人或管理员操作，采用逻辑删除
     *
     * @param id 问题ID
     * @return 操作结果
     */
    @Operation(summary = "删除问题", description = "删除自己发布的问题（逻辑删除）")
    @DeleteMapping("/questions/{id}")
    public JsonData<Void> deleteQuestion(
            @Parameter(description = "问题ID")
            @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        questionService.deleteQuestion(userId, id);
        return JsonData.buildSuccess();
    }

    /**
     * 获取回答列表
     * 支持分页查询、三种排序方式（最佳优先/最新/投票数最高）
     *
     * @param questionId 问题ID
     * @param current    当前页码
     * @param size       每页数量
     * @param sortBy     排序方式：best-最佳优先，newest-最新，votes-投票数最高
     * @return 分页回答列表
     */
    @Operation(summary = "获取回答列表", description = "获取指定问题的所有回答列表，支持分页和排序")
    @GetMapping("/questions/{questionId}/answers")
    public JsonData<Page<AnswerVO>> getAnswerList(
            @Parameter(description = "问题ID")
            @PathVariable Long questionId,
            @Parameter(description = "当前页码")
            @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "排序方式：best/newest/votes")
            @RequestParam(defaultValue = "best") String sortBy) {
        Page<AnswerVO> page = questionService.getAnswerList(questionId, current, size, sortBy);
        return JsonData.buildSuccess(page);
    }

    /**
     * 提交回答
     * 用户登录后可对问题提交回答
     *
     * @param questionId 问题ID
     * @param dto       回答参数
     * @return 创建结果
     */
    @Operation(summary = "提交回答", description = "用户对某个问题提交回答")
    @PostMapping("/questions/{questionId}/answers")
    public JsonData<AnswerCreateVO> createAnswer(
            @Parameter(description = "问题ID")
            @PathVariable Long questionId,
            @RequestBody AnswerCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        AnswerCreateVO vo = questionService.createAnswer(userId, questionId, dto.getContent());
        return JsonData.buildSuccess(vo);
    }

    /**
     * 删除回答
     * 仅限回答者本人或管理员操作，采用逻辑删除
     *
     * @param id 回答ID
     * @return 操作结果
     */
    @Operation(summary = "删除回答", description = "删除自己发布的回答（逻辑删除）")
    @DeleteMapping("/answers/{id}")
    public JsonData<Void> deleteAnswer(
            @Parameter(description = "回答ID")
            @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        questionService.deleteAnswer(userId, id);
        return JsonData.buildSuccess();
    }

    /**
     * 获取热门问题排行
     * 用于侧边栏展示
     *
     * @param limit 返回数量，最大20
     * @return 热门问题列表
     */
    @Operation(summary = "获取热门问题排行", description = "获取问答社区的热门问题排行榜")
    @GetMapping("/questions/hot")
    public JsonData<List<HotQuestionVO>> getHotQuestions(
            @Parameter(description = "返回数量，最大20")
            @RequestParam(defaultValue = "10") Integer limit) {
        List<HotQuestionVO> list = questionService.getHotQuestions(limit);
        return JsonData.buildSuccess(list);
    }

    /**
     * 获取我的问题列表
     * 支持分页查询、状态筛选（全部/已解决/待解决）、排序方式（按时间/按回答数/按点赞数）
     *
     * @param queryDTO 查询参数
     * @return 分页问题列表
     */
    @Operation(summary = "获取我的问题列表", description = "获取当前用户提出的问题列表，支持分页、状态筛选和排序")
    @GetMapping("/my/questions")
    public JsonData<Page<MyQuestionVO>> getMyQuestions(MyQuestionQueryDTO queryDTO) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        Page<MyQuestionVO> page = questionService.getMyQuestions(userId, queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取我的回答列表
     * 支持分页查询、类型筛选（全部/最佳答案/普通回答）、排序方式（按时间/按点赞数）
     *
     * @param queryDTO 查询参数
     * @return 分页回答列表
     */
    @Operation(summary = "获取我的回答列表", description = "获取当前用户发布的回答列表，支持分页、类型筛选和排序")
    @GetMapping("/my/answers")
    public JsonData<Page<MyAnswerVO>> getMyAnswers(MyAnswerQueryDTO queryDTO) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        Page<MyAnswerVO> page = questionService.getMyAnswers(userId, queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 采纳最佳答案
     * 问题作者将某个回答采纳为最佳答案，标记该问题已解决
     *
     * @param id 回答ID
     * @return 采纳结果（包含问题ID、回答ID、是否已解决）
     */
    @Operation(summary = "采纳最佳答案", description = "问题作者将某个回答采纳为最佳答案，标记该问题已解决。如果已有最佳答案会自动替换")
    @PostMapping("/answers/{id}/accept")
    public JsonData<AcceptAnswerVO> acceptBestAnswer(
            @Parameter(description = "回答ID")
            @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        AcceptAnswerVO vo = questionService.acceptBestAnswer(userId, id);
        return JsonData.buildSuccess(vo);
    }

}
