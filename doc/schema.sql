-- Auto-generated weakly constrained schema for auto-test system
-- All tables inherit BaseDO fields via explicit columns; no foreign keys.

CREATE TABLE autotest_project (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(120)    NOT NULL,
  swagger_source   VARCHAR(512)    NOT NULL COMMENT 'URL 或文件路径',
  swagger_type     TINYINT         NOT NULL COMMENT '1=URL,2=文件,3=离线',
  swagger_version  VARCHAR(64)     DEFAULT NULL,
  swagger_hash     CHAR(64)        DEFAULT NULL,
  sync_status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=待同步,1=成功,2=失败',
  sync_time        DATETIME        DEFAULT NULL,
  tags             JSON            DEFAULT NULL,
  status           TINYINT         NOT NULL DEFAULT 1,
  remark           VARCHAR(255)    DEFAULT NULL,
  extra            JSON            DEFAULT NULL COMMENT '额外配置/扩展字段',
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='Swagger 项目主表';

ALTER TABLE autotest_project
  ADD KEY idx_project_tenant_status (tenant_id, status);

CREATE TABLE autotest_project_env (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(80)     NOT NULL,
  env_type         TINYINT         NOT NULL DEFAULT 1 COMMENT '1=手工,2=继承',
  host             VARCHAR(255)    NOT NULL,
  headers          JSON            DEFAULT NULL COMMENT '公共 Header，敏感字段加密',
  variables        JSON            DEFAULT NULL COMMENT '环境级变量占位',
  is_default       BIT(1)          NOT NULL DEFAULT 0,
  status           TINYINT         NOT NULL DEFAULT 1,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='项目环境配置';

ALTER TABLE autotest_project_env
  ADD KEY idx_env_project (project_id);

CREATE TABLE autotest_swagger_sync (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  trigger_type     TINYINT         NOT NULL COMMENT '1=手动,2=定时,3=变更通知',
  status           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=进行中,1=成功,2=失败',
  start_time       DATETIME        NOT NULL,
  end_time         DATETIME        DEFAULT NULL,
  diff_summary     JSON            DEFAULT NULL COMMENT '新增/更新/删除概要',
  error_message    VARCHAR(1000)   DEFAULT NULL,
  operator_id      BIGINT UNSIGNED DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='Swagger 同步记录';

ALTER TABLE autotest_swagger_sync
  ADD KEY idx_swagger_sync_project (project_id, start_time);

CREATE TABLE autotest_collection (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  tag              VARCHAR(120)    NOT NULL,
  summary          VARCHAR(255)    DEFAULT NULL,
  order_no         INT             DEFAULT NULL,
  status           TINYINT         NOT NULL DEFAULT 1,
  metadata         JSON            DEFAULT NULL COMMENT 'Tag 级额外信息',
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='Swagger Tag 维度';

ALTER TABLE autotest_collection
  ADD KEY idx_collection_project (project_id, status);

CREATE TABLE autotest_group (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  collection_id    BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  method           VARCHAR(12)     NOT NULL,
  path             VARCHAR(255)    NOT NULL,
  summary          VARCHAR(255)    DEFAULT NULL,
  operation_id     VARCHAR(160)    DEFAULT NULL,
  hash             CHAR(64)        NOT NULL,
  deprecated       BIT(1)          NOT NULL DEFAULT 0,
  status           TINYINT         NOT NULL DEFAULT 1,
  request_schema   JSON            DEFAULT NULL,
  response_schema  JSON            DEFAULT NULL,
  last_sync_id     BIGINT UNSIGNED DEFAULT NULL,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='单接口定义';

ALTER TABLE autotest_group
  ADD KEY idx_group_project_method (project_id, method, status);

CREATE TABLE autotest_curl_variant (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  group_id         BIGINT UNSIGNED NOT NULL,
  variant_type     VARCHAR(32)     NOT NULL COMMENT 'minimal/full/custom...',
  version_no       VARCHAR(32)     DEFAULT NULL,
  curl_template    MEDIUMTEXT      NOT NULL,
  param_rules      JSON            DEFAULT NULL,
  generator_config JSON            DEFAULT NULL,
  rule_version     VARCHAR(32)     DEFAULT NULL,
  editor_id        BIGINT UNSIGNED DEFAULT NULL,
  editor_name      VARCHAR(120)    DEFAULT NULL,
  need_review      BIT(1)          NOT NULL DEFAULT 0,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='curl 模板与随机规则';

ALTER TABLE autotest_curl_variant
  ADD KEY idx_variant_group (group_id, variant_type);

CREATE TABLE autotest_rule_audit (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  variant_id       BIGINT UNSIGNED NOT NULL,
  change_type      VARCHAR(32)     NOT NULL COMMENT 'create/update/delete/review',
  rule_snapshot    JSON            NOT NULL,
  comment          VARCHAR(255)    DEFAULT NULL,
  operator_id      BIGINT UNSIGNED DEFAULT NULL,
  operator_name    VARCHAR(120)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='模板/规则审计';

ALTER TABLE autotest_rule_audit
  ADD KEY idx_rule_audit_variant (variant_id);

CREATE TABLE autotest_scenario (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(160)    NOT NULL,
  status           TINYINT         NOT NULL DEFAULT 1 COMMENT '0=草稿,1=启用,2=停用',
  tags             JSON            DEFAULT NULL,
  owner_id         BIGINT UNSIGNED DEFAULT NULL,
  owner_name       VARCHAR(120)    DEFAULT NULL,
  default_env_id   BIGINT UNSIGNED DEFAULT NULL,
  metadata         JSON            DEFAULT NULL COMMENT '调度选项/并发/执行窗口',
  need_review      BIT(1)          NOT NULL DEFAULT 0,
  last_execution_id BIGINT UNSIGNED DEFAULT NULL,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='测试场景';

ALTER TABLE autotest_scenario
  ADD KEY idx_scenario_project_status (project_id, status);

CREATE TABLE autotest_scenario_step (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  scenario_id      BIGINT UNSIGNED NOT NULL,
  order_no         INT             NOT NULL,
  step_alias       VARCHAR(120)    DEFAULT NULL,
  curl_variant_id  BIGINT UNSIGNED NOT NULL,
  invoke_options   JSON            DEFAULT NULL COMMENT '超时/重试/断言',
  variable_mapping JSON            DEFAULT NULL COMMENT '上游->下游 JSONPath 映射',
  extractors       JSON            DEFAULT NULL COMMENT '响应提取配置',
  condition_config JSON            DEFAULT NULL COMMENT '条件执行预留',
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='场景步骤';

ALTER TABLE autotest_scenario_step
  ADD KEY idx_step_scenario (scenario_id, order_no);

CREATE TABLE autotest_scenario_version (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  scenario_id      BIGINT UNSIGNED NOT NULL,
  version_no       VARCHAR(32)     NOT NULL,
  content          JSON            NOT NULL COMMENT '完整场景快照',
  comment          VARCHAR(255)    DEFAULT NULL,
  publisher_id     BIGINT UNSIGNED DEFAULT NULL,
  publisher_name   VARCHAR(120)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='场景版本存档';

ALTER TABLE autotest_scenario_version
  ADD KEY idx_scenario_version (scenario_id, version_no);

CREATE TABLE autotest_scenario_variable (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  scenario_id      BIGINT UNSIGNED NOT NULL,
  scope            VARCHAR(16)     NOT NULL COMMENT 'scene/step',
  owner_step_id    BIGINT UNSIGNED DEFAULT NULL,
  var_name         VARCHAR(120)    NOT NULL,
  source_type      VARCHAR(32)     NOT NULL COMMENT 'manual/default/extractor',
  init_value       JSON            DEFAULT NULL,
  binding_config   JSON            DEFAULT NULL COMMENT '输出绑定或默认值策略',
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='场景变量池';

ALTER TABLE autotest_scenario_variable
  ADD KEY idx_variable_scenario (scenario_id, scope);

CREATE TABLE autotest_execution (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  scenario_id      BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  env_id           BIGINT UNSIGNED DEFAULT NULL,
  trigger_type     VARCHAR(32)     NOT NULL COMMENT 'manual/webhook/schedule',
  trigger_user     VARCHAR(120)    DEFAULT NULL,
  external_job_id  VARCHAR(120)    DEFAULT NULL,
  status           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=排队,1=执行中,2=成功,3=失败,4=取消',
  start_time       DATETIME        NOT NULL,
  end_time         DATETIME        DEFAULT NULL,
  duration_ms      BIGINT          DEFAULT NULL,
  summary          JSON            DEFAULT NULL COMMENT '统计、失败节点',
  notify_channels  JSON            DEFAULT NULL,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='场景执行实例';

ALTER TABLE autotest_execution
  ADD KEY idx_execution_project_status (project_id, status);

CREATE TABLE autotest_execution_detail (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  execution_id     BIGINT UNSIGNED NOT NULL,
  scenario_step_id BIGINT UNSIGNED NOT NULL,
  step_order       INT             NOT NULL,
  status           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=排队,1=执行中,2=成功,3=失败,4=跳过',
  retry_count      INT             NOT NULL DEFAULT 0,
  request_snapshot JSON            DEFAULT NULL,
  response_snapshot JSON           DEFAULT NULL,
  variables_snapshot JSON          DEFAULT NULL,
  error_message    VARCHAR(1000)   DEFAULT NULL,
  start_time       DATETIME        DEFAULT NULL,
  end_time         DATETIME        DEFAULT NULL,
  remark           VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='步骤执行明细';

ALTER TABLE autotest_execution_detail
  ADD KEY idx_execution_detail_exec (execution_id, step_order);

-- 项目维度日志分表模板（创建项目时复制并替换 {projectId}）
CREATE TABLE autotest_execution_log_template (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  execution_id     BIGINT UNSIGNED NOT NULL,
  scenario_step_id BIGINT UNSIGNED DEFAULT NULL,
  log_time         DATETIME        NOT NULL,
  level            VARCHAR(16)     NOT NULL,
  message          VARCHAR(1000)   NOT NULL,
  extra            JSON            DEFAULT NULL,
  notification_channel VARCHAR(64) DEFAULT NULL,
  notification_status TINYINT      DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='执行日志分表模板 (复制为 autotest_execution_log_{projectId})';

ALTER TABLE autotest_execution_log_template
  ADD KEY idx_exec_log_execution (execution_id, log_time);

CREATE TABLE autotest_diff_snapshot (
  id               BIGINT UNSIGNED PRIMARY KEY,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  project_id       BIGINT UNSIGNED NOT NULL,
  source_type      VARCHAR(32)     NOT NULL COMMENT 'swagger/template/scenario',
  source_ref_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '指向 group/variant/scenario',
  related_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联同步或执行记录',
  diff_type        VARCHAR(32)     NOT NULL COMMENT 'add/update/delete/rule-change',
  diff_payload     JSON            NOT NULL,
  need_review      BIT(1)          NOT NULL DEFAULT 1,
  reviewer_id      BIGINT UNSIGNED DEFAULT NULL,
  reviewer_name    VARCHAR(120)    DEFAULT NULL,
  review_status    TINYINT         NOT NULL DEFAULT 0 COMMENT '0=待处理,1=通过,2=拒绝',
  review_comment   VARCHAR(255)    DEFAULT NULL,
  creator          VARCHAR(64)     NOT NULL,
  create_time      DATETIME        NOT NULL,
  updater          VARCHAR(64)     NOT NULL,
  update_time      DATETIME        NOT NULL,
  deleted          BIT(1)          NOT NULL DEFAULT 0
) COMMENT='差异快照与复核';

ALTER TABLE autotest_diff_snapshot
  ADD KEY idx_diff_project_status (project_id, need_review);
