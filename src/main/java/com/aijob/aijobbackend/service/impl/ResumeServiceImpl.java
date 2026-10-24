package com.aijob.aijobbackend.service.impl;

import com.aijob.aijobbackend.dto.ReportView;
import com.aijob.aijobbackend.dto.ResumeHistoryItem;
import com.aijob.aijobbackend.dto.ResumeSummary;
import com.aijob.aijobbackend.entity.Resume;
import com.aijob.aijobbackend.entity.ResumeReport;
import com.aijob.aijobbackend.exception.ResumeParseException;
import com.aijob.aijobbackend.mapper.ResumeMapper;
import com.aijob.aijobbackend.mapper.ResumeReportMapper;
import com.aijob.aijobbackend.service.DifyService;
import com.aijob.aijobbackend.service.ResumeService;
import com.aijob.aijobbackend.util.ResumeTextExtractor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
    /** 简历列表最多返回条数 */
    private static final int RESUME_LIST_LIMIT = 100;

    private final ResumeMapper resumeMapper;
    private final ResumeReportMapper resumeReportMapper;
    private final DifyService difyService;

    @Override
    public ResumeSummary saveFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeParseException("请选择要上传的简历文件");
        }

        String fileName = normalizeFileName(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResumeParseException("读取上传文件失败：" + e.getMessage(), e);
        }
        String content = ResumeTextExtractor.extract(bytes, fileName);

        Resume resume = new Resume();
        resume.setFileName(fileName);
        resume.setContent(content);
        resumeMapper.insert(resume);

        // 回查一次，拿到数据库默认值生成的 create_time
        Resume saved = resumeMapper.selectById(resume.getId());
        log.info("简历上传成功，resumeId={}，fileName={}，contentLength={}",
                saved.getId(), saved.getFileName(), content.length());
        return new ResumeSummary(saved.getId(), saved.getFileName(), saved.getCreateTime());
    }

    @Override
    public List<ResumeSummary> listRecent() {
        List<Resume> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .select(Resume::getId, Resume::getFileName, Resume::getCreateTime)
                .orderByDesc(Resume::getId)
                .last("LIMIT " + RESUME_LIST_LIMIT));
        return resumes.stream()
                .map(resume -> new ResumeSummary(resume.getId(), resume.getFileName(), resume.getCreateTime()))
                .toList();
    }

    @Override
    public ReportView getLatestReport(Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        ResumeReport report = resumeReportMapper.selectOne(new LambdaQueryWrapper<ResumeReport>()
                .eq(ResumeReport::getResumeId, resumeId)
                .orderByDesc(ResumeReport::getId)
                .last("LIMIT 1"));
        if (report == null) {
            return null;
        }
        return new ReportView(report.getId(), report.getResumeId(), report.getStatus(),
                report.getResult(), report.getErrorMsg(), report.getCreateTime());
    }

    @Override
    public List<ResumeHistoryItem> listHistory() {
        return resumeReportMapper.selectRecentHistory();
    }

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

    /** 只保留文件名部分，避免客户端传入路径 */
    private static String normalizeFileName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "未命名简历";
        }
        String name = originalName.replace('\\', '/');
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        return StringUtils.hasText(fileName) ? fileName : "未命名简历";
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
