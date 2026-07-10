# 数据库设计

当前版本使用 MySQL 8，通过 Flyway 管理数据库迁移。测试环境使用 H2 内存数据库验证迁移和接口行为。

## 当前核心表

- `users`：用户账号
- `circles`：社区空间
- `circle_members`：用户和社区的成员关系
- `user_profiles`：社区展示资料
- `community_topics`：社区话题
- `community_posts`：帖子
- `community_comments`：评论
- `community_reactions`：互动记录，目前用于帖子点赞
- `community_moderation_actions`：管理操作记录
- `media_files`：上传到对象存储的媒体文件记录
- `community_post_media`：帖子和媒体文件关联表

## 旧表删除

历史迁移 `V1` 到 `V7` 不能删除或修改，因为线上 Flyway 已经记录过它们。当前通过 `V8__remove_legacy_modules.sql` 删除旧业务表：

```text
projects
tasks
tags
task_tags
notes
note_blocks
daily_plans
journals
habits
habit_records
project_profiles
project_tech_stacks
github_repositories
portfolio_projects
```

这样既能保证线上迁移历史安全，又能让最新数据库只保留 Ruru 社区需要的数据结构。

## users

保存用户账号。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名，唯一 |
| email | VARCHAR(100) | 邮箱，唯一 |
| password_hash | VARCHAR(255) | 加密后的密码 |
| role | VARCHAR(30) | 全局角色，默认 `MEMBER`，可用 `ADMIN`、`OWNER` |
| status | VARCHAR(30) | 用户状态，默认 `ACTIVE`，禁用为 `DISABLED` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## circles

保存社区空间。当前只有一个默认社区。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 社区名称，当前为 `Ruru 社区` |
| slug | VARCHAR(120) | 访问标识，当前为 `ruru-community` |
| description | VARCHAR(500) | 社区说明 |
| visibility | VARCHAR(40) | 可见范围，当前为 `PUBLIC_REGISTERED` |
| status | VARCHAR(30) | 社区状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## circle_members

保存用户和社区的成员关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(30) | 圈内角色：`OWNER`、`ADMIN`、`MEMBER` |
| status | VARCHAR(30) | 圈内状态：`ACTIVE`、`MUTED`、`DISABLED` |
| joined_at | DATETIME | 加入时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

注册用户会自动加入默认社区。被禁言成员可以浏览内容，但不能发帖、评论、点赞、取消点赞或修改社区资料。`DISABLED` 圈内成员不能访问社区，也不会出现在普通成员列表中。

## user_profiles

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
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## community_topics

保存社区话题。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| name | VARCHAR(80) | 话题名称 |
| slug | VARCHAR(120) | 话题标识 |
| description | VARCHAR(500) | 话题说明 |
| color | VARCHAR(30) | 话题颜色 |
| sort_order | INT | 排序 |
| post_count | INT | 已发布帖子数量 |
| status | VARCHAR(30) | 话题状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

默认话题为 `公告`、`闲聊`、`求助`、`分享`。

## community_posts

保存社区帖子。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| author_id | BIGINT | 作者用户 ID |
| topic_id | BIGINT | 话题 ID，可为空 |
| title | VARCHAR(160) | 标题 |
| content | TEXT | 正文 |
| content_format | VARCHAR(30) | 内容格式，当前为 `TEXT` |
| visibility | VARCHAR(30) | 可见范围，当前为 `CIRCLE` |
| status | VARCHAR(30) | `PUBLISHED`、`HIDDEN`、`DELETED` |
| pinned | BOOLEAN | 是否置顶 |
| comment_count | INT | 评论数 |
| reaction_count | INT | 点赞数 |
| view_count | INT | 浏览数 |
| last_activity_at | DATETIME | 最近活跃时间 |
| deleted_at | DATETIME | 软删除时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

帖子图片不直接存在 `community_posts`，而是通过 `community_post_media` 关联到 `media_files`。

## media_files

保存上传到 Cloudflare R2 的图片元信息。真实图片文件在 R2，数据库不存二进制文件。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| uploader_id | BIGINT | 上传用户 ID |
| storage_provider | VARCHAR(30) | 存储提供商，当前为 `R2` |
| bucket_name | VARCHAR(120) | R2 Bucket 名称 |
| object_key | VARCHAR(500) | R2 对象路径，唯一 |
| original_filename | VARCHAR(255) | 原始文件名 |
| content_type | VARCHAR(120) | MIME 类型 |
| file_type | VARCHAR(30) | 文件类型，当前为 `IMAGE` |
| file_size | BIGINT | 文件大小，单位字节 |
| status | VARCHAR(30) | `PENDING`、`UPLOADED`、`ATTACHED` |
| uploaded_at | DATETIME | 前端确认上传完成时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## community_post_media

保存帖子和图片的多对多关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| post_id | BIGINT | 帖子 ID |
| media_file_id | BIGINT | 媒体文件 ID |
| sort_order | INT | 图片排序 |
| created_at | DATETIME | 创建时间 |

一条帖子最多关联 9 张图片。后期如果拆微服务，`media_files` 会是第一个适合迁出去的表。

## community_comments

保存帖子评论。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| post_id | BIGINT | 帖子 ID |
| author_id | BIGINT | 评论作者 |
| parent_id | BIGINT | 父评论 ID，当前为空 |
| content | TEXT | 评论内容 |
| status | VARCHAR(30) | `PUBLISHED`、`HIDDEN`、`DELETED` |
| reaction_count | INT | 评论互动数，当前保留 |
| deleted_at | DATETIME | 软删除时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## community_reactions

保存通用互动。当前用于帖子点赞。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| target_type | VARCHAR(30) | 目标类型，当前为 `POST` |
| target_id | BIGINT | 目标 ID |
| user_id | BIGINT | 操作用户 |
| reaction_type | VARCHAR(30) | 互动类型，当前为 `LIKE` |
| created_at | DATETIME | 创建时间 |

唯一约束：`circle_id + target_type + target_id + user_id + reaction_type`，保证同一用户不能重复点赞同一目标。

## community_moderation_actions

保存管理员审核操作记录。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| circle_id | BIGINT | 社区 ID |
| admin_user_id | BIGINT | 管理员用户 ID |
| target_type | VARCHAR(30) | 目标类型：`POST`、`COMMENT`、`MEMBER` |
| target_id | BIGINT | 目标 ID |
| action_type | VARCHAR(40) | 操作类型：`HIDE`、`RESTORE`、`MUTE`、`UNMUTE` |
| reason | VARCHAR(500) | 操作原因 |
| created_at | DATETIME | 操作时间 |
