package com.jichi.ragkb.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * 上传文件到 MinIO，返回对象路径。
     * 路径格式：kb/{kbId}/{uuid}-{originalFileName}
     */
    public String upload(Long kbId, MultipartFile file) {
        String objectPath = String.format("kb/%d/%s-%s",
                kbId, UUID.randomUUID().toString().substring(0, 8), file.getOriginalFilename());
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("[MinIO] 上传成功：path={}", objectPath);
            return objectPath;
        } catch (Exception e) {
            log.error("[MinIO] 上传失败：path={}，error={}", objectPath, e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从 MinIO 下载文件内容。
     */
    public byte[] download(String objectPath) {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build());
            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("[MinIO] 下载失败：path={}，error={}", objectPath, e.getMessage(), e);
            throw new RuntimeException("文件下载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除 MinIO 中的文件（文档删除时调用）。
     */
    public void delete(String objectPath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build());
            log.info("[MinIO] 删除成功：path={}", objectPath);
        } catch (Exception e) {
            // 删除失败不抛出异常，只记录警告（文件可能已经不存在）
            log.warn("[MinIO] 删除失败（可能已不存在）：path={}，error={}", objectPath, e.getMessage());
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("[MinIO] Bucket 已创建：{}", bucket);
        }
    }
}