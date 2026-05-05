package com.personblog.common.file;


import com.personblog.common.result.JsonData;
import com.personblog.common.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文件")
public class FileUpload {

    private final AliOssUtil aliOssUtil;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".zip", ".rar"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    public JsonData<String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return JsonData.buildError("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return JsonData.buildError("文件大小不能超过10MB");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return JsonData.buildError("不支持的文件类型");
            }

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            String objectName = datePath + "/" + fileName;

            byte[] bytes = file.getBytes();
            String fileUrl = aliOssUtil.upload(bytes, objectName);

            log.info("文件上传成功: {}", fileUrl);
            return JsonData.buildSuccess(fileUrl);

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return JsonData.buildError("文件上传失败");
        }
    }
}
