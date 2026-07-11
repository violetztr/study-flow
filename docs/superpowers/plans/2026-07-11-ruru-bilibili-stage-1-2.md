# Ruru Bilibili Stage 1-2 Task List

> 目标：把现在的 Ruru 社区，从“能发图文和视频的小社区”，升级成“有 B站式内容结构、视频投稿、审核、播放详情、用户主页的社区雏形”。

## 总原则

- 先做单体，不急着拆微服务。当前阶段的重点是业务闭环和代码质量。
- 先做 B站路线，不做抖音路线。优先横屏长视频、图文、弹幕、投稿审核、用户主页。
- 先做可用版本，再做高并发版本。Redis、异步转码、MQ、搜索和推荐放到后面阶段逐步接入。
- 每个任务都要能上线、能测试、能解释给面试官听。

## 阶段 1：B站式基础体验

### 任务 1：统一内容模型

状态：已完成。

目标：让后端明确区分图文、视频、直播占位，而不是靠前端临时判断。

要做：

- 给帖子增加内容类型字段：ARTICLE、VIDEO、LIVE。
- 创建帖子时，后端根据附件自动判断类型。
- 视频内容必须有视频文件和封面。
- 图文内容可以有文字和图片。
- 直播先只做占位，不开放真实开播。
- 首页频道按内容类型过滤。

涉及文件：

- `backend/src/main/resources/db/migration`
- `backend/src/main/java/com/studyflow/community/post/CommunityPost.java`
- `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostResponse.java`
- `frontend/src/pages/CircleFeedPage.tsx`
- `frontend/src/components/community/PostCard.tsx`

你要学会：

- 数据库字段如何支撑前端频道。
- 为什么不能只靠前端判断业务类型。
- 后端 DTO 怎么给前端稳定数据结构。

完成标准：

- 首页直播、图文、视频频道的数据来自后端内容类型。
- 视频卡片不会出现在图文频道。
- 图文卡片不会出现在视频频道。

### 任务 2：首页视频流布局升级

状态：已完成。

目标：首页看起来像真正的视频社区，而不是普通帖子列表。

要做：

- 视频卡片展示封面、标题、作者、播放量、评论数、弹幕数、投猪币数。
- 图文卡片展示首图、标题、作者、评论数、点赞数。
- 直播频道显示“直播功能开发中”的干净占位。
- 卡片保持简约风，不要多余说明文字。
- 手机端一列或两列自适应。
- 桌面端多列瀑布/网格布局。

涉及文件：

- `frontend/src/pages/CircleFeedPage.tsx`
- `frontend/src/components/community/PostCard.tsx`
- `frontend/src/index.css`

你要学会：

- B站式信息层级：封面第一，标题第二，作者和数据第三。
- CSS Grid / 响应式布局。
- 为什么列表页只加载封面，不加载视频。

完成标准：

- 打开首页第一眼像内容平台。
- 切换图文、视频、直播频道不卡顿。
- 视频列表只请求封面图，不请求视频文件。

### 任务 3：视频详情页升级

状态：已完成。

目标：视频详情页成为核心观看页。

要做：

- 播放器区域横向占主视觉。
- 右侧显示作者卡片、关注按钮、更多视频。
- 标题下显示播放量、弹幕数、发布时间、话题。
- 播放器下方显示点赞、投猪币、收藏、分享占位。
- 评论区移到播放器下方。
- 弹幕输入框贴近播放器。
- 管理员 ruru 能删除视频、评论、弹幕。

涉及文件：

- `frontend/src/pages/PostDetailPage.tsx`
- `frontend/src/components/community/CommentList.tsx`
- `frontend/src/index.css`
- `backend/src/main/java/com/studyflow/community/moderation`

你要学会：

- 复杂页面如何分区。
- 用户操作按钮如何和后端接口绑定。
- 管理员权限和普通用户权限如何区分。

完成标准：

- 视频详情页结构接近 B站观看页。
- 视频播放、弹幕、评论、点赞、投猪币都能正常使用。
- 管理员可以处理违规内容。

### 任务 4：图文详情页升级

目标：图文内容也像 B站专栏/动态，不是简单文本。

要做：

- 图文详情页展示标题、作者、发布时间、话题。
- 图片支持多图网格。
- 内容区域排版更舒服。
- 底部显示点赞、投猪币、收藏、评论。
- 评论区和视频页保持统一风格。

涉及文件：

- `frontend/src/pages/PostDetailPage.tsx`
- `frontend/src/index.css`

你要学会：

- 同一个详情页如何兼容不同内容类型。
- 组件复用和条件渲染。

完成标准：

- 图文和视频的详情页体验都完整。
- 不会因为没有视频而出现空播放器区域。

### 任务 5：收藏功能

目标：补齐 B站基础互动里的收藏。

要做：

- 新增收藏表 `community_favorites`。
- 新增收藏接口：收藏、取消收藏、我的收藏列表。
- 帖子返回字段增加 `favoriteCount` 和 `favoritedByCurrentUser`。
- 前端卡片和详情页增加收藏按钮。
- 未登录用户点击收藏跳转登录。

涉及文件：

- `backend/src/main/resources/db/migration`
- `backend/src/main/java/com/studyflow/community/favorite`
- `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostResponse.java`
- `frontend/src/api/community.ts`
- `frontend/src/components/community/PostCard.tsx`
- `frontend/src/pages/PostDetailPage.tsx`

你要学会：

- 多对多关系表设计。
- 用户行为状态如何返回给前端。
- 乐观更新和接口回滚。

完成标准：

- 登录用户能收藏和取消收藏。
- 首页和详情页收藏数同步变化。
- 我的收藏后续可以接入用户主页。

### 任务 6：用户主页

目标：让每个用户有自己的空间，像 B站 UP 主主页雏形。

要做：

- 用户主页展示头像占位、昵称、简介、关注数、粉丝数。
- 展示该用户发布的视频和图文。
- 支持关注 / 取消关注。
- 当前用户能进入“我的主页”。
- 管理员 ruru 的主页可以作为站长主页。

涉及文件：

- `backend/src/main/java/com/studyflow/community/member`
- `backend/src/main/java/com/studyflow/community/post/CommunityPostController.java`
- `frontend/src/pages/MemberProfilePage.tsx`
- `frontend/src/api/community.ts`
- `frontend/src/index.css`

你要学会：

- 用户维度的数据聚合。
- 前端路由参数。
- 作者页和内容列表的组合。

完成标准：

- 点击作者名进入用户主页。
- 用户主页能看到作品列表。
- 关注状态正确展示。

## 阶段 2：B站式视频投稿系统

### 任务 7：稿件状态模型

目标：把“发布视频”升级成“投稿审核流程”。

要做：

- 给帖子增加稿件状态：DRAFT、PENDING_REVIEW、PUBLISHED、REJECTED、DELETED。
- 普通用户投稿视频后默认 PENDING_REVIEW。
- 图文可以先直接 PUBLISHED，也可以后期统一审核。
- 管理员审核通过后才进入公开视频列表。
- 被驳回的稿件不公开。

涉及文件：

- `backend/src/main/resources/db/migration`
- `backend/src/main/java/com/studyflow/community/post/CommunityPost.java`
- `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- `backend/src/main/java/com/studyflow/community/moderation`

你要学会：

- 状态机设计。
- 为什么平台不能上传后立即公开视频。
- 审核流和公开流如何分离。

完成标准：

- 普通用户投稿视频后，首页看不到。
- 管理员审核通过后，首页能看到。
- 管理员驳回后，用户能看到原因。

### 任务 8：投稿页面改造

目标：让视频投稿像真正的平台投稿，而不是普通发帖。

要做：

- 发布入口改成“投稿”。
- 投稿页分为：标题、简介、话题、封面、视频上传。
- 视频必须上传封面，可以自动截取，也可以手动换。
- 上传中显示状态。
- 投稿成功后进入“我的稿件”。
- 图文发布保留，但和视频投稿区分开。

涉及文件：

- `frontend/src/pages/CreatePostPage.tsx`
- `frontend/src/components/community/PostComposer.tsx`
- `frontend/src/api/community.ts`
- `frontend/src/index.css`

你要学会：

- 表单拆分。
- 上传流程状态管理。
- 视频投稿和图文发布的业务差异。

完成标准：

- 用户能清楚选择发图文还是投视频。
- 视频投稿后显示“待审核”。
- 不会误以为投稿后已经公开。

### 任务 9：我的稿件页面

目标：用户能管理自己的投稿。

要做：

- 新增“我的稿件”页面。
- 展示稿件列表：封面、标题、状态、创建时间、审核结果。
- 支持查看详情。
- 被驳回显示驳回原因。
- 草稿功能可以先不做，后期再加。

涉及文件：

- `backend/src/main/java/com/studyflow/community/post/CommunityPostController.java`
- `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- `frontend/src/pages/MySubmissionsPage.tsx`
- `frontend/src/App.tsx`
- `frontend/src/api/community.ts`

你要学会：

- “我的数据”和“公开数据”接口分离。
- 状态标签如何驱动 UI。

完成标准：

- 用户能看到自己的待审核、已发布、被驳回视频。
- 别人的未公开视频不能被访问。

### 任务 10：管理员审核后台升级

目标：管理员 ruru 能像平台审核员一样处理稿件。

要做：

- 管理后台增加待审核稿件列表。
- 展示封面、标题、作者、上传时间、视频预览。
- 支持通过。
- 支持驳回并填写原因。
- 支持删除违规稿件。
- 审核通过后视频进入首页。

涉及文件：

- `backend/src/main/java/com/studyflow/community/moderation`
- `backend/src/main/java/com/studyflow/media/MediaController.java`
- `frontend/src/pages/CommunityAdminPage.tsx`
- `frontend/src/api/community.ts`

你要学会：

- 管理端接口和用户端接口分离。
- 审核操作如何记录日志。
- 审核通过如何触发状态变化。

完成标准：

- ruru 登录后能看到所有待审核视频。
- 通过/驳回后用户和首页状态正确变化。

### 任务 11：播放量和观看历史

目标：让视频数据更像平台，而不是普通帖子浏览数。

要做：

- 区分 `viewCount` 和真实播放行为。
- 用户打开详情页不立刻算播放。
- 视频播放超过 10 秒或超过 20% 后计一次播放。
- 同一用户短时间内重复播放不重复计数。
- 未登录用户用 IP 或临时标识粗略防刷。
- 增加观看历史表。

涉及文件：

- `backend/src/main/resources/db/migration`
- `backend/src/main/java/com/studyflow/community/view`
- `frontend/src/pages/PostDetailPage.tsx`
- `frontend/src/api/community.ts`

你要学会：

- 播放量为什么不能等于页面访问量。
- 简单防刷逻辑。
- 前端播放器事件如何上报后端。

完成标准：

- 视频播放达到条件才增加播放量。
- 刷新页面不疯狂增加播放量。
- 用户可以后续查看观看历史。

### 任务 12：弹幕体验升级

目标：让弹幕更接近 B站。

要做：

- 弹幕按时间展示，不只是当前 5 秒简单筛选。
- 支持弹幕开关。
- 支持弹幕颜色。
- 支持管理员删除弹幕。
- 右侧/下方展示弹幕列表。
- 弹幕数量计入视频数据。

涉及文件：

- `backend/src/main/java/com/studyflow/community/danmaku`
- `frontend/src/pages/PostDetailPage.tsx`
- `frontend/src/index.css`

你要学会：

- 时间轴数据结构。
- 播放器事件和弹幕渲染。
- 弹幕审核和删除。

完成标准：

- 播放时弹幕按时间飘过。
- 用户可以关闭弹幕。
- ruru 可以删除不合适弹幕。

## 阶段 1-2 推荐执行顺序

1. 内容类型字段和首页频道数据稳定。
2. 首页视频流布局升级。
3. 视频详情页升级。
4. 收藏功能。
5. 用户主页。
6. 投稿状态模型。
7. 投稿页面改造。
8. 我的稿件页面。
9. 管理员审核后台升级。
10. 播放量和观看历史。
11. 弹幕体验升级。
12. 文档、测试、部署说明同步更新。

## 每做完一批必须验证

- 后端：`cd backend && mvn test`
- 前端：`cd frontend && npm run build`
- 前端检查：`cd frontend && npm run lint`
- 本地提交：`git status --short`
- 线上部署：服务器 `git pull` 后 `sudo docker compose up -d --build`

## 面试讲法

阶段 1-2 做完后，可以这样介绍项目：

> 这是一个基于 Spring Boot、React、MySQL、Redis、Docker 和 Cloudflare R2 的 B站式社区平台。它支持图文和视频内容、视频封面、投稿审核、评论、弹幕、点赞、投币、收藏、关注、用户主页和管理员内容治理。视频文件不经过业务服务器直传，而是通过后端签发预签名 URL 上传到对象存储，列表页只加载封面，详情页才加载视频，后续可以继续扩展 FFmpeg 转码、HLS、消息队列、搜索推荐和直播模块。
