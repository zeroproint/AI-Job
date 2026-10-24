package com.aijob.aijobbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历分析报告表
 */
@Data
@TableName("resume_report")
public class ResumeReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联的简历ID */
    private Long resumeId;

    /** AI 分析结果（Dify 返回的 answer 原文） */
    private String result;

    /** 状态：0-分析中，1-分析成功，2-分析失败 */
    private Integer status;

    /** 失败原因（status=2 时有值） */
    private String errorMsg;

    /** 创建时间（由数据库默认值维护） */
    private LocalDateTime createTime;

    /** 更新时间（由数据库 ON UPDATE 维护） */
    private LocalDateTime updateTime;

    /** 逻辑删除：0-正常，1-已删除 */
    @TableLogic
    private Integer deleted;
}
