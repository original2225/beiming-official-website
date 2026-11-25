package cn.beiming.core;

/**
 * 跨模块的「用户注册状态提升」入口。
 *
 * <p>准入链终态失败点：注册产生的 auth 用户是 {@code PENDING_PROFILE}，只有
 * {@code POST /auth/admin/users/{id}} 这类 admin 接口能把它改成 {@code ACTIVE}。
 * 于是白名单审批通过后，成员仍是 PENDING_PROFILE，profile 建档也只能拿到这个错误状态，
 * 表现为「走完全部流程但永远不是成员」。
 *
 * <p>这个契约只做一件事：**把指定用户的 auth 状态提升为 ACTIVE**，供 profile 在建档时调用。
 * 放在 core 而不是 auth 包里：与其他跨模块契约同理，各模块源码边界扫描禁止跨模块包名。
 *
 * <p>刻意不抛异常：失败返回带原因的结果，调用方（profile）据此决定是否把建档判为失败。
 */
public interface UserActivationResolver {

    /**
     * 把用户的 auth 状态提升为 {@code ACTIVE}。
     *
     * <p>幂等：已经是 ACTIVE 直接返回成功。
     *
     * @param userId 目标用户 ID
     * @return 提升结果
     */
    ActivationOutcome activate(String userId);

    /**
     * 提升结果。
     *
     * @param activated   是否处于 ACTIVE；调用方据此决定能否继续建档
     * @param previousStatus 原状态，便于审计
     * @param failureCode 失败码；成功为 {@code null}
     * @param failureReason 失败原因；成功为 {@code null}
     */
    record ActivationOutcome(boolean activated, String previousStatus,
                             String failureCode, String failureReason) {

        public static ActivationOutcome alreadyActive(String previousStatus) {
            return new ActivationOutcome(true, previousStatus, null, null);
        }

        public static ActivationOutcome activated(String previousStatus) {
            return new ActivationOutcome(true, previousStatus, null, null);
        }

        public static ActivationOutcome failed(String previousStatus, String code, String reason) {
            return new ActivationOutcome(false, previousStatus, code, reason);
        }
    }
}