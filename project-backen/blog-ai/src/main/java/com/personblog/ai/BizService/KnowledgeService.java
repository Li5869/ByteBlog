package com.personblog.ai.BizService;

import com.personblog.ai.dto.KnowledgeUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final WebClient pythonAiWebClient;

    public KnowledgeUploadVO uploadFile(MultipartFile file) {
        log.info("上传文件到知识库: {}", file.getOriginalFilename());
        
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            
            Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(
                file.getResource(),
                new DefaultDataBufferFactory(),
                4096
            );
            
            builder.asyncPart("file", dataBufferFlux, DataBuffer.class)
                    .filename(Objects.requireNonNull(file.getOriginalFilename()))
                    .contentType(MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())));
            
            Map<String, Object> response = pythonAiWebClient.post()
                    .uri("/api/v1/knowledge/file")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            return parseResponse(response);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private KnowledgeUploadVO parseResponse(Map<String, Object> response) {
        if (response == null || !"success".equals(response.get("msg"))) {
            String errorMsg = response != null ? (String) response.get("msg") : "未知错误";
            throw new RuntimeException("上传失败: " + errorMsg);
        }
        
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            throw new RuntimeException("响应数据为空");
        }
        
        KnowledgeUploadVO vo = new KnowledgeUploadVO();
        
        if (data.containsKey("chunk_count")) {
            vo.setChunkCount((Integer) data.get("chunk_count"));
        }
        
        if (data.containsKey("filename")) {
            vo.setFilename((String) data.get("filename"));
        }
        
        if (data.containsKey("ids")) {
            vo.setIds((java.util.List<String>) data.get("ids"));
        }
        
        return vo;
    }
}
