/**
 * <strong>media</strong> — 媒体存储与上传模块
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>图片 / 视频上传（Cloudflare R2 预签名 URL）</li>
 *   <li>媒体文件元数据管理（{@code MediaFile}）</li>
 *   <li>帖子关联媒体（{@code CommunityPostMedia}）</li>
 * </ul>
 *
 * <h3>对外接口</h3>
 * <ul>
 *   <li>{@code MediaController} — 上传准备 / 完成确认 REST 接口</li>
 *   <li>{@code MediaService} — 预签名 URL 签发</li>
 * </ul>
 *
 * <h3>依赖规则</h3>
 * <ul>
 *   <li>依赖 R2 配置（基础设施级，非业务模块）</li>
 *   <li>被 community 模块引用（帖子关联媒体ID）</li>
 *   <li>不依赖 community / wallet / live</li>
 * </ul>
 *
 * <h3>未来拆服务时</h3>
 * 作为 {@code media-service} 独立部署，负责上传和转码链路的入口。
 */
package com.studyflow.media;
