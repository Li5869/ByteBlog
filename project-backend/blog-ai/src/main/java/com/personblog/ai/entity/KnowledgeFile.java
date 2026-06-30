package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文件表
 *
 * @author LSH
 */
@Data
@TableName("tb_knowledge_file")
public class KnowledgeFile {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 当前显示的文件名（可重命名）
     */
    private String fileName;

    /**
     * 原始上传文件名
     */
    private String originalName;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 阿里云OSS上的源文件访问地址
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * Parent Chunk 数量
     */
    private Integer chunkCount;

    /**
     * 关联的 Parent Chunk ID 列表（JSON 字符串）
     */
    private String parentIds;

    /**
     * 来源：file_upload
     */
    private String source;

    /**
     * 知识库分类：project(项目知识库) / interview(面试知识库) / general(通用)
     */
    private String category;

    /**
     * 上传者用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long uploaderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
