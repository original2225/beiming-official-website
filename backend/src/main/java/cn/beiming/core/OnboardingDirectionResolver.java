package cn.beiming.core;

/**
 * 跨模块的「入服申请方向」查询入口。
 *
 * <p>exam 出卷必须按考生在 onboarding 里**实际选择**的审核方向挑模板。此前 exam 靠
 * {@code switch (applicationId)} 硬编码判断方向——只有字面值 {@code "app-general"} 能识别成
 * GENERAL，真实申请 ID（如 {@code app-2001}）全部落到 {@code default} 分支被当成 REDSTONE。
 * 结果是：任何选了 GENERAL 的真实用户都被发到红石卷（卷里有问答题，不人工阅卷就永远过不了）。
 *
 * <p>这个接口只做一件事：**按 onboarding 申请 ID 查它当前登记的审核方向**。exam 据此选模板，
 * 不必知道 onboarding 的表结构或状态机。
 *
 * <p>放在 core 而不是任一侧的包里：这是模块间契约，各模块源码边界扫描禁止出现跨模块包名
 * （见 {@link ExamHandoffResolver} 同款说明）。
 *
 * <p>刻意不抛异常：查不到返回 {@code null}（申请不存在 / 未选方向 / 未就绪），
 * 由调用方决定是回 43910 还是别的错误码。
 */
public interface OnboardingDirectionResolver {

    /**
     * 查出该 onboarding 申请登记的审核方向。
     *
     * @param applicationId onboarding 申请 ID；为空或不存在返回 {@code null}
     * @return {@code GENERAL} / {@code REDSTONE} 等方向值；申请不存在或尚未选择方向返回 {@code null}
     */
    String resolveReviewDirection(String applicationId);
}