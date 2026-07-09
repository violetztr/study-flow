# 数据库设计

当前版本使用 MySQL 8，通过 Flyway 管理数据库迁移。测试环境使用 H2 内存数据库验证迁移和接口行为。

## 表关系概览

当前核心表：

- `users`：用户账号
- `projects`：项目
- `tasks`：任务
- `tags`：标签
- `task_tags`：任务和标签的多对多关系
- `notes`：笔记页
- `note_blocks`：笔记块
- `daily_plans`：日常计划
- `journals`：日记
- `habits`：习惯
- `habit_records`：习惯打卡记录
- `project_profiles`：项目中台档案
- `project_tech_stacks`：项目技术栈
- `github_repositories`：项目绑定的 GitHub 仓库
- `portfolio_projects`：公开作品集发布设置

## users

保存用户账号。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名，唯一 |
| email | VARCHAR(100) | 邮箱，唯一 |
| password_hash | VARCHAR(255) | 加密后的密码 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## projects

保存用户创建的项目。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| user_id | BIGINT | 所属用户 |
| name | VARCHAR(100) | 项目名称 |
| description | VARCHAR(500) | 项目描述 |
| status | VARCHAR(30) | `ACTIVE` 或 `ARCHIVED` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## tasks

保存项目下的学习任务。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| user_id | BIGINT | 所属用户 |
| project_id | BIGINT | 所属项目 |
| title | VARCHAR(120) | 任务标题 |
| description | TEXT | 任务描述 |
| status | VARCHAR(30) | `PENDING`、`IN_PROGRESS`、`DONE` |
| priority | VARCHAR(30) | `LOW`、`MEDIUM`、`HIGH` |
| deadline | DATETIME | 截止时间 |
| estimated_minutes | INT | 预计学习时长 |
| completed_at | DATETIME | 完成时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## tags / task_tags

`tags` 保存用户自定义标签，`task_tags` 保存任务和标签的多对多关系。

## notes / note_blocks

`notes` 保存笔记页树，`note_blocks` 保存页面里的段落、标题、待办、引用和代码块。

## daily_plans / journals / habits / habit_records

日常模块表：

- `daily_plans`：某一天要推进的计划
- `journals`：某一天的学习复盘
- `habits`：长期习惯
- `habit_records`：某个习惯在某一天是否完成

## project_profiles

项目中台的项目档案表，用来把项目整理成能展示、能复盘、能面试讲清楚的资料。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID，唯一 |
| user_id | BIGINT | 所属用户 |
| headline | VARCHAR(160) | 一句话定位 |
| production_url | VARCHAR(300) | 线上地址 |
| api_doc_url | VARCHAR(300) | 接口文档地址 |
| database_doc_url | VARCHAR(300) | 数据库文档地址 |
| architecture_summary | TEXT | 架构说明 |
| interview_highlights | TEXT | 面试亮点 |
| cover_image_url | VARCHAR(500) | 封面图 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## project_tech_stacks

项目技术栈表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID |
| user_id | BIGINT | 所属用户 |
| name | VARCHAR(60) | 技术名称 |
| category | VARCHAR(40) | `FRONTEND`、`BACKEND`、`DATABASE`、`DEPLOYMENT`、`TOOLING`、`OTHER` |
| sort_order | INT | 排序 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## github_repositories

项目绑定的 GitHub 仓库和同步后的元数据。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID，唯一 |
| user_id | BIGINT | 所属用户 |
| owner | VARCHAR(80) | GitHub owner |
| repo | VARCHAR(120) | GitHub repo |
| html_url | VARCHAR(300) | 仓库地址 |
| description | VARCHAR(500) | 仓库描述 |
| default_branch | VARCHAR(80) | 默认分支 |
| primary_language | VARCHAR(80) | 主语言 |
| stars | INT | star 数 |
| forks | INT | fork 数 |
| open_issues | INT | issue 数 |
| pushed_at | DATETIME | 最近 push 时间 |
| last_synced_at | DATETIME | 最近同步时间 |
| readme_present | BOOLEAN | 是否检测到 README |
| languages_json | TEXT | 语言统计 JSON |
| latest_commits_json | TEXT | 最近提交 JSON |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## portfolio_projects

公开作品集发布设置。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID，唯一 |
| user_id | BIGINT | 所属用户 |
| slug | VARCHAR(120) | 公开访问路径，唯一 |
| public_visible | BOOLEAN | 是否公开 |
| featured | BOOLEAN | 是否精选 |
| display_order | INT | 排序 |
| public_summary | VARCHAR(500) | 公开摘要 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 设计说明

当前版本暂时不使用数据库外键。业务层负责校验数据归属，例如任务必须属于当前用户自己的项目，项目档案、技术栈、GitHub 仓库和作品集设置也必须通过项目所有权校验。
