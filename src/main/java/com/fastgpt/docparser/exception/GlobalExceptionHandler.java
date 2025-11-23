package com.fastgpt.docparser.exception;

import com.fastgpt.docparser.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件大小超出限制", e);
        return ApiResponse.error(400, "文件大小超出限制，最大支持 50MB");
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统异常：" + e.getMessage());
    }
}
