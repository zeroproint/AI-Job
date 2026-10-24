-- ============================================================
-- AI 求职机 - 职位表（模块 7 步骤 1）
-- 执行方式：IDEA Database 面板 -> 连接到 MySQL -> 打开 Console -> 粘贴执行
-- 说明：脚本可重复执行（IF NOT EXISTS + INSERT IGNORE），不会删除已有数据
-- ============================================================

USE `ai-job`;

-- ------------------------------------------------------------
-- 职位表
-- salary_min / salary_max / financing 为步骤 3 的筛选预留：
-- salary 用于展示，数值区间用于薪资范围筛选；financing 对应筛选页的「融资」分类
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `job_position`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(100)    NOT NULL COMMENT '职位名',
    `salary`       VARCHAR(50)     NOT NULL COMMENT '薪资展示文案，如 25-40K·14薪',
    `salary_min`   INT UNSIGNED    NOT NULL COMMENT '月薪下限（单位 K），用于筛选',
    `salary_max`   INT UNSIGNED    NOT NULL COMMENT '月薪上限（单位 K），用于筛选',
    `company`      VARCHAR(100)    NOT NULL COMMENT '公司名',
    `company_size` VARCHAR(50)     NOT NULL COMMENT '公司规模',
    `tags`         VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '技能标签，逗号分隔',
    `location`     VARCHAR(100)    NOT NULL COMMENT '工作地点',
    `education`    VARCHAR(50)     NOT NULL COMMENT '学历要求',
    `experience`   VARCHAR(50)     NOT NULL COMMENT '经验要求',
    `financing`    VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '融资阶段',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_education` (`education`),
    KEY `idx_experience` (`experience`),
    KEY `idx_company_size` (`company_size`),
    KEY `idx_location` (`location`),
    KEY `idx_salary` (`salary_min`, `salary_max`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '职位表';

-- ------------------------------------------------------------
-- 模拟数据（10 条）
-- 学历 / 经验 / 规模 / 融资 的取值与前端筛选页选项保持一致，方便步骤 3 联调
-- ------------------------------------------------------------
INSERT IGNORE INTO `job_position`
(`id`, `name`, `salary`, `salary_min`, `salary_max`, `company`, `company_size`, `tags`, `location`, `education`,
 `experience`, `financing`)
VALUES (1, '高级Java开发工程师', '25-40K·14薪', 25, 40, '字节跳动', '1000-9999人', 'Java,Spring,微服务,Redis', '北京·海淀', '本科', '3-5 年', '已上市'),
       (2, '前端开发专家', '30-50K·16薪', 30, 50, '腾讯', '10000人以上', 'Vue,React,TypeScript,Webpack', '深圳·南山', '本科', '5-10 年', '已上市'),
       (3, '嵌入式软件工程师', '18-30K', 18, 30, '华为', '10000人以上', 'C/C++,RTOS,Linux,ARM', '深圳·龙岗', '本科', '3-5 年', '不需要融资'),
       (4, '网络安全工程师', '20-35K', 20, 35, '奇安信', '500-999人', '安全,渗透测试,防火墙', '北京·朝阳', '本科', '1-3 年', '已上市'),
       (5, '测试开发工程师', '12-20K', 12, 20, '金山软件', '500-999人', '自动化测试,Python,Selenium,Jenkins', '北京·海淀', '大专', '1-3 年', '已上市'),
       (6, 'Java 后端开发工程师', '20-35K·15薪', 20, 35, '美团', '1000-9999人', 'Java,Spring Boot,MySQL,微服务', '北京·朝阳', '本科', '3-5 年', '已上市'),
       (7, '前端开发工程师（校招）', '10-18K', 10, 18, '小红书', '1000-9999人', 'Vue3,TypeScript,小程序', '上海·徐汇', '本科', '应届生', 'D轮及以上'),
       (8, '算法工程师', '30-60K·16薪', 30, 60, '商汤科技', '1000-9999人', 'Python,深度学习,PyTorch,CV', '上海·浦东', '硕士', '3-5 年', '已上市'),
       (9, 'Java 后端开发工程师', '18-30K', 18, 30, '极客邦科技', '100-499人', 'Java,Spring Boot,MySQL', '北京·朝阳', '本科', '1-3 年', 'B轮'),
       (10, 'AI 算法专家', '40-70K·16薪', 40, 70, '百度', '10000人以上', '大模型,NLP,Python,分布式训练', '北京·海淀', '博士', '5-10 年', '已上市');
