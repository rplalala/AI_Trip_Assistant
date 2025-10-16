package com.demo.api.exception;

import com.demo.api.ApiRespond;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiRespond<Void> handleException(Exception e) {
        e.printStackTrace();
        return ApiRespond.error("系统错误: " + e.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiRespond<Void> handleBusinessException(BusinessException e) {
        return ApiRespond.error(e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiRespond<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiRespond.error("参数错误: " + e.getMessage());
    }

    /**
     * 处理认证异常
     * @param e
     * @return
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiRespond<Void> handleAuthException(AuthException e) {
        return ApiRespond.error(e.getMessage());
    }

}
