package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.KnowledgeFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文件 Mapper
 *
 * @author LSH
 */
@Mapper
public interface KnowledgeFileMapper extends BaseMapper<KnowledgeFile> {
}
