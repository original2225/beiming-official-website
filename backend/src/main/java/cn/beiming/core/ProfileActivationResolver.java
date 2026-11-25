package cn.beiming.core;

/**
 * 跨模块的「成员号变更」查询与通知入口。
 *
 * <p>准入链的最后一步本该是：白名单审批通过 → profile 真实建档 → auth 用户由
 * {@code PENDING_PROFILE} 转 {@code ACTIVE}。可实际上 whitelist 只往
 * {@code profileActivation} 里塞了一串**编造**的字段：
 * <pre>
 * "status", "ACTIVATED", "memberId", "member-" + userId, "profileStatus", "ACTIVE"
 * </pre>
 * 它没有调用 profile，也没有把 auth 用户转成 ACTIVE。后果是真实用户走完全部流程后
 * {@code GET /profile/me} 仍回 {@code 43200 profile not found}、{@code GET /auth/me} 仍是
 * {@code PENDING_PROFILE}，也就是「流程走完但永远不是成员」。
 *
 * <p>这个接口把该做的事收成一个幂等操作，由 profile 实现（它是 member 号的所有者），
 * 供 whitelist 在审批通过时调用。放在 core 而不是任一侧的包里：与其他跨模块契约同理，
 * 各模块源码边界扫描禁止出现跨模块包名。
 *
 * <p>刻意不抛异常：失败返回带 {@code failureCode} 的结果，由调用方决定是否把申请置为
 * {@code APPROVAL_BLOCKED}，而不是让异常穿透把审批整体回滚。
 */
public interface ProfileActivationResolver {

    /**
     * 为已通过白名单的成员真实建档，并把其 auth 状态提升为 {@code ACTIVE}。
     *
     * <p>幂等：同一 {@code userId} 重复调用返回既有档案，不重复创建、不报冲突。
     *
     * @param userId  目标 auth 用户 ID
     * @param reason  审计原因（写进 profile 审计日志）
     * @return 激活结果；用户不存在、auth 状态不允许等失败情形以 {@code status=FAILED} 返回
     */
    ActivationResult activate(String userId, String reason);

    /**
     * 激活结果。
     *
     * @param status         {@code ACTIVATED} / {@code ALREADY_ACTIVE} / {@code FAILED}
     * @param memberId       真实 member 号；失败为 {@code null}
     * @param profileStatus  档案状态；失败为 {@code null}
     * @param failureCode    失败码；成功为 {@code null}
     * @param failureReason  失败原因；成功为 {@code null}
     */
    record ActivationResult(String status, String memberId, String profileStatus,
                            String failureCode, String failureReason) {

        public static ActivationResult activated(String memberId, String profileStatus) {
            return new ActivationResult("ACTIVATED", memberId, profileStatus, null, null);
        }

        public static ActivationResult alreadyActive(String memberId, String profileStatus) {
            return new ActivationResult("ALREADY_ACTIVE", memberId, profileStatus, null, null);
        }

        public static ActivationResult failed(String code, String reason) {
            return new ActivationResult("FAILED", null, null, code, reason);
        }
    }
}