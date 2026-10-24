-- ============================================================
-- AI 求职机 - 数据库初始化脚本（模块 1）
-- 执行方式：IDEA Database 面板 -> 连接到 MySQL -> 打开 Console -> 粘贴执行
-- 说明：脚本可重复执行（IF NOT EXISTS），不会删除已有数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai-job`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `ai-job`;

-- ------------------------------------------------------------
-- 简历表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `resume`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED          DEFAULT NULL COMMENT '所属用户ID（预留，后续接入用户体系）',
    `file_name`   VARCHAR(255)             DEFAULT NULL COMMENT '简历原始文件名',
    `content`     LONGTEXT        NOT NULL COMMENT '简历文本内容（AI 分析输入）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '简历表';

-- ------------------------------------------------------------
-- 简历分析报告表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `resume_report`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resume_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联的简历ID',
    `result`      LONGTEXT NULL COMMENT 'AI 分析结果（Dify 返回的 answer 原文）',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-分析中，1-分析成功，2-分析失败',
    `error_msg`   VARCHAR(1000)            DEFAULT NULL COMMENT '失败原因（status=2 时有值）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_resume_id` (`resume_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '简历分析报告表';

-- 可选：插入一条测试简历，方便后续模块联调时使用（按需执行）
-- INSERT INTO `resume` (`file_name`, `content`)
-- VALUES ('张三-简历.txt', '张三，男，5年Java后端开发经验，熟悉 Spring Boot、MySQL、Redis，曾负责电商订单系统重构。');
