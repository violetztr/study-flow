/**
 * <strong>user</strong> — 用户与认证模块
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>用户注册、登录、JWT 令牌签发与解析</li>
 *   <li>用户基本信息（username, password）持久化</li>
 *   <li>认证过滤器与安全配置</li>
 * </ul>
 *
 * <h3>对外接口</h3>
 * <ul>
 *   <li>{@code AuthController} — 注册 / 登录 REST 接口</li>
 *   <li>{@code UserController} — 用户查询</li>
 *   <li>{@code JwtService} — 令牌签发 / 验证</li>
 *   <li>{@code UserMapper} — 用户表 CRUD</li>
 * </ul>
 *
 * <h3>依赖规则</h3>
 * <ul>
 *   <li>不依赖 community / media / live / wallet</li>
 *   <li>不依赖 infrastructure（安全模块是被 infrastructure 调用的）</li>
 * </ul>
 *
 * <h3>未来拆服务时</h3>
 * 可直接作为 {@code user-service} 独立部署，
 * 只需提供用户查询和认证接口即可。
 */
package com.studyflow.user;
