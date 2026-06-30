package com.personblog.ai.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * LangChain PGVector 向量表 Mapper
 * 表结构由 LangChain 自动管理，此处仅提供删除能力
 *
 * @author LSH
 */
@Mapper
public interface BlogKnowledgeMapper {

    /**
     * 根据 doc_id 删除向量记录
     * doc_id 存储在 langchain_metadata JSON 的 doc_id 字段中
     *
     * @param docId 父文档 ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM blog_knowledge WHERE langchain_metadata->>'doc_id' = #{docId}")
    int deleteByDocId(@Param("docId") String docId);
}
