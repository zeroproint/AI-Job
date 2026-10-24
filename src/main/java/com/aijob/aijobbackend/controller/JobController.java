package com.aijob.aijobbackend.controller;

import com.aijob.aijobbackend.dto.JobQuery;
import com.aijob.aijobbackend.entity.JobPosition;
import com.aijob.aijobbackend.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 职位接口
 */
@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * 职位列表，支持关键词、地点、学历、经验、规模、融资、薪资区间筛选。
     * 所有参数均可选，不传表示不限制。
     */
    @GetMapping("/list")
    public List<JobPosition> list(JobQuery query) {
        return jobService.search(query);
    }
}
