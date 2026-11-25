package cn.beiming.core;

import java.time.Instant;
import java.util.Map;

/**
 * 跨模块的「考试交接」查询入口。
 *
 * <p>准入链是 onboarding → exam → whitelist → attendance。下游模块（whitelist）原本靠
 * {@code switch (sessionId)} 硬编码十几个测试字符串来假装拿到上游结果，真实考生考完拿到的
 * sessionId 一律落到 {@code default} 分支，返回 {@code 47010 exam handoff unavailable}，
 * 于是真实用户永远建不出申请。
 *
 * <p>这个接口只做一件事：**按 sessionId 查出这场考试的交接快照**。下游据此判断是否通过、
 * 是谁考的、Minecraft 绑定是什么，而不必知道 exam 模块的表结构。
 *
 * <p>放在 core 而不是 exam 包里：与 {@link AuthSessionResolver} 同理——这是模块间契约，
 * 不是 exam 自己的业务实现；各模块源码边界扫描禁止出现 {@code cn.beiming.exam.} 等跨模块包名。
 *
 * <p>刻意不抛异常：下游各有自己的错误码体系。查不到返回 {@code null}，由调用方决定回
 * {@code 47010 exam handoff unavailable} 还是别的。
 */
public interface ExamHandoffResolver {

    /**
     * 按考试会话查出可交给下游的交接快照。
     *
     * @param sessionId 考试会话 ID；为空或不存在一律返回 {@code null}
     * @return 已结束且结果可用的交接快照；会话不存在、未结束、结果不可用都返回 {@code null}
     */
    HandoffSnapshot resolve(String sessionId);

    /**
     * 考试交接快照。
     *
     * <p>{@code result} 是 {@code PASSED} / {@code FAILED} 这类考试结论，调用方自己判断是否放行
     * （下游可能只接受 PASSED，也可能对 RECHECK 有特殊规则）。
     *
     * <p>{@code minecraftBinding} 是这场考试登记时的 Minecraft 绑定快照，可能为 {@code null}
     * ——下游需要自行决定「没有绑定」算不算不合格，而不是由这里替他兜一个假值。
     *
     * @param sessionId          考试会话 ID
     * @param applicationId      这份考试对应的 onboarding 申请 ID
     * @param userId             考生用户 ID
     * @param displayName        考生显示名快照
     * @param reviewDirection    审核方向（GENERAL / REDSTONE / BUILDING / LATE_GAME 等）
     * @param attemptType        尝试类型（FIRST_TIME / RECHECK）
     * @param result             考试结论（PASSED / FAILED / 等）
     * @param scoreSummary       分数摘要，原样透传
     * @param minecraftBinding   Minecraft 绑定快照，可能为 null
     * @param handoffVersion     交接版本号
     * @param passedAt           通过时间，未通过为 null
     */
    record HandoffSnapshot(
            String sessionId,
            String applicationId,
            String userId,
            String displayName,
            String reviewDirection,
            String attemptType,
            String result,
            Map<String, Object> scoreSummary,
            Map<String, Object> minecraftBinding,
            int handoffVersion,
            Instant passedAt) {
    }
}