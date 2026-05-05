package com.personblog.question.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.question.entity.QuestionTag;
import com.personblog.question.mapper.QuestionTagMapper;
import com.personblog.question.service.IQuestionTagService;
import org.springframework.stereotype.Service;

/**
 * 问题-标签关联表 服务实现类
 *
 * @author LSH
 */
@Service
public class QuestionTagServiceImpl extends ServiceImpl<QuestionTagMapper, QuestionTag> implements IQuestionTagService {

}
