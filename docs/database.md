# 数据库设计

本文档记录 StudyFlow V1 的数据表、字段、关系和索引。

## 表关系概览

StudyFlow 当前使用 11 张核心表：

- `users`：用户表，一个用户可以拥有多个项目、任务和标签。
- `projects`：项目表，一个项目属于一个用户。
- `tasks`：任务表，一个任务属于一个用户，也属于一个项目。
- `tags`：标签表，一个标签属于一个用户。
- `task_tags`：任务标签关联表，用来表示任务和标签的多对多关系。
- `notes`：笔记页面表，用来保存 Notion 风格的页面树。
- `note_blocks`：笔记块表，用来保存段落、标题、待办、引用和代码块。
- `daily_plans`：日常计划表，用来保存某一天要推进的事情。
- `journals`：日记表，用来保存某一天的学习复盘。
- `habits`：习惯表，用来保存长期习惯。
- `habit_records`：习惯打卡记录表，用来保存某一天是否完成。

## users 用户表

用途：保存用户账号信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 用户 ID，主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| email | VARCHAR(100) | 邮箱，唯一 |
| password_hash | VARCHAR(255) | 加密后的密码 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束和索引：

- `username` 唯一，防止重复用户名。
- `email` 唯一，防止重复邮箱。

## projects 项目表

用途：保存用户创建的学习项目。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 项目 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| name | VARCHAR(100) | 项目名称 |
| description | VARCHAR(500) | 项目描述 |
| status | VARCHAR(30) | 项目状态，默认 `ACTIVE` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束和索引：

- `idx_projects_user_id`：按用户查询项目列表时使用。

## tasks 任务表

用途：保存项目下面的学习任务。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 任务 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| project_id | BIGINT | 所属项目 ID |
| title | VARCHAR(120) | 任务标题 |
| description | TEXT | 任务描述 |
| status | VARCHAR(30) | 任务状态，默认 `PENDING` |
| priority | VARCHAR(30) | 任务优先级，默认 `MEDIUM` |
| deadline | DATETIME | 截止时间 |
| estimated_minutes | INT | 预计学习时长，单位分钟，可为空 |
| completed_at | DATETIME | 完成时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

任务状态：

- `PENDING`：待开始
- `IN_PROGRESS`：进行中
- `DONE`：已完成

任务优先级：

- `LOW`：低
- `MEDIUM`：中
- `HIGH`：高

约束和索引：

- `idx_tasks_user_id`：按用户查询任务时使用。
- `idx_tasks_project_id`：按项目查询任务时使用。
- `idx_tasks_status`：按任务状态筛选时使用。
- `idx_tasks_priority`：按任务优先级筛选时使用。

## tags 标签表

用途：保存用户自定义标签。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 标签 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| name | VARCHAR(50) | 标签名称 |
| color | VARCHAR(20) | 标签颜色，默认 `#1677ff` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束和索引：

- `uk_tags_user_name`：同一个用户不能创建同名标签。
- `idx_tags_user_id`：按用户查询标签时使用。

## task_tags 任务标签关联表

用途：保存任务和标签之间的多对多关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 关联 ID，主键，自增 |
| task_id | BIGINT | 任务 ID |
| tag_id | BIGINT | 标签 ID |
| created_at | DATETIME | 创建时间 |

约束和索引：

- `uk_task_tags_task_tag`：同一个任务不能重复绑定同一个标签。
- `idx_task_tags_task_id`：按任务查询标签时使用。
- `idx_task_tags_tag_id`：按标签查询任务时使用。

## 设计说明

第一版暂时不使用数据库外键。这样做是为了降低本地开发、测试数据清理和迁移调整的复杂度。业务层会负责校验数据归属，例如任务必须属于当前用户自己的项目。

## notes 笔记页面表

用途：保存笔记页面树，每条记录是一篇笔记或一个页面。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 笔记 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| parent_id | BIGINT | 父笔记 ID，可为空 |
| title | VARCHAR(120) | 笔记标题 |
| icon | VARCHAR(50) | 笔记图标 |
| favorite | BOOLEAN | 是否收藏 |
| archived | BOOLEAN | 是否归档 |
| sort_order | INT | 排序值 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## note_blocks 笔记块表

用途：保存笔记正文里的块内容。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 块 ID，主键，自增 |
| note_id | BIGINT | 所属笔记 ID |
| user_id | BIGINT | 所属用户 ID |
| type | VARCHAR(30) | 块类型：`paragraph`、`heading`、`todo`、`quote`、`code` |
| content | TEXT | 块内容 |
| checked | BOOLEAN | 待办块是否完成 |
| sort_order | INT | 块排序 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## daily_plans 日常计划表

用途：保存用户每天要推进的计划。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 计划 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| plan_date | DATE | 计划日期 |
| title | VARCHAR(120) | 计划标题 |
| description | VARCHAR(500) | 计划描述 |
| status | VARCHAR(30) | 状态：`TODO`、`DOING`、`DONE` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## journals 日记表

用途：保存每天的学习复盘，同一个用户同一天只有一篇日记。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 日记 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| journal_date | DATE | 日记日期 |
| mood | VARCHAR(30) | 心情或状态 |
| content | TEXT | 日记内容 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## habits 习惯表

用途：保存用户想长期坚持的习惯。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 习惯 ID，主键，自增 |
| user_id | BIGINT | 所属用户 ID |
| name | VARCHAR(80) | 习惯名称 |
| description | VARCHAR(300) | 习惯描述 |
| active | BOOLEAN | 是否启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## habit_records 习惯打卡记录表

用途：保存某个习惯在某一天是否完成。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 记录 ID，主键，自增 |
| habit_id | BIGINT | 所属习惯 ID |
| user_id | BIGINT | 所属用户 ID |
| record_date | DATE | 打卡日期 |
| completed | BOOLEAN | 是否完成 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
