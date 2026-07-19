/**
 * <strong>config</strong> — 全局配置
 *
 * <h3>内容</h3>
 * <ul>
 *   <li>{@code SecurityConfig} — Spring Security 配置</li>
 *   <li>{@code WebSocketConfig} — STOMP / WebSocket 端点</li>
 *   <li>{@code JwtHandshakeInterceptor} — WebSocket 握手认证</li>
 *   <li>{@code WebMvcConfig} — CORS 等 Web 配置</li>
 *   <li>{@code OpenApiConfig} — Knife4j 接口文档</li>
 * </ul>
 *
 * <p>不拆服务——属于单体应用胶水层，拆服务后每个服务自带。</p>
 */
package com.studyflow.config;
