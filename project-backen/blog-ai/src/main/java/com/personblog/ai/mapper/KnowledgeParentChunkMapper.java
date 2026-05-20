package com.personblog.ai.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * Parent Chunk 文本表 Mapper
 * 存储 Parent 级别的文档切片原文，供检索时返回完整上下文
 *
 * @author LSH
 */
@Mapper
public interface KnowledgeParentChunkMapper {

    /**
     * 根据 ID 删除 Parent Chunk
     *
     * @param id Parent Chunk UUID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM knowledge_parent_chunks WHERE id = #{id}")
    int deleteById(@Param("id") UUID id);
}
