package com.aijob.aijobbackend.service;

/**
 * 简历业务服务
 */
public interface ResumeService {

    /**
     * 判断简历是否存在（已逻辑删除视为不存在）。
     *
     * @param resumeId 简历ID
     * @return 存在返回 true
     */
    boolean exists(Long resumeId);

    /**
     * 异步分析简历：查询简历 → 调用 Dify → 写入分析报告。
     * <p>
     * 分析过程在独立线程池中执行，方法立即返回；分析结果通过 resume_report 查询。
     *
     * @param resumeId 简历ID
     */
    void asyncAnalyze(Long resumeId);
}
