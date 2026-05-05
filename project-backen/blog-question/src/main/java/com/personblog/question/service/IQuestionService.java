package com.personblog.question.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.question.dto.*;
import com.personblog.question.entity.Question;
import com.personblog.question.vo.*;

import java.util.List;

/**
 * 问题表 服务类接口
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface IQuestionService extends IService<Question> {

    /**
     * 分页查询问题列表
     *
     * @param queryDTO 查询参数
     * @return 分页问题列表
     */
    Page<QuestionListVO> getQuestionPage(QuestionQueryDTO queryDTO);

    /**
     * 获取问题详情（含作者信息、标签，并自增浏览量）
     *
     * @param id 问题ID
     * @return 问题详情
     */
    QuestionDetailVO getQuestionDetail(Long id);

    /**
     * 创建问题
     *
     * @param userId 当前用户ID
     * @param dto    创建参数
     * @return 创建结果VO
     */
    QuestionCreateVO createQuestion(Long userId, QuestionCreateDTO dto);

    /**
     * 删除问题（逻辑删除，仅限提问者本人或管理员）
     *
     * @param userId    当前用户ID
     * @param questionId 问题ID
     */
    void deleteQuestion(Long userId, Long questionId);

    /**
     * 获取回答列表
     *
     * @param questionId 问题ID
     * @param current    当前页码
     * @param size       每页数量
     * @param sortBy     排序方式：best-最佳优先，newest-最新，votes-投票数最高
     * @return 分页回答列表
     */
    Page<AnswerVO> getAnswerList(Long questionId, Integer current, Integer size, String sortBy);

    /**
     * 提交回答
     *
     * @param userId     当前用户ID
     * @param questionId 问题ID
     * @param content    回答内容
     * @return 创建结果VO
     */
    AnswerCreateVO createAnswer(Long userId, Long questionId, String content);

    /**
     * 删除回答（逻辑删除，仅限回答者本人或管理员）
     *
     * @param userId   当前用户ID
     * @param answerId 回答ID
     */
    void deleteAnswer(Long userId, Long answerId);

    /**
     * 获取热门问题排行
     *
     * @param limit 返回数量，最大20
     * @return 热门问题列表
     */
    List<HotQuestionVO> getHotQuestions(Integer limit);

    /**
     * 获取我的问题列表
     *
     * @param userId   当前用户ID
     * @param queryDTO 查询参数
     * @return 分页问题列表
     */
    Page<MyQuestionVO> getMyQuestions(Long userId, MyQuestionQueryDTO queryDTO);

    /**
     * 获取我的回答列表
     *
     * @param userId   当前用户ID
     * @param queryDTO 查询参数
     * @return 分页回答列表
     */
    Page<MyAnswerVO> getMyAnswers(Long userId, MyAnswerQueryDTO queryDTO);

    /**
     * 采纳最佳答案
     *
     * @param userId   当前用户ID（问题作者）
     * @param answerId 回答ID
     * @return 采纳结果VO
     */
    AcceptAnswerVO acceptBestAnswer(Long userId, Long answerId);

    // ==================== 管理端接口 ====================

    /**
     * 管理端 - 分页查询问题列表
     */
    Page<AdminQuestionVO> getAdminQuestionPage(AdminQuestionQueryDTO dto);

    /**
     * 管理端 - 获取问题详情（含回答列表）
     */
    AdminQuestionDetailVO getAdminQuestionDetail(Long id);

    /**
     * 管理端 - 删除问题
     */
    void deleteQuestionByAdmin(Long id);
}
