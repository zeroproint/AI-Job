package com.aijob.aijobbackend.controller;

import com.aijob.aijobbackend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
