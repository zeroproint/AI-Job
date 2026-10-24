# ai-job-backend — AI 求职机后端

## 项目概况

- 用途：接收简历文本，调用本地 Dify CHATFLOW 应用生成结构化诊断报告。
- 技术栈：Spring Boot 4.1.2、Java 17、MySQL 8.0、MyBatis-Plus 3.5.17、WebClient。
- 包根：`com.aijob.aijobbackend`
- 数据库：`ai-job`（注意：不是 `ai-job-backend`），表 `resume`、`resume_report`，DDL 在 `sql/schema.sql`。
- 前端项目：`D:\ai-job\ai-job-frontend`（不在本仓库）。

## 环境与踩坑（重要）

- IDEA 2024.2.4 无法嵌入项目 wrapper 指定的 Maven 3.10.0（Maven server 报 Guice `ArtifactHandlerManager` 错误），会导致导入失败、模块 Language Level 回退到 1.5 并报“不支持发行版本 5”。IDEA 中 Maven home 选 Bundled；命令行使用 Maven 3.9.14：
  `C:\Users\柒玥\.m2\wrapper\dists\apache-maven-3.9.14-bin\1cb7fhup6b5n3bed6kckbrnspv\apache-maven-3.9.14\bin\mvn`
- JDK 17 路径：`D:/java17/bellsoft-jdk17.0.15+10-windows-amd64-full/jdk-17.0.15-full`。
  系统 `JAVA_HOME` 指向 JDK 8（`D:\jdk`），命令行构建前必须覆盖 `JAVA_HOME`。
- Boot 4 的 WebClient 自动配置来自 `spring-boot-starter-webclient`，不要用 `spring-boot-starter-webflux`（后者不提供 `WebClient.Builder`）。
- MyBatis-Plus 必须用 `mybatis-plus-spring-boot4-starter`，Boot 3 的 starter 不兼容。
- 本机 8080 端口常被其他 Java 进程占用，启动验证建议加 `--server.port=0`。
- `application.yml` 内含真实 MySQL 密码和 Dify API Key。仓库是私有仓库，**用户自己负责 commit / push，不要擅自执行 git 操作，也不要把密钥外泄**。

## 当前进度（2026-10-24）

- ✅ 模块 1：建表 SQL、`application.yml`、pom 依赖、`WebClientConfig`（120 秒超时）。
- ✅ 模块 2：`Resume` / `ResumeReport` 实体、`ResumeMapper` / `ResumeReportMapper`（`@Mapper` + `BaseMapper`）。
- ✅ 模块 3：`DifyService` / `DifyServiceImpl` / `DifyException`。已实测：文本调用返回 `{"status":"success", ...}`。
- ⏳ 模块 4：未开始。内容：`ResumeService`（`@Async asyncAnalyze(Long resumeId)`）、`ResumeController`（`POST /api/resume/analyze/{id}`）、启动类加 `@EnableAsync`。
- 用户流程：**一次只生成一个模块**；等用户回复“模块 N 完成，请发模块 N+1”后再继续，不要提前生成下一模块。

## Dify 集成

- 配置见 `application.yml` 的 `dify.api-url / api-key / user / timeout-seconds`。
- 工作流类型 advanced-chat，简历文本通过 `sys.query` 传入。Dify 端已改为在 LLM 系统提示词中使用 `{{#sys.query#}}`，代码节点会先剥离 `<think>` 再解析 JSON。
- DSL 文件：
  - 最新版：`D:\ai-job\pt\AI求职机.yml`
  - 旧版：`D:\ai-job\AI求职机.yml`
  - 用户后续提供的**截图和其他文件都放在 `D:\ai-job\pt`**。
- Dify 返回的 `answer` 是 JSON **字符串**（代码节点 `json.dumps`），结构：
  - `status`：`success` / `error`
  - `total_score`：总分
  - `radar_data`：5 个维度分数数组
  - `overall_evaluation`：综合点评数组
  - `full_detail`：完整明细
  后端原样返回该字符串，模块 4 存入 `resume_report.result`，前端自行 `JSON.parse`。
- 若返回 `status:error`，先排查 Dify 工作流（输入是否为空、模型输出是否被解析），不要先改 Java。

## 数据库要点

- `resume`：id、user_id（预留）、file_name、content(LONGTEXT)、create_time、update_time、deleted（逻辑删除）。
- `resume_report`：id、resume_id、result(LONGTEXT)、status（0 分析中 / 1 成功 / 2 失败）、error_msg、create_time、update_time、deleted。
- 时间字段由 MySQL 默认值维护（`CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP`），实体中留空即可。
- 逻辑删除已通过 yml 全局配置 + `@TableLogic` 生效。
- 本地 MySQL 客户端：`C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe`（凭据见 `application.yml`）。

## 常用命令

```bash
export JAVA_HOME="D:/java17/bellsoft-jdk17.0.15+10-windows-amd64-full/jdk-17.0.15-full"
MVN="C:/Users/柒玥/.m2/wrapper/dists/apache-maven-3.9.14-bin/1cb7fhup6b5n3bed6kckbrnspv/apache-maven-3.9.14/bin/mvn"

"$MVN" -B -DskipTests compile
"$MVN" -B -DskipTests package
"$MVN" -B -Dspring-boot.run.fork=false -Dspring-boot.run.arguments=--server.port=0 spring-boot:run
```

- 临时冒烟测试用 `@Transactional` 回滚，验证完删除临时测试文件，保持仓库干净。
- IDEA 报“不支持发行版本 5”时，先检查 Maven 是否导入成功、模块 Language Level 是否为 17。

## 代码约定

- 实体用 Lombok `@Data`；Mapper 继承 `BaseMapper` 并加 `@Mapper`。
- 分层：`service` 接口、`service.impl` 实现、`config` 配置、`exception` 自定义异常。
- 注释用中文，简洁说明意图；日志用 `@Slf4j`，**不要打印 API Key**。
- 新依赖优先选 Boot 4 对应 starter，并先确认 Maven Central 坐标可用。

## 下一步（模块 4）

1. `ResumeService` / `ResumeServiceImpl`：
   - `@Async void asyncAnalyze(Long resumeId)`；
   - 查 `resume` → 调 `difyService.chat(content)` → 写 `resume_report`；
   - 成功 `status=1`，失败 `status=2` 并写 `error_msg`。
2. `ResumeController`：`POST /api/resume/analyze/{id}`，调用异步方法后立即返回成功提示。
3. 启动类加 `@EnableAsync`（建议同时配置自定义线程池，避免使用默认无界队列）。
4. 完成后做端到端验证：插入测试简历 → 调接口 → 轮询/查询报告表。
