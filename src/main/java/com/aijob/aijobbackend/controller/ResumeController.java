package com.aijob.aijobbackend.controller;

import com.aijob.aijobbackend.dto.ReportView;
import com.aijob.aijobbackend.dto.ResumeHistoryItem;
import com.aijob.aijobbackend.dto.ResumeSummary;
import com.aijob.aijobbackend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 简历接口
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 上传并解析简历文件（PDF / Word / TXT），解析出的文本入库。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeSummary upload(@RequestParam("file") MultipartFile file) {
        return resumeService.saveFromFile(file);
    }

    /**
     * 最近上传的简历列表（不含简历正文）。
     */
    @GetMapping("/list")
    public List<ResumeSummary> list() {
        return resumeService.listRecent();
    }

    /**
     * 简历分析历史：最近 10 份简历的最新一次报告。
     */
    @GetMapping("/history")
    public List<ResumeHistoryItem> history() {
        return resumeService.listHistory();
    }

    /**
     * 查询某份简历的最新分析报告。
     * 尚未生成报告时返回 404，前端轮询据此继续等待。
     */
    @GetMapping("/report/{resumeId}")
    public ResponseEntity<ReportView> report(@PathVariable("resumeId") Long resumeId) {
        ReportView report = resumeService.getLatestReport(resumeId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    /**
     * 提交简历分析任务（异步执行，立即返回）。
     *
     * @param id 简历ID
     */
    @PostMapping("/analyze/{id}")
    public ResponseEntity<Map<String, Object>> analyze(@PathVariable("id") Long id) {
        // 同步校验简历是否存在，无效ID直接返回 400，不进入异步流程
        if (!resumeService.exists(id)) {
            log.warn("简历不存在或已删除，拒绝分析请求，resumeId={}", id);
            return ResponseEntity.badRequest().body(Map.of(
                    "resumeId", id,
                    "message", "简历不存在或已删除"));
        }

        resumeService.asyncAnalyze(id);
        log.info("已提交简历分析任务，resumeId={}", id);
        return ResponseEntity.ok(Map.of(
                "resumeId", id,
                "message", "分析任务已提交，请稍后查询分析结果"));
    }
}
