package com.personblog.ai.BizService;

import com.personblog.ai.entity.KnowledgeFile;
import com.personblog.ai.mapper.KnowledgeFileMapper;
import com.personblog.ai.vo.KnowledgeFileUploadVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.AliOssUtil;
import com.personblog.common.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.personblog.ai.constants.PythonAiApiConstants.*;

/**
 * 知识库文件上传服务
 * 负责调用 Python AI 服务进行文件向量化，并将文件元数据保存到数据库
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final WebClient pythonAiWebClient;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final AliOssUtil aliOssUtil;

    /**
     * 上传文件到知识库
     * 流程：OSS上传 → Python向量化 → DB存储
     *
     * @param file        文件
     * @param description 文件描述（可选）
     * @return 上传结果
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFileUploadVO uploadFile(MultipartFile file, String description) {
        String filename = file.getOriginalFilename();
        log.info("上传文件到知识库: {}", filename);

        try {
            // Step 1: 上传源文件到阿里云 OSS
            String objectName = "knowledge/" + UUID.randomUUID() + ".md";
            String fileUrl;
            try {
                byte[] fileBytes = file.getBytes();
                fileUrl = aliOssUtil.upload(fileBytes, objectName);
                log.info("文件已上传到 OSS: {}", fileUrl);
            } catch (Exception e) {
                log.warn("OSS 上传失败，继续处理: {}", e.getMessage());
                fileUrl = "";
            }

            // Step 2: 调用 Python 服务进行 Parent-Child Chunking 和向量化
            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(
                    file.getResource(),
                    new DefaultDataBufferFactory(),
                    4096
            );

            builder.asyncPart("file", dataBufferFlux, DataBuffer.class)
                    .filename(Objects.requireNonNull(filename))
                    .contentType(MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())));

            Map<String, Object> response = pythonAiWebClient.post()
                    .uri(Knowledge.FILE_UPLOAD)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            // Step 3: 解析 Python 服务的响应
            Map<String, Object> data = parseResponse(response);
            Integer chunkCount = (Integer) data.get(Fields.CHUNK_COUNT);
            List<String> parentIds = (List<String>) data.get(Fields.IDS);

            // Step 4: 插入 tb_knowledge_file 记录
            KnowledgeFile knowledgeFile = new KnowledgeFile();
            knowledgeFile.setFileName(filename);
            knowledgeFile.setOriginalName(filename);
            knowledgeFile.setDescription(description != null ? description : "");
            knowledgeFile.setFileUrl(fileUrl);
            knowledgeFile.setFileSize(file.getSize());
            knowledgeFile.setChunkCount(chunkCount != null ? chunkCount : 0);
            knowledgeFile.setParentIds(parentIds != null ? toJson(parentIds) : "[]");
            knowledgeFile.setSource("file_upload");
            knowledgeFile.setUploaderId(UserContextHolder.getUserId());
            knowledgeFile.setCreatedAt(LocalDateTime.now());
            knowledgeFile.setUpdatedAt(LocalDateTime.now());

            knowledgeFileMapper.insert(knowledgeFile);
            log.info("知识库文件记录已保存: id={}, fileName={}", knowledgeFile.getId(), filename);

            // Step 5: 返回结果
            return KnowledgeFileUploadVO.builder()
                    .fileId(knowledgeFile.getId())
                    .fileName(filename)
                    .fileUrl(fileUrl)
                    .chunkCount(chunkCount)
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BizException(BizCodeEnum.AI_FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 解析 Python 服务的响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(Map<String, Object> response) {
        if (response == null || !Msg.SUCCESS.equals(response.get(Fields.MSG))) {
            String errorMsg = response != null ? (String) response.get(Fields.MSG) : "未知错误";
            log.error("上传失败: {}", errorMsg);
            throw new BizException(BizCodeEnum.AI_FILE_UPLOAD_ERROR);
        }

        Map<String, Object> data = (Map<String, Object>) response.get(Fields.DATA);
        if (data == null) {
            throw new BizException(BizCodeEnum.AI_RESPONSE_EMPTY);
        }

        return data;
    }

    /**
     * 将列表转换为 JSON 字符串
     */
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
