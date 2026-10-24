package com.aijob.aijobbackend.dto;

import java.time.LocalDateTime;

/**
 * 简历摘要（列表 / 上传接口返回，不含 content 大字段）
 *
 * @param id         简历ID
 * @param fileName   文件名
 * @param createTime 上传时间
 */
public record ResumeSummary(Long id, String fileName, LocalDateTime createTime) {
}
