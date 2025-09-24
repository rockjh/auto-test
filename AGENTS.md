# Codex Agent 指南

## 基础沟通约定
- **必须使用中文进行回复**，语气保持专业、简洁、可执行。
- 如信息不足，明确指出缺口，并提出需要用户补充的最小关键信息，而不是盲目假设。
- 回答时优先结合项目文档（`doc/PRD.md`、`doc/ARCHITECTURE.md`、`doc/DETAILED_DESIGN.md`、`doc/schema.sql`），必要时引用对应模块或表名帮助定位。

## 编码规范提醒
- 所有新老代码需补充必要注释，尤其是核心业务链路、易混淆的分支与参数处理，确保研发与 QA 均能快速理解意图。
- 任意接口（controller、内部 API、事件监听暴露的回调等）都必须按照标准 Swagger 注解格式维护，字段描述、示例值、响应结构需与实际实现保持一致，方便自动生成 `curl` 模板与差异复核。

## 项目速览
- 项目名称：自动生成 Swagger 接口测试场景系统。
- 目标：解析 Swagger/OpenAPI，自动生成可维护的 `curl` 模板，支持场景编排、变量映射、自研执行器执行与差异复核，提效 QA 的接口测试。
- 技术栈：Maven 多模块、Spring Boot 3.2.x、MyBatis-Plus、Redis；复用 RuoYi `yudao-framework` 的账号、权限、审计等能力。
- 运行形态：单体应用（`auto-test-server`），单库 MySQL + Redis；所有同步、执行目前均为手动触发。
- 依赖管理：新增外部能力时 **必须先在 `auto-test-infra` 中引入 RuoYi 对应功能模块**，业务模块统一只依赖 `auto-test-infra`；如需参考原始实现，请查看 `ruoyi-vue-pro`（目录 `/Volumes/file/code/mine/ruoyi-vue-pro`）。

## 模块职责速查
- `auto-test-infra`：封装数据源、MyBatis、Redis、线程池、基础领域抽象（`BaseDO`、`BaseMapperX`）、统一异常与审计。
- `auto-test-module-swagger`：管理 Swagger 项目、Tag、接口；负责同步、差异计算，落库 `autotest_project` / `collection` / `group` / `swagger_sync`。
- `auto-test-module-template`：生成并维护 `curl` 模板与随机策略；核心表 `autotest_curl_variant`、`autotest_rule_audit`；暴露预览与审计 API。
- `auto-test-module-scenario`：场景建模、步骤拖拽、变量池、版本管理；关注 `autotest_scenario`、`scenario_step`、`scenario_version`、`scenario_variable`、`project_env`。
- `auto-test-module-executor`：执行场景、串行步骤、记录日志；使用 `ExecutionDispatcher`、`StepRunner`，写入 `autotest_execution`、`execution_detail`、`execution_log_{project}`。
- `auto-test-module-diff`：监听同步/模板事件生成差异快照，维护复核流程，落库 `autotest_diff_snapshot`。
- `auto-test-server`：聚合所有模块，仅包含启动类和配置入口。

## 核心业务流程
1. **Swagger 同步**：手动触发 → 下载解析 → 计算差异 → 增量更新数据库 → 标记受影响模板/场景需复核 → 发布 `SwaggerSyncEvent`。
2. **模板生成与维护**：读取接口元数据 → 生成最小/全量 `curl` → 应用随机策略库 → 写入/更新 `autotest_curl_variant` → 审计手工调整。
3. **场景编排**：选择项目/模板 → 配置步骤顺序、JSONPath 变量映射、重试策略 → 保存草稿 → 发布生成版本快照。
4. **执行器运行**：触发执行（手动/Webhook 预留）→ 校验互斥与配额 → 线程池串行执行步骤 → 写 detail/log → 发送通知。
5. **差异复核**：同步或模板变更触发 diff → 写入 `autotest_diff_snapshot` → 待办提醒 → 复核通过后清除 `need_review` 标记。

## 数据模型提示
- 所有主表继承 `BaseDO` 公共字段（`tenant_id`、`create_time`、`deleted` 等）。
- 日志按项目分表：创建项目时需复制 `autotest_execution_log_template` 为 `autotest_execution_log_{projectId}`。
- 关键字段：
  - `autotest_group.hash`：用于识别 Swagger 接口差异。
  - `autotest_curl_variant.variant_type`（minimal/full/custom）、`need_review` 标记模板复核状态。
  - `autotest_scenario.need_review`：场景是否需人工检查；`default_env_id` 关联项目环境。
  - `autotest_execution.status`：0 排队 / 1 执行中 / 2 成功 / 3 失败 / 4 取消。

## 接口与配置关注点
- 主要 REST 前缀：`/api/swagger/*`、`/api/template/*`、`/api/scenario/*`、`/api/execution/*`、`/api/diff/*`。
- 线程池和配额参数放在 `SysConfig`，默认 `core=4`, `max=8`, `project-quota=2`, `tenant-quota=5`。
- 事件：`SwaggerSyncEvent`、`TemplateUpdateEvent`、`ScenarioExecutionEvent`，默认使用 Spring `ApplicationEventPublisher`，可拓展 Webhook/通知。

## 常见实现问题自检
- **Swagger 非规范字段**：解析异常需写入 `diff_summary.invalidNodes` 并提示手工补充。
- **随机策略缺口**：回退默认值但要记录 warning，建议补充策略库或提供手工编辑入口。
- **场景复用**：保存时需校验 `curl_variant` 是否 `need_review`，若是则场景同步标记。
- **执行器日志**：分表不存在时应自动建表且做重试/告警，避免执行失败。
- **互斥与配额**：`ScenarioLockRegistry` 限制同一场景并发，Dispatcher 检查项目/租户配额。

## 向用户提问指引
- 功能不明时，优先确认所处模块（Swagger/模板/场景/执行/差异）与目标场景。
- 涉及数据持久化需求时，请求用户提供字段、约束、涉及表，并提醒同步更新 `schema.sql` 与 Flyway 脚本。
- 遇到执行链路问题时，询问是否有现成场景样例、执行日志或配额配置，以便定位。
- 若用户要求新增策略/模板，请确认接口示例、期望随机规则及是否需审计记录。

## 交付与验证建议
- 代码改动完成后，建议补充/更新相关单元或集成测试（JUnit5 + Mockito + Spring Boot Test）。
- 需要运行脚本或初始化数据时，提示用户使用 `scripts/` 下工具并说明用途。
- 若无法在本地执行测试或构建，请明确说明限制，并给出用户可自行验证的步骤。

> 阅读上述要点，可快速理解项目背景、模块划分及常见关注点，确保每次对话都聚焦在具体需求与上下文。
