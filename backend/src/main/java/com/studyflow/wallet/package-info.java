/**
 * <strong>wallet</strong> — 猪币钱包模块
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>用户钱包余额</li>
 *   <li>每日登录奖励</li>
 *   <li>投币 / 收币流水</li>
 * </ul>
 *
 * <h3>对外接口</h3>
 * <ul>
 *   <li>{@code PigWalletService} — 被 community.reaction 调用（投币）</li>
 * </ul>
 *
 * <h3>依赖规则</h3>
 * <ul>
 *   <li>仅依赖 user（查用户）</li>
 *   <li>被 community 模块调用</li>
 * </ul>
 *
 * <h3>未来拆服务时</h3>
 * 作为 {@code wallet-service} 独立部署。
 */
package com.studyflow.wallet;
