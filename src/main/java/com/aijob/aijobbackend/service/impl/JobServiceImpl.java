package com.aijob.aijobbackend.service.impl;

import com.aijob.aijobbackend.dto.JobQuery;
import com.aijob.aijobbackend.entity.JobPosition;
import com.aijob.aijobbackend.mapper.JobPositionMapper;
import com.aijob.aijobbackend.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 职位业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final String OPTION_UNLIMITED = "不限";

    private final JobPositionMapper jobPositionMapper;

    @Override
    public List<JobPosition> search(JobQuery query) {
        JobQuery condition = query != null ? query : new JobQuery();
        List<JobPosition> jobs = jobPositionMapper.selectByCondition(
                trimToNull(condition.getKeyword()),
                normalizeOption(condition.getLocation()),
                normalizeOption(condition.getEducation()),
                normalizeOption(condition.getExperience()),
                normalizeOption(condition.getSize()),
                normalizeOption(condition.getFinancing()),
                condition.getSalaryMin(),
                condition.getSalaryMax());
        log.info("查询职位列表，keyword={}，location={}，education={}，experience={}，size={}，financing={}，salary=[{},{}]，count={}",
                condition.getKeyword(), condition.getLocation(), condition.getEducation(),
                condition.getExperience(), condition.getSize(), condition.getFinancing(),
                condition.getSalaryMin(), condition.getSalaryMax(), jobs.size());
        return jobs;
    }

    /** 前端传「不限」或空字符串时视为不筛选 */
    private static String normalizeOption(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return OPTION_UNLIMITED.equals(trimmed) ? null : trimmed;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
