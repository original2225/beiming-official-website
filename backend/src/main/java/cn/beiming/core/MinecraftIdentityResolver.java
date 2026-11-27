package cn.beiming.core;

/**
 * 跨模块的「游戏昵称 → 平台身份」解析入口。
 *
 * <p>白名单申请需要验证玩家填的游戏昵称在所选平台上真实存在（例如 LittleSkin 上有没有这个人）。
 * 这段查询逻辑住在本项目 auth 包里，而各模块的源码边界扫描禁止 whitelist 出现
 * {@code cn.beiming.auth.} 前缀，所以只在 core 定义契约、由 auth 侧实现。
 *
 * <p>与 {@link ExamHandoffResolver}、{@link AuthSessionResolver} 同一套路：
 * 这是模块间契约，不是某一方的业务实现。
 *
 * <p><b>刻意不抛异常</b>：第三方站点抖动、站点挂掉、名字不存在，一律返回 {@code null}。
 * 由调用方决定是回「查不到该昵称」还是「平台暂不可用」，这里不替它兜假值。
 */
public interface MinecraftIdentityResolver {

    /** 支持的游戏平台。顺序即业务优先级（正版 &gt; 外置 &gt; 自建皮肤站 &gt; 离线）。 */
    enum Platform {
        MOJANG,
        LITTLESKIN,
        BEIMING,
        OFFLINE
    }

    /**
     * 解析结果。
     *
     * @param name     平台上实际登记的昵称（按平台返回的规范大小写）
     * @param uuid     32 位小写无连字符 UUID
     * @param platform 来源平台
     */
    record Identity(String name, String uuid, Platform platform) {
    }

    /**
     * 按昵称 + 平台解析身份。
     *
     * @param rawName     玩家填写的昵称，两端空格会被裁掉
     * @param rawPlatform 平台名（大小写不敏感）；无法识别时返回 {@code null}
     * @return 解析出的身份；昵称不合法、平台不认识、平台查不到都返回 {@code null}
     */
    Identity resolve(String rawName, String rawPlatform);
}