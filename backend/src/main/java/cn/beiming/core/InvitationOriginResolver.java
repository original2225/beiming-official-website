package cn.beiming.core;

/**
 * 邀请码来源查询契约。
 *
 * <p>B 方案（花蝉 2026-11-27 定）：邀请码只用于注册；用<b>管理员签发的邀请码</b>
 * 注册进来的玩家，白名单申请免人工审核；普通玩家码则仍需审核。
 *
 * <p>为什么放在 core：whitelist 模块不允许直接依赖 {@code cn.beiming.auth.*}
 * （{@code compatibilityAndBoundaryContract} 会扫描源码快照并拦下），
 * 跨模块能力一律经 core 契约。
 *
 * <p>返回 null 表示「查不到」——用户不存在、没有可用来源，或 auth 模块未装配。
 * 刻意不抛异常：auth 抖动不该让白名单申请整条链路 500。
 */
public interface InvitationOriginResolver {

    /**
     * 查询某个用户的注册来源邀请码类型。
     *
     * @param userId 站内用户 id（{@code usr_*}）
     * @return {@link Origin}；查不到返回 null
     */
    Origin resolve(String userId);

    /**
     * 注册来源。
     *
     * @param type    邀请码类型：{@code ADMIN} / {@code PLAYER} / 其它自定义值
     * @param admin   {@code true} 表示管理员签发（免审核）
     * @param code   邀请码前缀，便于审计定位（不是明文码）
     */
    record Origin(String type, boolean admin, String code) {
    }
}
