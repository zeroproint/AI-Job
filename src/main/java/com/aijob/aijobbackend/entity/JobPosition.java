package com.aijob.aijobbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位表
 */
@Data
@TableName("job_position")
public class JobPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 职位名 */
    private String name;

    /** 薪资展示文案，如 25-40K·14薪 */
    private String salary;

    /** 月薪下限（单位 K），用于筛选 */
    private Integer salaryMin;

    /** 月薪上限（单位 K），用于筛选 */
    private Integer salaryMax;

    /** 公司名 */
    private String company;

    /** 公司规模 */
    private String companySize;

    /** 技能标签，逗号分隔 */
    private String tags;

    /** 工作地点 */
    private String location;

    /** 学历要求 */
    private String education;

    /** 经验要求 */
    private String experience;

    /** 融资阶段 */
    private String financing;

    /** 创建时间（由数据库默认值维护） */
    private LocalDateTime createTime;
}
