package com.aijob.aijobbackend.service.impl;

import com.aijob.aijobbackend.entity.Resume;
import com.aijob.aijobbackend.entity.ResumeReport;
import com.aijob.aijobbackend.mapper.ResumeMapper;
import com.aijob.aijobbackend.mapper.ResumeReportMapper;
import com.aijob.aijobbackend.service.DifyService;
import com.aijob.aijobbackend.service.ResumeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 简历业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    /** 报告状态：分析中 */
    private static final int STATUS_ANALYZING = 0;
    /** 报告状态：分析成功 */
    private static final int STATUS_SUCCESS = 1;
    /** 报告状态：分析失败 */
    private static final int STATUS_FAILED = 2;
    /** error_msg 字段长度上限 */
    private static final int ERROR_MSG_MAX_LENGTH = 1000;

    private final ResumeMapper resumeMapper;
    private final ResumeReportMapper resumeReportMapper;
    private final DifyService difyService;

    @Override
    public boolean exists(Long resumeId) {
        if (resumeId == null) {
            return false;
        }
        return resumeMapper.exists(new LambdaQueryWrapper<Resume>().eq(Resume::getId, resumeId));
    }

    @Override
    @Async("analysisExecutor")
    public void asyncAnalyze(Long resumeId) {
        log.info("开始异步分析简历，resumeId={}", resumeId);

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            log.warn("简历不存在或已删除，resumeId={}", resumeId);
            return;
        }
        if (!StringUtils.hasText(resume.getContent())) {
            log.warn("简历内容为空，跳过分析，resumeId={}", resumeId);
            return;
        }

        // 先落一条「分析中」记录，便于前端轮询；分析完成后原地更新
        ResumeReport report = new ResumeReport();
        report.setResumeId(resumeId);
        report.setStatus(STATUS_ANALYZING);
        resumeReportMapper.insert(report);

        try {
            String answer = difyService.chat(resume.getContent());
            report.setResult(answer);
            report.setStatus(STATUS_SUCCESS);
            resumeReportMapper.updateById(report);
            log.info("简历分析成功，resumeId={}，reportId={}", resumeId, report.getId());
        } catch (Exception e) {
            // 异步任务中的异常不会抛给调用方，必须在此记录并落库
            log.error("简历分析失败，resumeId={}，reportId={}", resumeId, report.getId(), e);
            report.setStatus(STATUS_FAILED);
            report.setErrorMsg(truncate(e.getMessage()));
            resumeReportMapper.updateById(report);
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= ERROR_MSG_MAX_LENGTH
                ? message
                : message.substring(0, ERROR_MSG_MAX_LENGTH);
    }
}
