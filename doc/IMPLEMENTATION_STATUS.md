# 文档功能覆盖检查报告

## 核心结论
- 代码已经打通 PRD 规划的主链路（Swagger 同步 → 模板生成 → 场景编排 → 执行日志 → 差异快照），但详细设计中关于随机策略审计、执行调度配额、通知审计等增强能力尚未落地，需按阶段补齐。

## 已实现的关键能力
- **Swagger 同步闭环**：满足 doc/DETAILED_DESIGN.md:70 关于同步、差异记录与事件发布的要求；手动触发同步、落库 diff 与发布事件的实现位于 auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/service/SwaggerProjectServiceImpl.java:130 以及对应的 REST 接口 auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/controller/SwaggerProjectController.java:24。
- **项目环境管理**：文档提出的项目级 Host/Header 维护（doc/PRD.md:41）已通过 ProjectEnvironmentController 与 Service 落地，核心代码见 auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/controller/ProjectEnvironmentController.java:24 与 auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/service/ProjectEnvironmentServiceImpl.java:50。
- **模板最小/全量生成与事件联动**：PRD 对 minimal/full 模板的诉求（doc/PRD.md:31）在 TemplateGenerationService 中实现，具体逻辑位于 auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateGenerationService.java:695 与 auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateGenerationService.java:720，并通过 SwaggerSyncEventListener 监听同步事件（auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/listener/SwaggerSyncEventListener.java:22）。
- **模板预览与复核联动**：TemplateServiceImpl 已提供参数规则渲染与预览能力，满足 doc/DETAILED_DESIGN.md:103 的预览需求，对应实现见 auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateServiceImpl.java:87，且模板更新会广播 TemplateUpdateEvent 供场景与差异模块消费（auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateGenerationService.java:186）。
- **场景建模、变量校验与版本存档**：场景模块履行 doc/PRD.md:95 的编排与版本化要求，创建/更新/发布逻辑及 need_review 判定位于 auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/service/ScenarioServiceImpl.java:72、auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/service/ScenarioServiceImpl.java:221 与场景 REST 接口 auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/controller/ScenarioController.java:24。
- **执行器串行执行、变量传递与日志分表**：doc/DETAILED_DESIGN.md:184 所述的串行执行与 JSONPath 提取通过 ExecutionServiceImpl、StepRunner 与 VariableContext 落地，核心代码见 auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/service/ExecutionServiceImpl.java:80、auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/runner/StepRunner.java:54，以及分表日志实现 auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/service/impl/ExecutionLogServiceImpl.java:76。
- **差异捕获与复核接口**：满足 doc/DETAILED_DESIGN.md:249 关于差异快照与复核的需求，监听器与复核服务分别位于 auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/listener/SwaggerSyncDiffListener.java:23、auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/listener/TemplateUpdateDiffListener.java:45 与 auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/service/DiffServiceImpl.java:25；REST 接口见 auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/controller/DiffController.java:24。
- **基础设施封装**：与 doc/ARCHITECTURE.md:87 对执行线程池的描述一致，ThreadPoolConfig 和 RestTemplateConfig 等基础设施位于 auto-test-infra/src/main/java/com/example/autotest/infra/config/ThreadPoolConfig.java:20 与 auto-test-infra/src/main/java/com/example/autotest/infra/config/RestTemplateConfig.java:19。

## 部分实现但存在差距的功能
- **Swagger 差异明细**：文档要求统计 `diff_summary.invalidNodes`（doc/DETAILED_DESIGN.md:82），当前同步逻辑仅输出 added/updated/removed，未记录异常字段，影响非规范字段追踪。
- **随机策略库与审计链路**：doc/DETAILED_DESIGN.md:93-116 规划了 `RuleRegistry`、`RuleOverrideService` 与 `autotest_rule_audit`，现有实现仅生成样例值（TemplateGenerationService）且无审计记录或自定义策略留痕。
- **场景事件与跨模块联动**：架构设计要求发布 `ScenarioUpdatedEvent`（doc/ARCHITECTURE.md:53），目前 publish 流程仅写入版本表（auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/service/ScenarioServiceImpl.java:124），未向差异或通知模块广播，导致复核链路需要手动刷新。
- **执行调度与配额控制**：doc/DETAILED_DESIGN.md:192-243 规划 `ExecutionDispatcher`、项目/租户配额及取消接口。现阶段 trigger 方法直接在事务中同步执行（auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/service/ExecutionServiceImpl.java:80），未校验配额，也缺少 `/api/execution/{id}/cancel`。
- **差异通知闭环**：详细设计中的 `DiffNotificationService` 与 need_review 回写（doc/DETAILED_DESIGN.md:257）尚未实现，DiffServiceImpl 只更新差异自身状态，无法自动清理模板/场景的复核标记或推送提醒。
- **配置中心与参数化**：文档期望通过 SysConfig 动态调整线程池与配额（doc/ARCHITECTURE.md:86），当前仅有本地 `autotest.executor.*` 配置（ThreadPoolProperties），缺少 SysConfig 绑定与运行时调整能力。

## 尚未覆盖的需求
- **模板与规则审计表**：`autotest_rule_audit` 在 doc/schema.sql:82 定义，但仓库缺少对应 DO、Mapper 与 Service，手工编辑记录无法追溯。
- **通知/集成通道**：文档规划的 `NotificationGateway`、Webhook 入口与差异/执行告警（doc/ARCHITECTURE.md:96、doc/DETAILED_DESIGN.md:230）未见实现，相关事件目前只在日志中留痕。
- **执行取消与配额脚本**：PRD 中的取消接口与运维脚本（doc/DETAILED_DESIGN.md:233、doc/DETAILED_DESIGN.md:275）尚未提供，`scripts/` 目录不存在，对应初始化脚本缺失。
- **操作审计与权限配置**：需求强调复用 RuoYi 审计/权限能力（doc/PRD.md:123、doc/ARCHITECTURE.md:45），当前业务层尚未接入 `AuditLogService` 或细粒度权限校验，角色隔离需额外规划。

## 后续建议
- 依据上述差距补全随机策略审计、执行调度、通知告警等能力，并同步更新 Flyway 脚本与 `doc/DETAILED_DESIGN.md` 的状态说明，避免需求与实现再次偏离。
