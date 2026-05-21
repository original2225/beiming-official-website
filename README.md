# Beiming Official Website

中文名：北冥官网

这是北冥官网的正式项目仓库。系统从零开始开发，先固化项目规则、需求文档、系统设计和开发治理，再逐步落地前端、后端服务、管理后台和运维控制台。

## 当前状态

仓库处于从零开发阶段，当前重点是明确开发边界、模块顺序和协作规则。

## 文档

- `docs/requirements.md`：需求文档
- `docs/system-design.md`：系统设计文档
- `docs/development-governance.md`：开发治理文档
- `docs/contracts-common.md`：P0 公共契约
- `docs/contracts-auth.md`：P0 账号权限与邀请码契约
- `AGENTS.md`：开发协作规则

正式需求、系统设计、开发治理、接口契约和模块验收标准都保存在 `docs/` 并随仓库提交。本地阶段资料、测试记录、临时分析和微服务开发指导文档只保存在 `.local-docs/`，不上传仓库。

## 开发原则

先定边界，再写代码。一次只做一个模块。新增模块必须兼容已有模块，不允许顺手修改无关代码。
