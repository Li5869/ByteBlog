package com.personblog.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
 
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {
 
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
 
    public String upload(byte[] bytes, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
 
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            int lastDotIndex = objectName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String extension = objectName.substring(lastDotIndex + 1).toLowerCase();
                String contentType = getContentType(extension);
                if (contentType != null) {
                    metadata.setContentType(contentType);
                }
            }
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);
 
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes), metadata);
            
            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
            
        } catch (OSSException oe) {
            log.error("OSS异常: ErrorCode={}, ErrorMessage={}, RequestId={}", 
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            throw new RuntimeException("文件上传失败: " + oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage());
            throw new RuntimeException("文件上传失败: " + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
 
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);
 
        log.info("文件访问URL: {}", stringBuilder.toString());
 
        return stringBuilder.toString();
    }
    
    private String getContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> null;
        };
    }
}
