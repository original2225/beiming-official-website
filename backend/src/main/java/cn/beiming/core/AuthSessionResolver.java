package cn.beiming.core;

import java.util.List;

/**
 * 跨模块的会话校验入口。
 *
 * <p>业务模块（ops-control 等）不能直接依赖 auth 的 {@code AuthStore}——它是包私有实现，
 * 且把会话、用户、邀请码的内存结构都暴露在外面。这里只对外提供「拿 Authorization 头换主体」这一件事。
 *
 * <p>放在 core 而不是 auth 包里：这是模块间的**接口契约**，不是 auth 自己的业务实现。
 * 各业务模块的源码边界扫描禁止出现 {@code cn.beiming.auth.} 等跨模块包名（防止直读对方数据表），
 * 但允许依赖 core 里的公共契约。
 *
 * <p>刻意不抛异常：调用方各自有自己的错误码体系（ops 用 OpsException，auth 用 ApiException）。
 * 校验失败返回 {@code null}，由调用方决定回 41000 unauthenticated 还是 41003 bad token。
 */
public interface AuthSessionResolver {

    /**
     * 校验 {@code Authorization: Bearer <token>} 并解析出会话主体。
     *
     * @param authorization 原始请求头，可为 {@code null}
     * @return 有效会话的主体；无头、格式错、会话不存在、已撤销、已过期一律返回 {@code null}
     */
    ResolvedSession resolve(String authorization);

    /**
     * 会话主体。{@code role} 取 OWNER/ADMIN/HELPER/USER 中优先级最高的一个，
     * {@code permissions} 是该用户显式授予的能力（可能为空，按角色下发默认能力是调用方的决定）。
     */
    record ResolvedSession(String userId, String username, String role, List<String> permissions) {
    }
}