package com.personblog.ai.controller;

import com.personblog.admin.aspect.RecordLog;
import com.personblog.ai.BizService.KnowledgeService;
import com.personblog.ai.dto.KnowledgeUploadVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "知识库管理", description = "知识库文档上传和管理接口")
@Slf4j
@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "上传文件", description = "上传文件到知识库，支持 .txt 和 .md 文件，自动切片并向量化存储")
    @PostMapping("/file")
    @RecordLog(Type = "create",businessType = "knowledge",description = "上传知识库文件")
    public JsonData<KnowledgeUploadVO> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".txt") && !filename.endsWith(".md"))) {
            return JsonData.buildError("仅支持 .txt 和 .md 文件");
        }
        
        KnowledgeUploadVO result = knowledgeService.uploadFile(file);
        return JsonData.buildSuccess(result);
    }
}
