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

## 当前进度（2026-10-25）

- ✅ 模块 1：建表 SQL、`application.yml`、pom 依赖、`WebClientConfig`（120 秒超时）。
- ✅ 模块 2：`Resume` / `ResumeReport` 实体、`ResumeMapper` / `ResumeReportMapper`（`@Mapper` + `BaseMapper`）。
- ✅ 模块 3：`DifyService` / `DifyServiceImpl` / `DifyException`。已实测：文本调用返回 `{"status":"success", ...}`。
- ✅ 模块 4：`ResumeService` / `ResumeServiceImpl`（`@Async("analysisExecutor") asyncAnalyze`）、`ResumeController`（`POST /api/resume/analyze/{id}`）、`AsyncConfig` 线程池、启动类 `@EnableAsync`。已实测：接口立即返回，报告先落 status=0，分析完成后更新为 status=1；简历不存在时 Controller 同步校验返回 400，不进入异步流程。
- ✅ 模块 5：前后端对接完成。后端新增 CORS、简历上传解析（PDFBox 3.0.8 + POI 5.5.1，支持 PDF/Word/TXT）、简历列表、报告查询；前端新增 `utils/request.js`，上传页走真实上传+分析，报告页每 2 秒轮询并渲染。已用 Edge（Playwright）实测：上传 txt → 分析 → 轮询 → 报告渲染，总分 77、4 条评价、5 个维度、雷达图正常。
- ✅ 模块 6：分析历史。后端 `GET /api/resume/history`（resume + resume_report 关联，每份简历取最新报告，总分用 MySQL `JSON_EXTRACT` 从 result 提取，最多 10 条）；前端 `history.vue` 去掉静态数据，`onShow` 加载真实数据，按状态 0/1/2 显示蓝/绿/红标签，点击带 `resumeId` 进报告页。浏览器实测 4 条不同状态记录与跳转均正常。用户已补 `static/xiazai.png`，报告页 404 已消除。
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
  后端原样返回该字符串并存 `resume_report.result`，前端从 `GET /api/resume/report/{resumeId}` 取到后自行 `JSON.parse`。
- 若返回 `status:error`，先排查 Dify 工作流（输入是否为空、模型输出是否被解析），不要先改 Java。

## 后端接口清单

- `POST /api/resume/upload`：multipart（字段名 `file`），解析 PDF/Word/TXT 后入库，返回 `{id, fileName, createTime}`；格式不支持或解析失败返回 400。
- `GET /api/resume/list`：最近 100 份简历摘要（不含 content），按 id 倒序。
- `GET /api/resume/history`：最近 10 份简历的分析历史；每份简历只取最新一次报告（否则按 resumeId 跳转报告页会与列表行不一致）。返回 `{reportId, resumeId, fileName, uploadTime, status, totalScore}`，totalScore 仅 status=1 且 result 为合法 JSON 时有值。
- `POST /api/resume/analyze/{id}`：提交异步分析；简历不存在返回 400。
- `GET /api/resume/report/{resumeId}`：最新一份报告 `{reportId, resumeId, status, result, errorMsg, createTime}`；尚未生成返回 404（前端轮询据此继续等待）。
- 统一错误体：`{"message": "..."}`（`GlobalExceptionHandler`）。
- CORS：`CorsConfig` 对 `/api/**` 放开所有来源（未开 credentials），开发演示用，上线需收紧。
- 上传大小限制：`spring.servlet.multipart.max-file-size=10MB / max-request-size=12MB`。

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

## 前端联调（`D:\ai-job\ai-job-frontend`）

- 技术栈：uni-app（Vue 3 + Vite）H5，目录是 HBuilderX 结构（没有 `src/`）。
- 用命令行构建必须指定输入目录，否则报找不到 `src/manifest.json`：
  `UNI_INPUT_DIR=D:/ai-job/ai-job-frontend npm run build:h5`，产物在 `dist/build/h5`。
- 本机 npm 全局缓存 `D:\nodejs\node_cache` 会报 EPERM，安装依赖加本地缓存：
  `npm install --cache D:/ai-job/.npm-cache`
- 后端地址集中在 `utils/request.js`：默认 `http://localhost:8080`，可用 `VITE_API_BASE_URL` 覆盖。
- 页面流程：`upload.vue` 上传后调 `POST /api/resume/analyze/{id}` 并带 `resumeId` 跳转；`report.vue` 每 2 秒轮询报告接口，处理 0（继续等）/1（渲染）/2（错误），3 分钟超时。
- `history.vue` 在 `onShow` 调 `GET /api/resume/history`，点击记录带 `resumeId` 进报告页；报告页仍保留无 `resumeId` 时的静态样例兜底。

## 代码约定

- 实体用 Lombok `@Data`；Mapper 继承 `BaseMapper` 并加 `@Mapper`。
- 分层：`service` 接口、`service.impl` 实现、`config` 配置、`exception` 自定义异常。
- 注释用中文，简洁说明意图；日志用 `@Slf4j`，**不要打印 API Key**。
- 新依赖优先选 Boot 4 对应 starter，并先确认 Maven Central 坐标可用。

## 下一步

- 模块 6 已完成并浏览器实测通过。等用户指定模块 7 后再继续，不要提前生成。
- 后续可做的方向（需用户确认）：用户体系（`user_id` 目前预留）、历史记录分页/筛选（现在固定 10 条且筛选按钮还是弹提示）、报告页按历史记录打开指定某次报告（目前只展示最新一次）、部署与 CORS 收紧。
