package com.personblog.question.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.question.entity.Answer;

/**
 * <p>
 * 回答表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface IAnswerService extends IService<Answer> {

    /**
     * 管理端 - 删除回答
     */
    void deleteAnswerByAdmin(Long id);
}
