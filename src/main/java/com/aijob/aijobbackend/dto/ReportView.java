package com.aijob.aijobbackend.dto;

import java.time.LocalDateTime;

/**
 * 简历分析报告视图（用于前端轮询）
 *
 * @param reportId   报告ID
 * @param resumeId   简历ID
 * @param status     状态：0-分析中，1-成功，2-失败
 * @param result     Dify 返回的 answer 原文（JSON 字符串，前端自行解析）
 * @param errorMsg   失败原因
 * @param createTime 报告创建时间
 */
public record ReportView(Long reportId, Long resumeId, Integer status, String result,
                         String errorMsg, LocalDateTime createTime) {
}
