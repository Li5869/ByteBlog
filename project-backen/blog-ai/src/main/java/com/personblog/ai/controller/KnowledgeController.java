package com.personblog.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.ai.BizService.KnowledgeService;
import com.personblog.ai.dto.KnowledgeFileBatchDeleteDTO;
import com.personblog.ai.dto.KnowledgeFileQueryDTO;
import com.personblog.ai.dto.KnowledgeFileUpdateDTO;
import com.personblog.ai.entity.KnowledgeFile;
import com.personblog.ai.service.IKnowledgeFileService;
import com.personblog.ai.vo.*;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理 Controller
 *
 * @author LSH
 */
@Tag(name = "知识库管理", description = "知识库文档上传和管理接口")
@Slf4j
@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final IKnowledgeFileService knowledgeFileService;

    @Operation(summary = "上传文件", description = "上传文件到知识库，仅支持 .md 文件，自动切片并向量化存储")
    @PostMapping("/file")
    @RecordLog(Type = "create", businessType = "knowledge", description = "上传知识库文件")
    public JsonData<KnowledgeFileUploadVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".md")) {
            return JsonData.buildError("仅支持 .md 格式的 Markdown 文件");
        }

        KnowledgeFileUploadVO result = knowledgeService.uploadFile(file, description);
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "分页查询知识库文件列表")
    @GetMapping("/list")
    public JsonData<Page<KnowledgeFileListVO>> getFileList(KnowledgeFileQueryDTO dto) {
        Page<KnowledgeFile> page = knowledgeFileService.getFilePage(dto);

        // 转换为 ListVO
        Page<KnowledgeFileListVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<KnowledgeFileListVO> records = page.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
        resultPage.setRecords(records);

        return JsonData.buildSuccess(resultPage);
    }

    @Operation(summary = "获取知识库文件详情")
    @GetMapping("/{fileId}")
    public JsonData<KnowledgeFileDetailVO> getFileDetail(@PathVariable Long fileId) {
        KnowledgeFileDetailVO detail = knowledgeFileService.getFileDetail(fileId);
        return JsonData.buildSuccess(detail);
    }

    @Operation(summary = "更新知识库文件信息")
    @PutMapping("/{fileId}")
    @RecordLog(Type = "update", businessType = "knowledge", description = "更新知识库文件信息")
    public JsonData<Void> updateFile(@PathVariable Long fileId, @RequestBody KnowledgeFileUpdateDTO dto) {
        knowledgeFileService.updateFile(fileId, dto);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "删除知识库文件（级联删除）")
    @DeleteMapping("/{fileId}")
    @RecordLog(Type = "delete", businessType = "knowledge", description = "删除知识库文件")
    public JsonData<KnowledgeFileDeleteVO> deleteFile(@PathVariable Long fileId) {
        int deletedCount = knowledgeFileService.cascadeDeleteById(fileId);
        KnowledgeFileDeleteVO vo = KnowledgeFileDeleteVO.builder()
                .fileId(fileId)
                .deletedParentCount(deletedCount)
                .deletedChildCount(0)
                .build();
        return JsonData.buildSuccess(vo);
    }

    @Operation(summary = "批量删除知识库文件")
    @PostMapping("/batch-delete")
    @RecordLog(Type = "delete", businessType = "knowledge", description = "批量删除知识库文件")
    public JsonData<KnowledgeFileBatchDeleteVO> batchDelete(@RequestBody KnowledgeFileBatchDeleteDTO dto) {
        int[] results = knowledgeFileService.cascadeDeleteByIds(dto.getFileIds());
        KnowledgeFileBatchDeleteVO vo = KnowledgeFileBatchDeleteVO.builder()
                .deletedCount(dto.getFileIds().size())
                .deletedParentCount(results.length > 0 ? results[0] : 0)
                .deletedChildCount(0)
                .build();
        return JsonData.buildSuccess(vo);
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
                .uploaderId(file.getUploaderId())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
