# 当前功能实现状态

本报告基于 `doc/PRD.md` 与 `doc/DETAILED_DESIGN.md` 的需求，对比仓库内源码的实现情况。

## 已落地模块/能力
- **Swagger 项目管理**：提供项目创建、列表、详情、同步及差异查询；同步流程会解析本地 Swagger 文档、更新 `autotest_project` / `autotest_collection` / `autotest_group`，并触发 `SwaggerSyncEvent`（`auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/service/SwaggerProjectServiceImpl.java`, `auto-test-module-swagger/swagger-biz/src/main/java/com/example/autotest/swagger/controller/SwaggerProjectController.java`）。
- **模板生成与查询**：监听同步事件，为新增/更新接口生成 `minimal` 模板，并提供模板列表、重生成、预览接口，同时在模板变更时发布 `TemplateUpdateEvent`（`auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/listener/SwaggerSyncEventListener.java`, `auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateGenerationService.java`, `auto-test-module-template/template-biz/src/main/java/com/example/autotest/template/service/TemplateServiceImpl.java`）。
- **场景管理基础**：支持场景创建/更新/发布、按项目查询及步骤保存；模板发生变更时可标记相关场景需复核（`auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/service/ScenarioServiceImpl.java`, `auto-test-module-scenario/scenario-biz/src/main/java/com/example/autotest/scenario/listener/TemplateUpdateScenarioListener.java`）。
- **执行器雏形**：实现串行执行流程，基于模板预览生成请求快照、写入执行/步骤记录，并在结束时发布 `ScenarioExecutionEvent`（`auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/service/ExecutionServiceImpl.java`, `auto-test-module-executor/executor-biz/src/main/java/com/example/autotest/executor/controller/ExecutionController.java`）。
- **差异快照与复核接口**：监听同步与模板事件生成差异快照，提供待复核列表与复核操作（`auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/listener/SwaggerSyncDiffListener.java`, `auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/listener/TemplateUpdateDiffListener.java`, `auto-test-module-diff/diff-biz/src/main/java/com/example/autotest/diff/service/DiffServiceImpl.java`）。
- **基础设施**：完成数据源、MyBatis-Plus、线程池、Redis、统一异常处理、通用响应、`BizIdGenerator` 等通用能力（`auto-test-infra/src/main/java/com/example/autotest/infra/config/DataSourceConfig.java`, `auto-test-infra/src/main/java/com/example/autotest/infra/config/ThreadPoolConfig.java`, `auto-test-infra/src/main/java/com/example/autotest/infra/core/web/GlobalExceptionHandler.java`）。

## 主要缺口
- **Swagger 环境与远程同步**：需求要求维护项目环境（Host/Header/变量）并支持 URL 同步（`doc/PRD.md` 第 5 节），当前只有 DO 定义，缺少 Service/Controller；同步实现仅允许读取本地文件，并显式禁用网络抓取。
- **模板策略与审计体系**：PRD 要求最小/全量/自定义模板、随机策略库、手工校正与审计（`doc/PRD.md` 第 2 节、`doc/DETAILED_DESIGN.md` 第 3 节）。现阶段仅生成路径参数样例，无 `RuleRegistry`、`RuleAudit`、自定义变体、need_review 生命周期等能力。
- **场景变量与高级编排**：文档强调变量池、JSONPath 映射、重试/条件配置（`doc/PRD.md` 第 5 节、`doc/DETAILED_DESIGN.md` 第 4 节），但当前实现尚未落地变量表读写、JSONPath 校验、重试策略或 default 环境校验，也未与 need_review 流程联动。
- **执行器实际能力**：需求包含真实 HTTP 调用、随机参数/变量替换、重试、互斥、日志分表、告警与取消等（`doc/PRD.md` 第 6 节、`doc/DETAILED_DESIGN.md` 第 5 节）。目前执行逻辑仅构造模拟响应，缺少线程池调度、并发配额、请求发送、日志与通知。
- **差异闭环与通知**：虽然生成了差异快照，但未集成复核任务、need_review 回写与告警推送，UI/权限/审计链路也尚未建设。
- **RuoYi 权限与配置复用**：详细设计要求复用 RuoYi 登录、权限、配置、审计（`doc/DETAILED_DESIGN.md` 第 1 节），现在项目仍是裸 Spring Boot，未接入认证、菜单、操作日志、配置中心。

## 建议的后续迭代顺序
1. **完善 Swagger 项目与环境管理**：实现环境 CRUD、同步操作人、允许受控的 URL 拉取，并完善差异信息结构。
2. **扩展模板模块**：补齐随机策略库、模板审计、need_review 生命周期、多模板类型及 Header/Body 变量处理。
3. **强化场景编排**：落地变量模型与 JSONPath 校验、重试与条件配置、need_review 联动，发布时写入版本/变量信息。
4. **落地执行器核心能力**：实现真实 HTTP 调用、变量传递、重试与超时、执行互斥、日志分表及通知通道，完善线程池/配额配置。
5. **搭建权限与横切能力**：接入 RuoYi 权限体系、配置中心、操作日志，并补充事件驱动的复核/告警闭环。
6. **补充测试与监控**：覆盖单元/集成测试、引入性能指标与告警策略，保证核心链路稳定。

以上内容将随功能推进持续更新。
