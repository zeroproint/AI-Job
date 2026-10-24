package com.aijob.aijobbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历分析历史记录（resume + resume_report 关联查询结果）
 */
@Data
public class ResumeHistoryItem {

    /** 报告ID */
    private Long reportId;

    /** 简历ID（点击历史记录时用于跳转报告页） */
    private Long resumeId;

    /** 文件名 */
    private String fileName;

    /** 简历上传时间 */
    private LocalDateTime uploadTime;

    /** 报告状态：0-分析中，1-成功，2-失败 */
    private Integer status;

    /** 总分（status=1 时从 result JSON 中提取，其余为 null） */
    private Integer totalScore;
}
