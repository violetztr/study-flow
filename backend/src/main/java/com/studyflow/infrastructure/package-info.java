/**
 * <strong>infrastructure</strong> — 基础设施层
 *
 * <h3>子模块</h3>
 * <table>
 *   <tr><td>{@code redis}</td><td>Redis 缓存封装（CacheService, Key 规范）</td></tr>
 *   <tr><td>{@code ratelimit}</td><td>接口限流（拦截器、规则匹配）</td></tr>
 *   <tr><td>{@code logging}</td><td>请求 traceId（MDC Filter）</td></tr>
 * </table>
 *
 * <h3>规则</h3>
 * <ul>
 *   <li>不依赖任何业务模块（user/community/media/live/wallet）</li>
 *   <li>只被业务模块依赖</li>
 * </ul>
 */
package com.studyflow.infrastructure;
