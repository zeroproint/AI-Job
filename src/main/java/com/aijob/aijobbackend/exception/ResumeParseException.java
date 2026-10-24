package com.aijob.aijobbackend.exception;

/**
 * 简历文件解析失败（格式不支持、文件损坏、内容为空等）时抛出的业务异常
 */
public class ResumeParseException extends RuntimeException {

    public ResumeParseException(String message) {
        super(message);
    }

    public ResumeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
