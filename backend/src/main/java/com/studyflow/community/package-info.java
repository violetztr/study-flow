/**
 * <strong>community</strong> — 内容与社区核心模块
 *
 * <h3>子模块</h3>
 * <table>
 *   <tr><td>{@code post}</td><td>帖子（图文/视频）CRUD、缓存、计数</td></tr>
 *   <tr><td>{@code comment}</td><td>评论</td></tr>
 *   <tr><td>{@code danmaku}</td><td>视频弹幕</td></tr>
 *   <tr><td>{@code reaction}</td><td>点赞、投币、收藏</td></tr>
 *   <tr><td>{@code follow}</td><td>关注关系</td></tr>
 *   <tr><td>{@code member}</td><td>圈子成员、用户资料</td></tr>
 *   <tr><td>{@code topic}</td><td>话题 / 频道</td></tr>
 *   <tr><td>{@code moderation}</td><td>审核 / 管理员治理</td></tr>
 *   <tr><td>{@code view}</td><td>播放量统计 / 观看历史</td></tr>
 *   <tr><td>{@code ranking}</td><td>热门榜单</td></tr>
 *   <tr><td>{@code collection}</td><td>合集</td></tr>
 *   <tr><td>{@code behavior}</td><td>用户行为记录</td></tr>
 *   <tr><td>{@code circle}</td><td>圈子定义</td></tr>
 *   <tr><td>{@code background}</td><td>个人主页背景</td></tr>
 * </table>
 *
 * <h3>依赖规则</h3>
 * <ul>
 *   <li>依赖 user（查用户名、头像）和 media（查附件）— 这是后续拆服务时优先解决的问题</li>
 *   <li>社区内部子模块之间允许互相调用</li>
 * </ul>
 *
 * <h3>未来拆服务时</h3>
 * 拆成 {@code content-service}（帖子、评论、弹幕）和
 * {@code interaction-service}（点赞、投币、收藏、关注）。
 */
package com.studyflow.community;
