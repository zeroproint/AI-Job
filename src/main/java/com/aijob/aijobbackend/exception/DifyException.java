package com.aijob.aijobbackend.exception;

/**
 * 调用 Dify 失败时抛出的业务异常
 */
public class DifyException extends RuntimeException {

    public DifyException(String message) {
        super(message);
    }

    public DifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
