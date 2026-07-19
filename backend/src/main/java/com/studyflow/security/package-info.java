/**
 * <strong>security</strong> — 安全基础设施
 *
 * <h3>内容</h3>
 * <ul>
 *   <li>{@code JwtService} — JWT 解析 / 签发</li>
 *   <li>{@code JwtAuthenticationFilter} — 请求认证过滤器</li>
 *   <li>{@code UserPrincipal} — 认证主体</li>
 * </ul>
 *
 * <p>虽在 security 包下，但实际属于 user 模块的认证子域。
 * 拆 user-service 时移入该服务。</p>
 */
package com.studyflow.security;
