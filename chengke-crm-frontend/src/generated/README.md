# OpenAPI 生成目录

此目录用于保存从后端 `/v3/api-docs` 生成的 TypeScript 类型或 API Client。

- 生成文件不得手工修改。
- 后端接口契约稳定后再确定具体生成器。
- 业务 feature 通过适配函数消费生成类型，不直接依赖后端数据库对象。
