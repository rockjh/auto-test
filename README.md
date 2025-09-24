# 自动生成器 API 测试平台

本项目实现了 PRD/设计文档中描述的架构。这是一个 Maven 多模块的 Spring Boot 应用，由基础设施、Swagger 同步、模板生成、场景编排、执行以及差异审核等模块组成。

## 模块划分

- `auto-test-infra` – 共享的 Spring 配置、持久化辅助工具和 ID 生成器。
- `auto-test-module-swagger` – 项目管理、Swagger 导入与差异跟踪接口。
- `auto-test-module-template` – curl 模板生成与预览接口。
- `auto-test-module-scenario` – 场景建模、步骤编排与发布。
- `auto-test-module-executor` – 轻量级执行器，用于模拟场景顺序执行。
- `auto-test-module-diff` – 差异快照列表与审核操作。
- `auto-test-server` – Spring Boot 启动入口，装配所有模块。

## 快速开始

1. 确认已安装 JDK 17 和 Maven。
2. 在仓库根目录执行 `mvn -pl auto-test-server -am package`。
3. 通过 `java -jar auto-test-server/target/auto-test-server-0.0.1-SNAPSHOT.jar` 启动服务。
4. 在 `http://localhost:18080` 访问 Swagger 接口及其他 API。

应用默认使用内存版 H2 数据库启动，并会自动从 `schema.sql` 加载数据库结构。
