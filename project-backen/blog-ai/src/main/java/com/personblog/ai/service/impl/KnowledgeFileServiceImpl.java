package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.ai.dto.KnowledgeFileQueryDTO;
import com.personblog.ai.dto.KnowledgeFileUpdateDTO;
import com.personblog.ai.entity.KnowledgeFile;
import com.personblog.ai.mapper.BlogKnowledgeMapper;
import com.personblog.ai.mapper.KnowledgeFileMapper;
import com.personblog.ai.mapper.KnowledgeParentChunkMapper;
import com.personblog.ai.service.IKnowledgeFileService;
import com.personblog.ai.vo.KnowledgeFileDetailVO;
import com.personblog.ai.vo.KnowledgeFileListVO;
import com.personblog.common.exception.BizException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库文件 Service 实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl extends ServiceImpl<KnowledgeFileMapper, KnowledgeFile>
        implements IKnowledgeFileService {

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final BlogKnowledgeMapper blogKnowledgeMapper;
    private final KnowledgeParentChunkMapper knowledgeParentChunkMapper;

    @Override
    public Page<KnowledgeFileListVO> getFilePage(KnowledgeFileQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        Page<KnowledgeFile> page = new Page<>(current, size);

        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(dto.getKeyword())) {
            wrapper.like(KnowledgeFile::getFileName, dto.getKeyword());
        }

        if (StringUtils.isNotBlank(dto.getSource())) {
            wrapper.eq(KnowledgeFile::getSource, dto.getSource());
        }

        if (StringUtils.isNotBlank(dto.getCategory())) {
            wrapper.eq(KnowledgeFile::getCategory, dto.getCategory());
        }

        wrapper.orderByDesc(KnowledgeFile::getCreatedAt);
        Page<KnowledgeFile> pages = knowledgeFileMapper.selectPage(page, wrapper);
        // 转换为 ListVO
        Page<KnowledgeFileListVO> resultPage = new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        List<KnowledgeFileListVO> records = pages.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public KnowledgeFileDetailVO getFileDetail(Long fileId) {
        KnowledgeFile file = knowledgeFileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException("文件不存在");
        }
        return convertToDetailVO(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFile(Long fileId, KnowledgeFileUpdateDTO dto) {
        KnowledgeFile file = knowledgeFileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException("文件不存在");
        }

        KnowledgeFile updateEntity = new KnowledgeFile();
        updateEntity.setId(fileId);

        if (StringUtils.isNotBlank(dto.getFileName())) {
            updateEntity.setFileName(dto.getFileName());
        }
        if (dto.getDescription() != null) {
            updateEntity.setDescription(dto.getDescription());
        }
        updateEntity.setUpdatedAt(LocalDateTime.now());

        knowledgeFileMapper.updateById(updateEntity);
        log.info("更新知识库文件信息: fileId={}", fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cascadeDeleteById(Long fileId) {
        KnowledgeFile file = knowledgeFileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException("文件不存在");
        }

        // 解析 parent_ids，逐个删除关联的向量和父切片
        List<String> parentIds = parseParentIds(file.getParentIds());
        int deletedChildCount = 0;
        int deletedParentCount = 0;

        for (String parentId : parentIds) {
            deletedChildCount += blogKnowledgeMapper.deleteByDocId(parentId);
            try {
                deletedParentCount += knowledgeParentChunkMapper.deleteById(UUID.fromString(parentId));
            } catch (IllegalArgumentException e) {
                log.warn("无效的 parent_id 格式: {}", parentId);
            }
        }

        // 删除文件记录本身
        knowledgeFileMapper.deleteById(fileId);

        log.info("级联删除知识库文件: fileId={}, fileName={}, 删除 Parent={}, Child={}",
                fileId, file.getFileName(), deletedParentCount, deletedChildCount);
        return deletedParentCount + deletedChildCount + 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int[] cascadeDeleteByIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BizException("文件ID列表不能为空");
        }

        // 批量查询所有文件记录
        List<KnowledgeFile> files = knowledgeFileMapper.selectBatchIds(fileIds);
        if (files.isEmpty()) {
            return new int[]{0, 0, 0};
        }

        int totalChildCount = 0;
        int totalParentCount = 0;

        // 逐个文件级联删除
        for (KnowledgeFile file : files) {
            List<String> parentIds = parseParentIds(file.getParentIds());
            for (String parentId : parentIds) {
                totalChildCount += blogKnowledgeMapper.deleteByDocId(parentId);
                try {
                    totalParentCount += knowledgeParentChunkMapper.deleteById(UUID.fromString(parentId));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的 parent_id 格式: {}", parentId);
                }
            }
        }

        // 批量删除文件记录
        int fileCount = knowledgeFileMapper.deleteBatchIds(fileIds);

        log.info("批量级联删除知识库文件: fileIds={}, 删除文件={}, Parent={}, Child={}",
                fileIds, fileCount, totalParentCount, totalChildCount);
        return new int[]{fileCount, totalParentCount, totalChildCount};
    }

    /**
     * 转换为详情 VO
     */
    private KnowledgeFileDetailVO convertToDetailVO(KnowledgeFile file) {
        return KnowledgeFileDetailVO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .originalName(file.getOriginalName())
                .description(file.getDescription())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .chunkCount(file.getChunkCount())
                .parentIds(parseParentIds(file.getParentIds()))
                .source(file.getSource())
                .category(file.getCategory())
                .uploaderId(file.getUploaderId())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    /**
     * 解析 parentIds JSON 字符串为列表
     * 格式: ["uuid1","uuid2"] → ["uuid1", "uuid2"]
     */
    private List<String> parseParentIds(String parentIdsJson) {
        if (parentIdsJson == null || parentIdsJson.isEmpty() || "[]".equals(parentIdsJson)) {
            return List.of();
        }
        try {
            String cleaned = parentIdsJson.replaceAll("[\\[\\]\"]", "");
            if (cleaned.isEmpty()) {
                return List.of();
            }
            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析 parentIds 失败: {}", parentIdsJson, e);
            return List.of();
        }
    }

    /**
     * 转换为列表 VO
     */
    private KnowledgeFileListVO convertToListVO(KnowledgeFile file) {
        return KnowledgeFileListVO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .description(file.getDescription())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .chunkCount(file.getChunkCount())
                .source(file.getSource())
                .category(file.getCategory())
                .uploaderId(file.getUploaderId())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
