/**
 * <strong>live</strong> — 直播模块
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>直播间管理（创建、更新、开播/下播）</li>
 *   <li>实时聊天 / 弹幕消息（WebSocket STOMP）</li>
 *   <li>SRS 回调处理（on_publish / on_unpublish）</li>
 *   <li>在线人数统计（Redis ZSet + 心跳）</li>
 *   <li>僵尸房间自动清理</li>
 * </ul>
 *
 * <h3>对外接口</h3>
 * <ul>
 *   <li>{@code LiveRoomController} — REST: 直播间 CRUD、心跳</li>
 *   <li>{@code LiveMessageController} — REST: 历史消息查询</li>
 *   <li>{@code LiveWebSocketController} — STOMP: 实时消息</li>
 *   <li>{@code SrsCallbackController} — SRS HTTP 回调</li>
 * </ul>
 *
 * <h3>依赖规则</h3>
 * <ul>
 *   <li>依赖 community.member（验证用户是否圈子成员）— 拆服务时需解决</li>
 *   <li>依赖 infrastructure.redis（在线人数）</li>
 *   <li>不依赖 community.post / reaction / moderation</li>
 * </ul>
 *
 * <h3>未来拆服务时</h3>
 * 作为 {@code live-service} 独立部署，保留 WebSocket 长连接能力。
 */
package com.studyflow.live;
