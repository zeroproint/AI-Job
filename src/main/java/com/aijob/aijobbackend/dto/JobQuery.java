package com.aijob.aijobbackend.dto;

import lombok.Data;

/**
 * 职位筛选条件（GET /api/job/list 的查询参数）
 * <p>
 * 所有字段都可为空，为空表示不限制；前端选择「不限」时不会传该参数。
 */
@Data
public class JobQuery {

    /** 关键词：匹配职位名、公司、地点、标签 */
    private String keyword;

    /** 地点，如 北京 / 北京·海淀 */
    private String location;

    /** 学历要求，如 本科 */
    private String education;

    /** 经验要求，如 3-5 年 */
    private String experience;

    /** 公司规模，如 1000-9999人 */
    private String size;

    /** 融资阶段，如 已上市 */
    private String financing;

    /** 薪资筛选下限（K） */
    private Integer salaryMin;

    /** 薪资筛选上限（K） */
    private Integer salaryMax;
}
