package com.personblog.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.question.dto.QuestionQueryDTO;
import com.personblog.question.entity.Question;
import com.personblog.question.vo.QuestionDetailVO;
import com.personblog.question.vo.QuestionListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 问题表 Mapper 接口
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 分页查询问题列表（含作者信息）
     *
     * @param dto     查询参数
     * @param offset  偏移量
     * @return 问题列表VO
     */
    List<QuestionListVO> selectQuestionPageWithInfo(@Param("dto") QuestionQueryDTO dto, @Param("offset") int offset);

    /**
     * 按条件统计问题总数
     *
     * @param dto 查询参数
     * @return 总数
     */
    Long countQuestionsWithCondition(@Param("dto") QuestionQueryDTO dto);

    /**
     * 查询问题详情（含作者信息），并自增浏览量
     *
     * @param id 问题ID
     * @return 问题详情VO
     */
    QuestionDetailVO selectQuestionDetailById(@Param("id") Long id);
}
