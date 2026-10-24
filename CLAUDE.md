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
- 本机 8080 端口常被其他 Java 进程占用：单独验证接口可加 `--server.port=0`；**前后端联调必须用 8080**（前端默认指向它，或同步改 `utils/request.js` 的 BASE_URL）。
- `application.yml` 内含真实 MySQL 密码和 Dify API Key。仓库是私有仓库，**用户自己负责 commit / push，不要擅自执行 git 操作，也不要把密钥外泄**。

## 当前进度（2026-10-25）

- ✅ 模块 1：建表 SQL、`application.yml`、pom 依赖、`WebClientConfig`（120 秒超时）。
- ✅ 模块 2：`Resume` / `ResumeReport` 实体、`ResumeMapper` / `ResumeReportMapper`（`@Mapper` + `BaseMapper`）。
- ✅ 模块 3：`DifyService` / `DifyServiceImpl` / `DifyException`。已实测：文本调用返回 `{"status":"success", ...}`。
- ✅ 模块 4：`ResumeService` / `ResumeServiceImpl`（`@Async("analysisExecutor") asyncAnalyze`）、`ResumeController`（`POST /api/resume/analyze/{id}`）、`AsyncConfig` 线程池、启动类 `@EnableAsync`。已实测：接口立即返回，报告先落 status=0，分析完成后更新为 status=1；简历不存在时 Controller 同步校验返回 400，不进入异步流程。
- ✅ 模块 5：前后端对接完成。后端新增 CORS、简历上传解析（PDFBox 3.0.8 + POI 5.5.1，支持 PDF/Word/TXT）、简历列表、报告查询；前端新增 `utils/request.js`，上传页走真实上传+分析，报告页每 2 秒轮询并渲染。已用 Edge（Playwright）实测：上传 txt → 分析 → 轮询 → 报告渲染，总分 77、4 条评价、5 个维度、雷达图正常。
- ✅ 模块 6：分析历史。后端 `GET /api/resume/history`（resume + resume_report 关联，每份简历取最新报告，总分用 MySQL `JSON_EXTRACT` 从 result 提取，最多 10 条）；前端 `history.vue` 去掉静态数据，`onShow` 加载真实数据，按状态 0/1/2 显示蓝/绿/红标签，点击带 `resumeId` 进报告页。浏览器实测 4 条不同状态记录与跳转均正常。用户已补 `static/xiazai.png`，报告页 404 已消除。
- 🔄 模块 7（求职模块，分 3 步）：
  - ✅ 步骤 1：`sql/job_position.sql` 建表 + 10 条种子数据（可重复执行）、`JobPosition` / `JobPositionMapper` / `JobService` / `JobController`、`GET /api/job/list`（实测返回 10 条）。
  - ✅ 步骤 2：`job.vue` 去掉静态数据，`onShow` 调 `GET /api/job/list`；后端 `companySize` 映射为 `size`，`tags` 逗号字符串拆成数组；加载中/失败重试/空状态；关键词搜索仍在前端对已加载数据做模糊匹配。浏览器实测 10 条卡片、搜索“奇安信”命中 1 条、点击进入详情正常。
  - ✅ 步骤 3：后端 `JobQuery` + `JobPositionMapper.xml` 动态 SQL（keyword/location/education/experience/size/financing/salaryMin/salaryMax，全部可选）；关键词匹配职位名/公司/地点/标签，薪资按区间 overlap，经验和规模比较前去掉空格。前端关键词 300ms 防抖、快捷标签、区域选择和筛选页事件全部联动，薪资标签映射为数值区间，请求序号防止竞态。浏览器实测：搜索“前端”2 条、标签“测试”2 条、朝阳区 3 条、大专 1 条、本科+3-5年 3 条、50K以上 3 条，均与接口一致。
  - ✅ 步骤 3 补充修复（用户反馈「学历都显示本科及以上」）：列表卡片增加 `学历 · 经验` 展示；`jobDetail.vue` 通过 query 接收 `education/experience/companySize/tags`，移除硬编码的「本科及以上 / 3-5年 / 1000-9999人」，任职要求第一行改为按真实学历/经验生成，技能标签用职位 tags；查询进行中禁止点击卡片，避免点进与筛选条件不符的职位。已在 5173+8080 实测：大专筛选 → 金山测试开发 → 详情显示「1-3 年 / 大专 / 500-999人 / 自动化测试…」。
- 模块 7 的 3 个步骤全部完成。
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
- `GET /api/job/list`：职位列表，按 id 升序。支持可选查询参数（模块 7 步骤 3）：
  - `keyword`：模糊匹配 name/company/location/tags
  - `location`：前缀匹配，如 `北京`、`北京·海淀`
  - `education`、`experience`、`size`、`financing`：精确匹配（experience/size 比较前忽略空格）
  - `salaryMin` / `salaryMax`：月薪区间（K），与职位薪资区间有交集即命中；可只传一侧
  - 全部不传返回全部；`tags` 是逗号分隔字符串，前端需 `split(',')`。
- 统一错误体：`{"message": "..."}`（`GlobalExceptionHandler`）。
- CORS：`CorsConfig` 对 `/api/**` 放开所有来源（未开 credentials），开发演示用，上线需收紧。
- 上传大小限制：`spring.servlet.multipart.max-file-size=10MB / max-request-size=12MB`。

## 数据库要点

- `resume`：id、user_id（预留）、file_name、content(LONGTEXT)、create_time、update_time、deleted（逻辑删除）。
- `resume_report`：id、resume_id、result(LONGTEXT)、status（0 分析中 / 1 成功 / 2 失败）、error_msg、create_time、update_time、deleted。
- 时间字段由 MySQL 默认值维护（`CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP`），实体中留空即可。
- 逻辑删除已通过 yml 全局配置 + `@TableLogic` 生效。
- `job_position`（模块 7）：id、name、salary（展示文案）、salary_min/salary_max（月薪区间，单位 K，供步骤 3 筛选）、company、company_size、tags（逗号分隔）、location、education、experience、financing（融资阶段，供步骤 3 筛选）、create_time。DDL + 10 条种子数据在 `sql/job_position.sql`，可重复执行（`INSERT IGNORE`）。
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
- 当前依赖已安装（`node_modules/` 已存在），H5 已构建过（`dist/build/h5`）；改前端代码后需重新运行/构建。
- 后端地址集中在 `utils/request.js`：默认 `http://localhost:8080`，可用 `VITE_API_BASE_URL` 覆盖。
- IDEA 同时查看前后端（推荐）：打开后端项目，`File → Attach to Project…`（旧版是 `File → Open` 选前端目录后点 **Attach**）。不要选 **This Window**（会替换当前项目），不要把前端导入为 Maven 模块。附加后把 `node_modules`、`dist`、`unpackage` 标为 Excluded，避免索引卡顿。后端 `.idea`、Maven、运行配置不受影响。
- 页面流程：`upload.vue` 上传后调 `POST /api/resume/analyze/{id}` 并带 `resumeId` 跳转；`report.vue` 每 2 秒轮询报告接口，处理 0（继续等）/1（渲染）/2（错误），3 分钟超时。
- `history.vue` 在 `onShow` 调 `GET /api/resume/history`，点击记录带 `resumeId` 进报告页；报告页仍保留无 `resumeId` 时的静态样例兜底。
- `job.vue`：`onShow` 按当前条件查询 `GET /api/job/list`；`companySize→size`、`tags` 字符串按逗号拆分。关键词输入 300ms 防抖、快捷标签（点第二次取消）、区域页和筛选页通过 `uni.$emit` 回传条件；薪资标签在 `SALARY_RANGE_MAP` 中映射为 `salaryMin/salaryMax`；用请求序号丢弃过期响应。
- `area.vue` 现在 emit `{ label, location }`：区县会转成职位数据的 `城市·区域` 格式（海淀区→北京·海淀，浦东新区→上海·浦东），整市转成 `北京`，不限则 `location=''`。
- `jobDetail.vue` 的职位基本信息全部来自列表页 query（含 `education/experience/companySize/tags`），不要再写死学历/经验/规模；列表卡片的 `学历 · 经验` 也来自接口字段。

## 本地联调启动（自测用）

前置：MySQL 已启动；本地 Dify 可访问且工作流正常。

1. 启动后端（端口 8080，前端默认指向这里）
   - IDEA 直接运行 `AiJobBackendApplication`；或命令行：
     ```bash
     export JAVA_HOME="D:/java17/bellsoft-jdk17.0.15+10-windows-amd64-full/jdk-17.0.15-full"
     MVN="C:/Users/柒玥/.m2/wrapper/dists/apache-maven-3.9.14-bin/1cb7fhup6b5n3bed6kckbrnspv/apache-maven-3.9.14/bin/mvn"
     cd D:/ai-job/ai-job-backend && "$MVN" -B -Dspring-boot.run.fork=false spring-boot:run
     ```
   - 启动自检：`curl http://localhost:8080/api/resume/history` 返回 JSON 数组，说明后端和数据库都正常。
   - 改了后端代码必须重启；8080 被占用时先结束旧进程：`netstat -ano | grep :8080`。
2. 启动前端（二选一）
   - HBuilderX：打开 `D:\ai-job\ai-job-frontend` → 运行 → 运行到浏览器。
   - 命令行（已验证）：`cd D:/ai-job/ai-job-frontend && UNI_INPUT_DIR=D:/ai-job/ai-job-frontend npx uni -p h5`，打开 http://localhost:5173 。
3. 自测流程
   - 上传页选择 PDF/Word/TXT（≤10MB）→ 上传成功后新简历在列表第一项并选中 → 点「AI 简历分析」。
   - 报告页先显示「AI 正在分析您的简历」，约 10–30 秒后渲染总分、总体评价、5 个维度和雷达图。
   - 返回后进「分析历史」，`onShow` 会刷新真实记录（状态 0 蓝 / 1 绿 / 2 红）；点击记录按 `resumeId` 打开报告。
4. 常见问题
   - 前端提示「无法连接后端服务」：后端没启动、不在 8080，或端口被别的程序占用。
   - 报告一直「分析中」：看后端日志里的 Dify 调用；直接查 `resume_report` 最新一行的 `status/error_msg`。
   - 上传返回 400：格式不支持/超过 10MB/文件损坏，看响应里的 `message`。
   - 改前端代码后页面没变化：HBuilderX 需重新运行，命令行需重启 dev server 或重新 build。
5. 当前演示数据：`resume` id=6（李四-Java工程师.txt，报告 77 分）、id=2（模块4测试-张三.txt，66 分）。

## 代码约定

- 实体用 Lombok `@Data`；Mapper 继承 `BaseMapper` 并加 `@Mapper`。
- 分层：`service` 接口、`service.impl` 实现、`config` 配置、`exception` 自定义异常。
- 注释用中文，简洁说明意图；日志用 `@Slf4j`，**不要打印 API Key**。
- 新依赖优先选 Boot 4 对应 starter，并先确认 Maven Central 坐标可用。

## 下一步

- 模块 7（求职模块）步骤 1–3 全部完成并实测通过。等用户指定模块 8 后再继续，不要提前生成。
- 注意：8080 上的后端进程如果是步骤 2 之前启动的，不会识别筛选参数（会忽略参数返回全部），需要重启后端；前端 5173 由 HBuilderX 热更新，无需重新构建。步骤 3 的浏览器验证临时在 8081 + 5174 完成。
- 后续可做的方向（需用户确认）：职位详情页接真实数据、职位分页/加载更多、用户体系（`user_id` 目前预留）、简历历史分页/筛选、报告页按历史记录打开指定某次报告、部署与 CORS 收紧。
