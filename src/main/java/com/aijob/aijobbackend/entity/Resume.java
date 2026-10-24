package com.aijob.aijobbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历表
 */
@Data
@TableName("resume")
public class Resume implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户ID（预留，后续接入用户体系） */
    private Long userId;

    /** 简历原始文件名 */
    private String fileName;

    /** 简历文本内容（AI 分析输入） */
    private String content;

    /** 创建时间（由数据库默认值维护） */
    private LocalDateTime createTime;

    /** 更新时间（由数据库 ON UPDATE 维护） */
    private LocalDateTime updateTime;

    /** 逻辑删除：0-正常，1-已删除 */
    @TableLogic
    private Integer deleted;
}
