# 自动生成 Swagger 接口测试场景系统架构设计

## 1. 总体概览
- **项目形态**：基于 Maven 多模块结构，`auto-test-server` 为唯一启动入口，聚合业务模块形成单实例单体应用。
- **基础依赖**：自建 `auto-test-infra` 模块引入 `ruoyi-vue-pro` 的 `yudao-framework`、`yudao-module-system` 等基础能力，复用认证、权限、审计、配置、通知等组件，参考路径/Volumes/file/code/mine/ruoyi-vue-pro。
- **运行环境**：单实例部署、单库 MySQL 存储，Redis 作为缓存与简单队列。所有同步与执行均为手动触发。
- **扩展策略**：预留外部 Webhook、CI/CD、告警等扩展接口，事件驱动架构支持后续无侵入扩展。

## 2. Maven 模块划分
```
auto-test-backed (pom)
├── auto-test-server           # Spring Boot 启动模块
├── auto-test-infra            # 框架封装 & 公共能力
├── auto-test-module-swagger   # Swagger 源管理
│   ├── swagger-api
│   └── swagger-biz
├── auto-test-module-template  # curl 模板与随机策略
│   ├── template-api
│   └── template-biz
├── auto-test-module-scenario  # 场景编排
│   ├── scenario-api
│   └── scenario-biz
├── auto-test-module-executor  # 执行器
│   ├── executor-api
│   └── executor-biz
└── auto-test-module-diff      # 差异与复核
    ├── diff-api
    └── diff-biz
```
- `server` 仅聚合依赖与启动类，不承载业务逻辑。
- 各 `*-biz` 依赖 `infra` 与自身 `*-api`；跨模块调用通过 `api` 暴露的接口/DTO，避免循环依赖。
- 父 POM 统一管理依赖版本（JDK 17、Spring Boot 3.2.x、MyBatis-Plus 3.5.x 等）与构建插件。

## 3. 核心模块职责
### 3.1 auto-test-infra
- 引入 RuoYi 框架依赖，封装统一配置：数据源、MyBatis-Plus、Redis、异常、审计、权限、定时任务等。
- 提供基础抽象：`BaseDO`、`BaseMapper`、`BaseService`、`SequenceGenerator`、`EncryptField` 等。
- 管理系统配置（`SysConfig`）、字典（`SysDict`）、操作日志、通知通道、线程池配置。

### 3.2 Swagger 模块
- 维护 `autotest_project`、`autotest_collection`、`autotest_group`、`autotest_swagger_sync`。
- 提供 Swagger URL/文件导入、手动同步、OpenAPI 解析、差异计算、版本记录。
- 同步过程发布 `SwaggerSyncEvent` 供扩展监听。

### 3.3 模板模块
- 管理 `autotest_curl_variant`、`autotest_rule_audit`。
- 根据 Swagger 元数据生成最小/全量模板与随机策略，注册策略库 `RuleRegistry`。
- 支持手工编辑模板与规则，记录审计历史，提供预览与随机样例。

### 3.4 场景模块
- 维护 `autotest_scenario`、`autotest_scenario_step`、`autotest_scenario_version`、`autotest_project_env`。
- 负责场景建模、步骤拖拽、变量映射（JSONPath）、执行环境引用、场景版本管理。
- 场景保存/发布时发布 `ScenarioUpdatedEvent`。

### 3.5 执行器模块
- 管理 `autotest_execution`、`autotest_execution_detail`、`autotest_execution_log_{project}`。
- 手动触发场景执行：`ExecutionDispatcher` 校验配额 → 写入执行记录 → 提交线程池 → `StepRunner` 串行执行 → 结果/变量/日志落库。
- 同一场景互斥执行；执行完成/失败发布 `ScenarioExecutionEvent`，支持通知扩展。

### 3.6 差异模块
- 追踪 Swagger 重生成差异、模板变更影响、场景复核状态（`need_review`）。
- 负责差异快照展示、覆盖策略处理、复核待办推送；监听模板/同步事件触发标记。

## 4. 数据模型摘要
所有表继承 `BaseDO`（含 `id`, `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`）。

| 表名 | 关键字段 | 说明 |
| --- | --- | --- |
| `autotest_project` | `name`, `swagger_source`, `swagger_version`, `swagger_hash`, `status`, `remark` | 项目/Swagger 源信息 |
| `autotest_project_env` | `project_id`, `name`, `host`, `headers`, `is_default`, `status` | 项目级环境配置（headers 支持加密） |
| `autotest_collection` | `project_id`, `tag`, `summary` | Swagger Tag 维度 |
| `autotest_group` | `collection_id`, `method`, `path`, `summary`, `hash`, `status` | 单接口定义及哈希 |
| `autotest_curl_variant` | `group_id`, `type`, `curl_template`, `param_rules`, `rule_version`, `editor_id` | 模板及随机策略版本 |
| `autotest_rule_audit` | `variant_id`, `rule_snapshot`, `change_type`, `operator_id`, `change_reason` | 模板/规则变更审计 |
| `autotest_scenario` | `project_id`, `name`, `status`, `owner_id`, `tags`, `metadata`, `need_review` | 场景信息 |
| `autotest_scenario_step` | `scenario_id`, `order_no`, `curl_variant_id`, `variable_mapping`, `retry_policy`, `condition_config` | 场景步骤 |
| `autotest_scenario_version` | `scenario_id`, `version_no`, `content`, `comment`, `publisher_id` | 场景版本存档 |
| `autotest_execution` | `scenario_id`, `project_id`, `env_id`, `trigger_type`, `external_job_id`, `status`, `start_time`, `end_time`, `trigger_user` | 执行实例 |
| `autotest_execution_detail` | `execution_id`, `step_id`, `status`, `retry_count`, `request_snapshot`, `response_snapshot`, `variables_snapshot`, `error_message` | 步骤执行详情 |
| `autotest_swagger_sync` | `project_id`, `trigger_type`, `status`, `start_time`, `end_time`, `result_diff`, `operator_id` | Swagger 同步记录 |
| `autotest_execution_log_{project}` | `execution_id`, `step_id`, `timestamp`, `level`, `message`, `extra`, `notification_channel`, `notification_status` | 项目级分表日志 |

- 日志分表命名 `autotest_execution_log_${projectId}`；项目创建时通过初始化脚本生成分表。
- SQL 与迁移脚本存放于 `sql/autotest/`，由 Flyway/Liquibase 在启动时执行。

## 5. 并发与执行策略
- **线程池**：专用 `ThreadPoolTaskExecutor`，默认 `core=4`、`max=8`、`queue=100`，支持 `SysConfig` 动态调整。
- **场景互斥**：`ScenarioLockRegistry`（内存锁），确保同一场景串行执行，变量上下文无竞争。
- **配额控制**：提交执行时校验项目/租户并发上限（默认项目 2、租户 5），超出则返回排队提示。
- **队列处理**：线程池队列满时记录排队状态，提示用户稍后重试；也可选择持久化待执行队列以便人工干预。
- **状态缓存**: 执行状态实时写 DB 并同步 Redis，前端可秒级轮询；完成后触发站内信/邮件通知。

## 6. 配置样例
```yaml
server:
  port: 18080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/autotest?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
autotest:
  executor:
    core-size: 4
    max-size: 8
    queue-capacity: 100
    project-quota: 2
    tenant-quota: 5
    external-trigger-allowed: false
  integration:
    webhook-enabled: false
    notify-channels: []
    cicd-token: ""
logging:
  level:
    com.skyler.autotest: INFO
```
- 所有参数可通过 `SysConfig` 在线调整，支持热更新。

## 7. 事件与扩展接口
- **事件模型**：
  - `SwaggerSyncEvent{eventId, projectId, status, startTime, endTime, diffSummary}`
  - `ScenarioExecutionEvent{eventId, executionId, scenarioId, status, triggerType, duration, error}`
  - `TemplateUpdateEvent{variantId, changeType, operatorId, timestamp}`
- **事件总线**：使用 Spring `ApplicationEventPublisher` 简化；默认监听仅记录日志，未来可添加 Webhook/MQ/告警监听器。
- **Webhook 入口**：`/api/integration/hooks`（默认关闭），开启后需校验 `cicd-token`。示例请求：
```json
{
  "token": "xxx",
  "event": "SCENARIO_EXECUTE",
  "payload": {
    "scenarioId": 1001,
    "envId": 201,
    "remark": "pipeline #123"
  }
}
```
- **NotificationGateway**：抽象通知接口，默认实现调用站内信/邮件；后续可扩展钉钉、企业微信等通道。

## 8. 测试策略
- **单元测试**：JUnit5 + Mockito，覆盖领域服务、生成器、解析器、策略注册等。
- **集成测试**：Spring Boot Test + H2/Embedded Redis，覆盖 Swagger 同步、模板生成、场景执行主流程。
- **契约测试**：REST 接口使用 Spring REST Docs / OpenAPI 校验；事件监听器提供契约测试示例。
- **性能与稳定性**：JMH/Gatling 针对执行器线程池、模板生成做基准；场景执行压测模拟多并发手动触发。

## 9. 部署与初始化
- 单实例打包 `auto-test-server` 可执行 JAR；通过外部 `application-prod.yaml` 配置生产环境。
- 初始化流程：执行数据库迁移 → 创建管理员账号 → 导入默认线程池参数/随机策略/字典。
- 日志输出：使用 Spring 默认日志 + 业务事件日志；未来接入 ELK、SkyWalking 时在 `infra` 扩展配置即可。

## 10. 后续工作
1. 绘制 ER 图并确认字段约束、索引策略。
2. 完成 Swagger 同步、场景执行等核心接口契约文档（REST + 事件）。
3. 搭建 Maven 模块骨架与基础配置，初始化 Flyway/Liquibase 脚本。
4. 准备测试用 Swagger 样例与场景样例，为开发与评审提供数据支撑。
5. 迭代实现核心流程，边开发边完善测试与监控策略。
