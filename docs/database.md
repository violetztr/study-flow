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

早期个人学习表主要通过业务层校验数据归属，例如任务必须属于当前用户自己的项目，项目档案、技术栈、GitHub 仓库和作品集设置也必须通过项目所有权校验。Violet Circle 社区表已经补充核心外键，用数据库约束保护圈子、用户、主题、帖子、评论、反应和审核动作之间的基础关系。

## Violet Circle 社区表

社区模块通过 `V6__add_violet_circle_community.sql` 新增。第一阶段只有一个默认圈子 `violet-circle`，但表结构保留 `circle_id`，以后可以扩展多个圈子。V6 会把已有 `ACTIVE` 用户回填进默认圈子，并为他们创建默认 `user_profiles` 记录，避免老用户升级后访问 `/circle` 时没有圈子身份。

### users 新增字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| role | VARCHAR(30) | 用户全局角色，默认 `MEMBER`，管理员可用 `ADMIN`，圈主可用 `OWNER` |
| status | VARCHAR(30) | 用户状态，默认 `ACTIVE`，禁用为 `DISABLED` |

### circles

保存圈子空间。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 圈子名称 |
| slug | VARCHAR(120) | 访问标识，默认圈子为 `violet-circle` |
| description | VARCHAR(500) | 圈子说明 |
| visibility | VARCHAR(40) | 可见范围，第一阶段为 `PUBLIC_REGISTERED` |
| status | VARCHAR(30) | 圈子状态 |

### circle_members

保存用户和圈子的成员关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(30) | 圈内角色：`OWNER`、`ADMIN`、`MEMBER` |
| status | VARCHAR(30) | 圈内状态：`ACTIVE`、`MUTED`、`DISABLED` |
| joined_at | DATETIME | 加入时间 |

注册用户会自动加入默认圈子。被禁言成员可以浏览内容，但不能发帖、评论、点赞、取消点赞或修改社区资料。`DISABLED` 圈内成员不能访问社区，也不会出现在普通成员列表中。

### user_profiles

保存社区展示资料。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID，唯一 |
| display_name | VARCHAR(80) | 展示昵称 |
| bio | VARCHAR(500) | 简介 |
| avatar_url | VARCHAR(500) | 头像地址 |
| skills | VARCHAR(500) | 技能标签字符串 |
| github_url | VARCHAR(300) | GitHub 地址 |
| website_url | VARCHAR(300) | 个人网站 |

### community_topics

保存社区话题。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| name | VARCHAR(80) | 话题名称 |
| slug | VARCHAR(120) | 话题标识 |
| color | VARCHAR(30) | 话题颜色 |
| sort_order | INT | 排序 |
| post_count | INT | 已发布帖子数量 |
| status | VARCHAR(30) | 话题状态 |

默认初始化学习、笔记、日常、项目四个话题。

### community_posts

保存社区帖子。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| author_id | BIGINT | 作者用户 ID |
| topic_id | BIGINT | 话题 ID，可为空 |
| title | VARCHAR(160) | 标题 |
| content | TEXT | 正文 |
| content_format | VARCHAR(30) | 内容格式，第一阶段为 `TEXT` |
| visibility | VARCHAR(30) | 可见范围，第一阶段为 `CIRCLE` |
| status | VARCHAR(30) | `PUBLISHED`、`HIDDEN`、`DELETED` |
| pinned | BOOLEAN | 是否置顶 |
| comment_count | INT | 评论数 |
| reaction_count | INT | 点赞数 |
| view_count | INT | 浏览数 |
| last_activity_at | DATETIME | 最近活跃时间 |
| deleted_at | DATETIME | 软删除时间 |

### community_comments

保存帖子评论。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| post_id | BIGINT | 帖子 ID |
| author_id | BIGINT | 评论作者 |
| parent_id | BIGINT | 父评论 ID，第一阶段为空 |
| content | TEXT | 评论内容 |
| status | VARCHAR(30) | `PUBLISHED`、`HIDDEN`、`DELETED` |
| reaction_count | INT | 评论互动数，第一阶段保留 |
| deleted_at | DATETIME | 软删除时间 |

### community_reactions

保存通用互动。第一阶段只使用帖子点赞。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| target_type | VARCHAR(30) | 目标类型，第一阶段为 `POST` |
| target_id | BIGINT | 目标 ID |
| user_id | BIGINT | 操作用户 |
| reaction_type | VARCHAR(30) | 互动类型，第一阶段为 `LIKE` |

唯一约束：`circle_id + target_type + target_id + user_id + reaction_type`，保证同一用户不能重复点赞同一目标。

### community_moderation_actions

保存管理员审核操作记录。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 圈子 ID |
| admin_user_id | BIGINT | 管理员用户 ID |
| target_type | VARCHAR(30) | 目标类型：`POST`、`COMMENT`、`MEMBER` |
| target_id | BIGINT | 目标 ID |
| action_type | VARCHAR(40) | 操作类型：`HIDE`、`RESTORE`、`MUTE`、`UNMUTE` |
| reason | VARCHAR(500) | 操作原因 |
| created_at | DATETIME | 操作时间 |

审核状态变更使用条件更新，避免并发重复隐藏、重复恢复、重复禁言或重复解禁导致计数或审计记录不一致。管理员不能管理自己，也不能禁言圈主或同级管理员；圈主可以处理管理员和普通成员。
