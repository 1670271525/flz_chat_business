package com.flz_chat_business.flz_chat.file.controller;

import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.flz_chat.file.dto.FilePresignRequest;
import com.flz_chat_business.flz_chat.file.service.MinioPresignService;
import com.flz_chat_business.flz_chat.file.vo.FileDownloadPresignVO;
import com.flz_chat_business.flz_chat.file.vo.FileUploadPresignVO;
import com.flz_chat_business.security.util.SecurityUtils;
import com.flz_chat_business.vo.Result;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
/**
 * 文件业务接口（MinIO）。
 */
public class FileController {

    private final MinioPresignService minioPresignService;

    @PostMapping("/presign")
    /**
     * 生成上传预签名地址。
     *
     * @param request bucket 为桶别名(chat/public)，filename 为原文件名，contentType 为文件类型，size 为文件大小
     * @return objectKey、uploadUrl、expireSeconds
     */
    public Result<FileUploadPresignVO> presignUpload(@Valid @RequestBody FilePresignRequest request) {
        try {
            return Result.success(minioPresignService.presignUpload(SecurityUtils.getCurrentUserId(), request));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResponseCodeEnum.SYSTEM_EXCEPTION.getCode(), "生成上传预签名失败: " + ex.getMessage());
        }
    }

    @GetMapping("/presign")
    /**
     * 生成下载预签名地址。
     *
     * @param objectKey MinIO 对象键
     * @return url、expireSeconds
     */
    public Result<FileDownloadPresignVO> presignDownload(@RequestParam("objectKey") String objectKey) {
        if (StringUtils.isBlank(objectKey)) {
            throw new BizException(ResponseCodeEnum.PARAM_INVALID);
        }
        try {
            return Result.success(minioPresignService.presignDownload(objectKey));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResponseCodeEnum.SYSTEM_EXCEPTION.getCode(), "生成下载预签名失败: " + ex.getMessage());
        }
    }

    @PostMapping("/upload")
    /**
     * 服务端代理上传文件到 MinIO。
     *
     * @param bucket 目标桶别名（chat/public）
     * @param file 上传文件
     * @return data 为写入后的 objectKey
     */
    public Result<Object> upload(@RequestParam("bucket") String bucket, @RequestPart("file") MultipartFile file) {
        try {
            String objectKey = minioPresignService.upload(SecurityUtils.getCurrentUserId(), bucket, file);
            return Result.success(objectKey);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResponseCodeEnum.SYSTEM_EXCEPTION.getCode(), "文件上传失败: " + ex.getMessage());
        }
    }
}
