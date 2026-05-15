package com.personblog.ai.BizService;

import com.personblog.ai.dto.KnowledgeUploadVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
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

import static com.personblog.ai.constants.PythonAiApiConstants.*;

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
                    .uri(Knowledge.FILE_UPLOAD)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            return parseResponse(response);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BizException(BizCodeEnum.AI_FILE_UPLOAD_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private KnowledgeUploadVO parseResponse(Map<String, Object> response) {
        if (response == null || !Msg.SUCCESS.equals(response.get(Fields.MSG))) {
            String errorMsg = response != null ? (String) response.get(Fields.MSG) : "未知错误";
            log.error("上传失败: {}", errorMsg);
            throw new BizException(BizCodeEnum.AI_FILE_UPLOAD_ERROR);
        }
        
        Map<String, Object> data = (Map<String, Object>) response.get(Fields.DATA);
        if (data == null) {
            throw new BizException(BizCodeEnum.AI_RESPONSE_EMPTY);
        }
        
        KnowledgeUploadVO vo = new KnowledgeUploadVO();
        
        if (data.containsKey(Fields.CHUNK_COUNT)) {
            vo.setChunkCount((Integer) data.get(Fields.CHUNK_COUNT));
        }
        
        if (data.containsKey(Fields.FILENAME)) {
            vo.setFilename((String) data.get(Fields.FILENAME));
        }
        
        if (data.containsKey(Fields.IDS)) {
            vo.setIds((java.util.List<String>) data.get(Fields.IDS));
        }
        
        return vo;
    }
}
