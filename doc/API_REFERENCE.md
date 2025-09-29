# 接口操作手册

## 使用顺序概览
1. **Swagger 项目管理**：创建或同步项目，确保接口元数据入库，并获取差异信息。
2. **项目环境维护**：为项目配置 Host/Header/变量，供模板和场景引用。
3. **`curl` 模板管理**：生成与预览模板，确认随机策略与复核状态。
4. **场景建模与发布**：基于模板编排步骤，配置变量映射与默认环境。
5. **场景执行与监控**：触发执行、查看执行详情与日志，关注互斥与配额。
6. **差异复核闭环**：处理同步或模板变更产生的差异快照，完成复核与标记清理。

## 响应包装说明
系统所有接口统一返回 `CommonResult<T>`。该结构字段如下：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `code` | Integer | 业务状态码，`0` 表示成功，其余参见 `AutoTestErrorCode` | `0` |
| `message` | String | 与 `code` 对应的描述信息 | `OK` |
| `data` | T | 实际业务数据载荷，具体结构见各接口说明 | 取决于接口 |

下文“响应字段”中未特别说明的字段均位于 `data` 节点内。

---

## 阶段一：Swagger 项目（PRD 第五章、详细设计第 2 章）

### 1. 创建 Swagger 项目 `POST /api/swagger/projects`
- **说明**：根据提供的 Swagger 来源创建项目，并触发首次解析入库。
- **请求体字段（ProjectCreateRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `name` | String | 是 | - | 项目名称 | `支付中心` |
| `swaggerSource` | String | 是 | - | Swagger 文档来源，URL/JSON 文本/文件地址 | `https://example.com/openapi.yaml` |
| `swaggerType` | Integer | 是 | - | Swagger 来源类型：`1`-URL，`2`-JSON 文本，`3`-文件上传 | `1` |

- **响应字段**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `data` | Long | 新建项目编号 | `1001` |

### 2. 查询项目列表 `GET /api/swagger/projects`
- **说明**：返回当前租户下全部 Swagger 项目的基础信息与同步状态。
- **请求参数**：无。
- **响应字段（List<ProjectResponse>）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 项目编号 | `1001` |
| `name` | String | 项目名称 | `支付中心` |
| `swaggerSource` | String | 原始 Swagger 来源 | `https://example.com/openapi.yaml` |
| `swaggerType` | Integer | 来源类型：`1`-URL，`2`-JSON 文本，`3`-文件上传 | `1` |
| `swaggerVersion` | String | 最近解析得到的版本号 | `1.0.12` |
| `swaggerHash` | String | 最近一次同步内容 Hash | `6f5a0b86f271` |
| `syncStatus` | Integer | 最近同步状态：`0`-未开始，`1`-成功，`2`-失败 | `1` |
| `syncTime` | LocalDateTime | 最近同步完成时间（ISO-8601） | `2025-01-15T10:15:30` |

### 3. 查询项目详情 `GET /api/swagger/projects/{id}`
- **说明**：读取单个项目的最新元数据。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `id` | Long | 是 | 项目编号 | `1001` |

- **响应字段**：同“查询项目列表”中的 `ProjectResponse` 结构。

### 4. 触发 Swagger 同步 `POST /api/swagger/projects/{id}/sync`
- **说明**：手动重新抓取或覆盖 Swagger 内容，生成差异摘要。
- **路径参数**：同上。
- **请求体字段（ProjectSyncRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `triggerType` | Integer | 否 | `1` | 触发类型：`1`-手动，`2`-Webhook 等 | `1` |
| `overrideSource` | String | 否 | - | 直接覆盖的 Swagger 内容（JSON/YAML 文本）；为空则按项目配置远程抓取 | `{ "openapi": "3.0.1" }` |
| `remark` | String | 否 | - | 同步备注，便于审计与复盘 | `紧急修复 definition 缺失` |
| `allowRemoteFetch` | Boolean | 否 | `false` | 是否允许使用项目配置远程下载 Swagger | `false` |
| `requestHeaders` | Map<String,String> | 否 | - | 远程抓取附加 Header，key 为 Header 名，value 为值 | `{ "X-Tenant": "qa" }` |

- **响应字段（SwaggerDiffResponse）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `syncId` | Long | 同步记录编号 | `202501150001` |
| `status` | Integer | 同步状态：`0`-进行中，`1`-成功，`2`-失败 | `1` |
| `diffSummary` | String | 差异摘要：新增/修改/删除统计等 | `新增接口 3 个，字段差异 2 项` |
| `startTime` | LocalDateTime | 同步开始时间 | `2025-01-15T10:15:30` |
| `endTime` | LocalDateTime | 同步结束时间 | `2025-01-15T10:15:45` |
| `errorMessage` | String | 同步失败时的错误信息 | `解析 path '/api/order' 缺少 method 定义` |

### 5. 查询最新差异 `GET /api/swagger/projects/{id}/diff`
- **说明**：查看项目最近一次同步的差异摘要。
- **路径参数**：同上。
- **响应字段**：同 `SwaggerDiffResponse`。

---

## 阶段二：项目环境（PRD「项目级环境配置」、详细设计 2.3）

### 1. 新增项目环境 `POST /api/swagger/projects/{projectId}/envs`
- **说明**：为项目创建 Host/Header/变量配置，可选设置默认环境。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `projectId` | Long | 是 | 所属项目编号 | `1001` |

- **请求体字段（ProjectEnvironmentCreateRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `projectId` | Long | 是 | - | 所属项目编号，需与路径参数一致 | `1001` |
| `name` | String | 是 | - | 环境名称 | `SIT` |
| `envType` | Integer | 否 | `1` | 环境类型：`1`-手工维护，`2`-继承 | `1` |
| `host` | String | 是 | - | 环境 Host/BaseUrl | `https://sit.example.com` |
| `headers` | Map<String,String> | 否 | - | 通用请求 Header，值可包含变量占位符 | `{ "Authorization": "Bearer ${token}" }` |
| `variables` | Map<String,Object> | 否 | - | 环境级变量池 | `{ "tenant": "qa" }` |
| `isDefault` | Boolean | 否 | `false` | 是否设为默认环境 | `true` |
| `status` | Integer | 否 | `1` | 启用状态：`1`-启用，`0`-停用 | `1` |
| `remark` | String | 否 | - | 备注信息 | `默认走 SIT 集群` |

- **响应字段**：`data` 为新建环境编号 `Long`。

### 2. 更新项目环境 `PUT /api/swagger/projects/{projectId}/envs/{envId}`
- **说明**：调整环境配置或默认标记。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `projectId` | Long | 是 | 项目编号 | `1001` |
| `envId` | Long | 是 | 环境编号 | `2001` |

- **请求体字段（ProjectEnvironmentUpdateRequest）**：字段与创建请求一致，但均可选。

- **响应字段**：`data` 为 `Boolean`，成功固定返回 `true`。

### 3. 删除项目环境 `DELETE /api/swagger/projects/{projectId}/envs/{envId}`
- **说明**：逻辑删除指定环境。
- **路径参数**：同上。
- **响应字段**：`data` 为 `Boolean`，成功返回 `true`。

### 4. 查询项目环境列表 `GET /api/swagger/projects/{projectId}/envs`
- **说明**：按项目返回全部环境，默认环境优先排序。
- **路径参数**：同新增接口。
- **响应字段（List<ProjectEnvironmentResponse>）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 环境编号 | `2001` |
| `projectId` | Long | 所属项目编号 | `1001` |
| `name` | String | 环境名称 | `SIT` |
| `envType` | Integer | 环境类型：`1`-手工维护，`2`-继承 | `1` |
| `host` | String | 环境 Host/BaseUrl | `https://sit.example.com` |
| `headers` | Map<String,String> | 公共 Header 配置 | `{ "Authorization": "Bearer ${token}" }` |
| `variables` | Map<String,Object> | 环境变量映射 | `{ "tenant": "qa" }` |
| `isDefault` | Boolean | 是否默认环境 | `true` |
| `status` | Integer | 启用状态：`1`-启用，`0`-停用 | `1` |
| `remark` | String | 备注 | `默认走 SIT 集群` |
| `createTime` | LocalDateTime | 创建时间 | `2025-01-11T10:00:00` |
| `updateTime` | LocalDateTime | 最后更新时间 | `2025-01-12T09:30:00` |

### 5. 查询环境详情 `GET /api/swagger/projects/{projectId}/envs/{envId}`
- **说明**：读取单个环境配置，字段同 `ProjectEnvironmentResponse`。

---

## 阶段三：`curl` 模板管理（PRD 第五章、详细设计第 3 章）

### 1. 查询接口模板 `GET /api/template/variants/group/{groupId}`
- **说明**：根据接口分组编号返回所有模板类型及复核状态。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `groupId` | Long | 是 | Swagger 接口分组编号 | `2001001` |

- **响应字段（List<CurlVariantResponse>）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 模板编号 | `301001` |
| `projectId` | Long | 所属项目编号 | `1001` |
| `groupId` | Long | 所属接口分组编号 | `2001001` |
| `variantType` | String | 模板类型：`minimal`/`full`/`custom` | `minimal` |
| `curlTemplate` | String | 模板正文，包含变量占位符 | `curl -X GET ...` |
| `paramRules` | String | 参数随机策略 JSON 字符串 | `{ "orderId": {"random": "number"} }` |
| `needReview` | Boolean | 是否需要人工复核 | `false` |

### 2. 重新生成最小模板 `POST /api/template/variants/group/{groupId}/minimal`
- **说明**：基于最新 Swagger 元数据生成默认最小模板并覆盖旧版本。
- **路径参数**：同上。
- **响应字段**：`CurlVariantResponse` 结构。

### 3. 按类型重新生成模板 `POST /api/template/variants/group/{groupId}/regenerate/{variantType}`
- **说明**：重新生成指定类型的模板，支持扩展新类型。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `groupId` | Long | 是 | 接口分组编号 | `2001001` |
| `variantType` | String | 是 | 模板类型，允许值：`minimal`、`full` 等 | `full` |

- **响应字段**：`CurlVariantResponse` 结构。

### 4. 预览模板 `POST /api/template/variants/{variantId}/preview`
- **说明**：根据变量映射渲染模板，返回可执行的 `curl` 文本，供 QA 校验。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `variantId` | Long | 是 | 模板编号 | `301001` |

- **请求体字段（TemplatePreviewRequest，可选）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `variables` | Map<String,Object> | 否 | - | 覆盖模板变量的键值对，key 为变量路径 | `{ "path.orderId": 123456789 }` |

- **响应字段**：`data` 为渲染后的 `curl` 字符串。

---

## 阶段四：场景建模与发布（PRD 第五章、详细设计第 4 章）

### 1. 创建场景 `POST /api/scenario`
- **说明**：创建场景草稿，包含步骤定义、变量映射与默认执行环境。
- **请求体字段（ScenarioCreateRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `projectId` | Long | 是 | - | 所属项目编号 | `1001` |
| `name` | String | 是 | - | 场景名称 | `订单支付成功链路` |
| `defaultEnvId` | Long | 否 | - | 默认执行环境编号 | `2001` |
| `remark` | String | 否 | - | 场景备注 | `覆盖下单+支付+查询` |
| `steps` | List<ScenarioStepRequest> | 是 | - | 场景步骤列表，至少 1 条 | 见下 |

- **步骤结构（ScenarioStepRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `curlVariantId` | Long | 是 | - | 引用的模板编号 | `301001` |
| `stepAlias` | String | 否 | - | 步骤别名，便于识别 | `提交订单` |
| `orderNo` | Integer | 否 | - | 执行顺序，默认按传入顺序，建议从 1 开始 | `1` |
| `variableMapping` | String | 否 | - | 变量映射配置 JSON 数组，记录 JSONPath 与目标字段 | `[{"sourcePath":"$.data.orderId","targetKey":"query.orderId"}]` |
| `invokeOptions` | String | 否 | - | 调用选项 JSON，如重试策略等 | `{ "retry": 1 }` |

- **响应字段**：`data` 为新建场景编号 `Long`。

### 2. 更新场景 `PUT /api/scenario/{id}`
- **说明**：覆盖场景草稿内容，未发布版本仍可修改。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `id` | Long | 是 | 场景编号 | `5001001` |

- **请求体字段**：同创建请求。
- **响应字段**：`data` 为空（`null`）。

### 3. 发布场景 `POST /api/scenario/{id}/publish`
- **说明**：生成新的场景版本，冻结当前步骤并标记可执行。
- **路径参数**：同上。
- **请求体字段（ScenarioPublishRequest，可选）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `comment` | String | 否 | - | 发布备注 | `覆盖 1.2.0 版本接口` |

- **响应字段**：`data` 为空。

### 4. 查询场景详情 `GET /api/scenario/{id}`
- **说明**：返回场景基础信息及最新步骤列表。
- **路径参数**：同上。
- **响应字段（ScenarioResponse）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 场景编号 | `5001001` |
| `projectId` | Long | 所属项目编号 | `1001` |
| `name` | String | 场景名称 | `订单支付成功链路` |
| `status` | Integer | 场景状态：`0`-草稿，`1`-已发布，`2`-废弃 | `1` |
| `defaultEnvId` | Long | 默认环境编号 | `2001` |
| `remark` | String | 备注 | `覆盖 1.2.0 版本接口` |
| `needReview` | Boolean | 是否需要人工复核 | `false` |
| `createTime` | LocalDateTime | 创建时间 | `2025-01-15T11:02:00` |
| `steps` | List<ScenarioStepVO> | 最新步骤视图 | 见下 |

- **步骤视图（ScenarioStepVO）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 步骤编号 | `7001001` |
| `orderNo` | Integer | 执行顺序 | `1` |
| `stepAlias` | String | 步骤别名 | `提交订单` |
| `curlVariantId` | Long | 引用的模板编号 | `301001` |
| `variableMapping` | String | 变量映射配置 JSON | `["..."]` |
| `invokeOptions` | String | 调用选项配置 JSON | `{ "retry": 1 }` |

### 5. 查询项目场景列表 `GET /api/scenario`
- **说明**：按项目编号返回全部场景（草稿 + 已发布）。
- **查询参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `projectId` | Long | 是 | 项目编号 | `1001` |

- **响应字段**：同 `ScenarioResponse` 列表。

---

## 阶段五：场景执行与监控（PRD 第五章、详细设计第 5 章）

### 1. 触发执行 `POST /api/execution/trigger`
- **说明**：校验场景互斥与配额后，提交执行任务并写入执行记录。
- **请求体字段（ExecutionTriggerRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `scenarioId` | Long | 是 | - | 目标场景编号 | `5001001` |
| `envId` | Long | 否 | - | 指定执行环境编号；为空则使用场景默认环境 | `2001` |
| `remark` | String | 否 | - | 触发备注，写入执行记录 | `回归-订单链路` |

- **响应字段**：`data` 为执行记录编号 `Long`。

### 2. 查询执行详情 `GET /api/execution/{id}`
- **说明**：返回执行总体状态、耗时以及各步骤快照。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `id` | Long | 是 | 执行记录编号 | `9001001` |

- **响应字段（ExecutionResponse）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 执行记录编号 | `9001001` |
| `scenarioId` | Long | 场景编号 | `5001001` |
| `status` | Integer | 执行状态：`0`-排队，`1`-执行中，`2`-成功，`3`-失败，`4`-取消 | `2` |
| `startTime` | LocalDateTime | 执行开始时间 | `2025-01-15T12:30:00` |
| `endTime` | LocalDateTime | 执行结束时间 | `2025-01-15T12:30:15` |
| `durationMs` | Long | 执行耗时（毫秒） | `15000` |
| `remark` | String | 触发备注 | `上线前回归` |
| `steps` | List<ExecutionDetailResponse> | 步骤执行详情列表 | 见下 |

- **步骤详情（ExecutionDetailResponse）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 执行步骤记录编号 | `9100001` |
| `scenarioStepId` | Long | 场景步骤编号 | `7001001` |
| `stepOrder` | Integer | 步骤顺序 | `1` |
| `status` | Integer | 步骤状态：`0`-执行中，`1`-成功，`2`-失败，`3`-跳过 | `1` |
| `requestSnapshot` | String | 请求快照 JSON | `{ "path": "/api/order" }` |
| `responseSnapshot` | String | 响应快照 JSON | `{ "code": 0, "data": { ... } }` |
| `errorMessage` | String | 失败时的错误信息 | `超时 5s` |
| `startTime` | LocalDateTime | 步骤开始时间 | `2025-01-15T12:30:01` |
| `endTime` | LocalDateTime | 步骤结束时间 | `2025-01-15T12:30:03` |

### 3. 分页查询执行日志 `GET /api/execution/{id}/logs`
- **说明**：按执行记录编号分页返回执行日志，支持排查失败原因。
- **路径参数**：同执行详情。
- **查询参数**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `pageNo` | Integer | 否 | `1` | 页码，从 1 开始 | `1` |
| `pageSize` | Integer | 否 | `20` | 每页条数 | `20` |

- **响应字段**：`data` 为 `PageResult<ExecutionLogResponse>`，包含分页元数据（`total`、`pageNo`、`pageSize`）及日志列表。
- **日志条目（ExecutionLogResponse）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 日志编号 | `9100001` |
| `executionId` | Long | 关联执行记录编号 | `9001001` |
| `scenarioStepId` | Long | 关联步骤编号，可为空 | `7001001` |
| `logTime` | LocalDateTime | 日志时间 | `2025-01-15T12:30:05` |
| `level` | String | 日志级别 | `INFO` |
| `message` | String | 日志内容 | `步骤 1 执行成功` |
| `extra` | String | 附加信息 JSON | `{ "command": "curl -X GET ..." }` |
| `notificationChannel` | String | 通知渠道，如 `EMAIL`、`DINGTALK`，无通知为空 | `EMAIL` |
| `notificationStatus` | Integer | 通知状态码，具体含义见通知模块 | `0` |

---

## 阶段六：差异复核闭环（PRD 第五章、详细设计第 6 章）

### 1. 查询差异列表 `GET /api/diff`
- **说明**：按项目筛选待复核或全部差异快照，支持差异复盘。
- **查询参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `projectId` | Long | 否 | 项目编号，缺省返回当前租户全部差异 | `1001` |

- **响应字段（List<DiffSnapshotResponse>）**：

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | Long | 差异快照编号 | `8001001` |
| `projectId` | Long | 项目编号 | `1001` |
| `sourceType` | String | 差异来源类型（如 `swagger`、`template`） | `swagger` |
| `sourceRefId` | Long | 差异来源关联 ID（如同步记录 ID） | `202501150001` |
| `relatedId` | Long | 关联实体 ID（如模板或场景编号） | `301001` |
| `diffType` | String | 差异类型，见差异模块枚举 | `INTERFACE_CHANGE` |
| `diffPayload` | String | 差异内容快照 JSON | `{ "added": 3, "updated": 2 }` |
| `needReview` | Boolean | 是否待复核 | `true` |
| `reviewStatus` | Integer | 复核状态：`0`-待处理，`1`-通过，`2`-驳回 | `0` |
| `reviewComment` | String | 复核备注 | `等待模板调整` |
| `createTime` | LocalDateTime | 创建时间 | `2025-01-15T09:30:00` |

### 2. 复核差异 `POST /api/diff/{id}/review`
- **说明**：提交差异复核结果，联动模板/场景 `needReview` 状态。
- **路径参数**：

| 字段 | 类型 | 是否必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `id` | Long | 是 | 差异快照编号 | `8001001` |

- **请求体字段（DiffReviewRequest）**：

| 字段 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `reviewStatus` | Integer | 是 | - | 复核状态：`1`-通过，`2`-驳回 | `1` |
| `reviewComment` | String | 否 | - | 复核备注，建议填写处理意见 | `确认模板调整无误` |

- **响应字段**：返回最新 `DiffSnapshotResponse`，字段同上。

---

## 全局注意事项
- 错误码与提示语统一维护在 `AutoTestErrorCode` 枚举，禁止硬编码常量。
- 多租户与权限信息通过登录态传递，服务内部使用 `TenantContextHolder`、`SecurityContextUtils` 获取租户/用户上下文。
- 若后续 PRD 或设计文档更新导致接口契约变动，请同步确认是否需要更新 `doc/` 目录下的文档，以保持一致性。
