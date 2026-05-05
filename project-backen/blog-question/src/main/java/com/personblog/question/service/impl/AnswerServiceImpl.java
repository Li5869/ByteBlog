package com.personblog.question.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.questionAPI.AnswerApi;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.question.entity.Answer;
import com.personblog.question.mapper.AnswerMapper;
import com.personblog.question.service.IAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 回答表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@Slf4j
public class AnswerServiceImpl extends ServiceImpl<AnswerMapper, Answer> implements IAnswerService, AnswerApi {

    @Override
    public void updateLikeCount(List<LikeMessageDTO> likeMessageDTOS) {

    }

    @Override
    public Long getAnswerAuthorId(Long answerId) {
        Answer answer = getById(answerId);
        return answer != null ? answer.getAuthorId() : null;
    }

    // ==================== 管理端接口实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnswerByAdmin(Long id) {
        Answer answer = getById(id);
        if (answer == null || Boolean.TRUE.equals(answer.getIsDeleted())) {
            throw new BizException(BizCodeEnum.ANSWER_NOT_EXIST);
        }
        this.removeById(id);
        log.info("管理员删除回答: answerId={}", id);
    }
}
