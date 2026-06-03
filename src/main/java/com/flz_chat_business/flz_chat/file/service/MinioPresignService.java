package com.flz_chat_business.flz_chat.file.service;

import com.flz_chat_business.common.util.DateTimes;
import com.flz_chat_business.config.properties.MinioProperties;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.flz_chat.file.dto.FilePresignRequest;
import com.flz_chat_business.flz_chat.file.vo.FileDownloadPresignVO;
import com.flz_chat_business.flz_chat.file.vo.FileUploadPresignVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * MinIO 文件服务。
 * 提供上传预签名、下载预签名、服务端直传能力。
 */
public class MinioPresignService {

    /** MinIO 预签名 URL 最长有效期（SDK 限制 7 天） */
    private static final int MAX_PRESIGN_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    private static final Set<String> ALLOWED_CONTENT_TYPE = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "audio/mpeg", "audio/aac", "video/mp4", "application/pdf",
            "application/octet-stream"
    ));

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 生成上传预签名。
     *
     * @param userId 当前用户ID
     * @param request 文件参数（桶、文件名、内容类型、大小）
     * @return 上传预签名信息
     * @throws Exception MinIO SDK 异常
     */
    public FileUploadPresignVO presignUpload(Long userId, FilePresignRequest request) throws Exception {
        String bucket = resolveBucket(request.getBucket());
        String ext = getFileExt(request.getFilename());
        String objectKey = request.getBucket() + "/" + userId + "/"
                + LocalDate.now(DateTimes.ZONE).toString().replace("-", "/") + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (StringUtils.isBlank(ext) ? "" : "." + ext);
        int expireSeconds = 600;
        String uploadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(expireSeconds)
                        .build()
        );
        return new FileUploadPresignVO(objectKey, uploadUrl, expireSeconds);
    }

    /**
     * 生成下载预签名。
     *
     * @param objectKey 对象键
     * @return 下载预签名信息
     * @throws Exception MinIO SDK 异常
     */
    public FileDownloadPresignVO presignDownload(String objectKey) throws Exception {
        int configuredSeconds = resolveConfiguredExpirySeconds();
        if (objectKey != null && objectKey.startsWith("public/")) {
            String directUrl = buildPublicDirectUrl(objectKey);
            if (directUrl != null) {
                return new FileDownloadPresignVO(directUrl, configuredSeconds);
            }
        }
        String bucket = resolveBucketByObjectKey(objectKey);
        int expireSeconds = Math.min(configuredSeconds, MAX_PRESIGN_EXPIRY_SECONDS);
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(expireSeconds)
                        .build()
        );
        return new FileDownloadPresignVO(url, expireSeconds);
    }

    /**
     * 由业务服务代传文件到 MinIO。
     *
     * @param userId 当前用户ID
     * @param bucketAlias 桶别名（chat/public）
     * @param file 文件对象
     * @return 生成后的 objectKey
     * @throws Exception MinIO SDK 异常
     */
    public String upload(Long userId, String bucketAlias, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "文件大小不可超过10MB");
        }
        if (!ALLOWED_CONTENT_TYPE.contains(file.getContentType())) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "不支持的文件类型");
        }
        String bucket = resolveBucket(bucketAlias);
        String ext = getFileExt(file.getOriginalFilename());
        String objectKey = bucketAlias + "/" + userId + "/"
                + LocalDate.now(DateTimes.ZONE).toString().replace("-", "/") + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (StringUtils.isBlank(ext) ? "" : "." + ext);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(new ByteArrayInputStream(file.getBytes()), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return objectKey;
    }

    private String resolveBucket(String bucketAlias) {
        if ("public".equalsIgnoreCase(bucketAlias)) {
            return minioProperties.getBucketPublic();
        }
        if ("chat".equalsIgnoreCase(bucketAlias)) {
            return minioProperties.getBucketChat();
        }
        throw new BizException(ResponseCodeEnum.PARAM_INVALID.getCode(), "bucket 仅支持 chat/public");
    }

    private String resolveBucketByObjectKey(String objectKey) {
        if (objectKey != null && objectKey.startsWith("public/")) {
            return minioProperties.getBucketPublic();
        }
        return minioProperties.getBucketChat();
    }

    private String getFileExt(String filename) {
        if (StringUtils.isBlank(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private int resolveConfiguredExpirySeconds() {
        Integer days = minioProperties.getPresignedExpiryDays();
        if (days == null || days <= 0) {
            days = 7;
        }
        return days * 24 * 60 * 60;
    }

    /**
     * public 桶资源拼接直链（需桶开启匿名读或经 CDN/网关代理）。
     */
    private String buildPublicDirectUrl(String objectKey) {
        String base = minioProperties.getPublicBaseUrl();
        if (StringUtils.isBlank(base)) {
            return null;
        }
        return StringUtils.stripEnd(base, "/") + "/"
                + minioProperties.getBucketPublic() + "/"
                + objectKey;
    }
}
