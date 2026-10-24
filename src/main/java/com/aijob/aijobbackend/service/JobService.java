package com.aijob.aijobbackend.service;

import com.aijob.aijobbackend.dto.JobQuery;
import com.aijob.aijobbackend.entity.JobPosition;

import java.util.List;

/**
 * 职位业务服务
 */
public interface JobService {

    /**
     * 按条件查询职位（条件均可为空）。
     *
     * @param query 筛选条件
     * @return 按 id 升序的职位列表
     */
    List<JobPosition> search(JobQuery query);
}
