package com.personblog.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.ai.dto.KnowledgeFileQueryDTO;
import com.personblog.ai.dto.KnowledgeFileUpdateDTO;
import com.personblog.ai.entity.KnowledgeFile;
import com.personblog.ai.vo.KnowledgeFileDetailVO;
import com.personblog.ai.vo.KnowledgeFileListVO;

import java.util.List;

/**
 * 知识库文件 Service 接口
 *
 * @author LSH
 */
public interface IKnowledgeFileService extends IService<KnowledgeFile> {

    /**
     * 分页查询知识库文件列表
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    Page<KnowledgeFileListVO> getFilePage(KnowledgeFileQueryDTO dto);

    /**
     * 获取知识库文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    KnowledgeFileDetailVO getFileDetail(Long fileId);

    /**
     * 更新知识库文件信息
     *
     * @param fileId 文件ID
     * @param dto    更新参数
     */
    void updateFile(Long fileId, KnowledgeFileUpdateDTO dto);

    /**
     * 级联删除知识库文件
     * 删除 tb_knowledge_file、knowledge_parent_chunks、blog_knowledge 中的关联数据
     *
     * @param fileId 文件ID
     * @return 删除的记录数
     */
    int cascadeDeleteById(Long fileId);

    /**
     * 批量级联删除知识库文件
     *
     * @param fileIds 文件ID列表
     * @return 各表删除的记录数数组
     */
    int[] cascadeDeleteByIds(List<Long> fileIds);
}
