package com.aijob.aijobbackend.service;

import com.aijob.aijobbackend.dto.ReportView;
import com.aijob.aijobbackend.dto.ResumeSummary;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历业务服务
 */
public interface ResumeService {

    /**
     * 解析上传的简历文件（PDF/Word/TXT）并入库。
     *
     * @param file 上传文件
     * @return 新建简历的摘要
     */
    ResumeSummary saveFromFile(MultipartFile file);

    /**
     * 查询最近上传的简历列表（不含 content）。
     *
     * @return 按上传时间倒序的简历摘要
     */
    List<ResumeSummary> listRecent();

    /**
     * 查询某份简历最新一次分析报告。
     *
     * @param resumeId 简历ID
     * @return 报告视图；尚未生成报告时返回 null
     */
    ReportView getLatestReport(Long resumeId);

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
