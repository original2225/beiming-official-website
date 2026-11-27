-- V21：放行「管理员后台系统录入」——去掉两条 handoff 版本号的 > 0 约束
--
-- 背景（花蝉 2026-11-27 定）：白名单不走邀请码、不走考核，由管理员在后台直接录入。
--
-- V20 已经放开了 onboarding_application_id 的非空约束，但系统录入还是报 52000，
-- 原因在第二条约束：
--
--   whitelist_applications_exam_handoff_version_check      CHECK (exam_handoff_version > 0)
--   whitelist_applications_onboarding_handoff_version_check CHECK (onboarding_handoff_version > 0)
--
-- 这两个约束假定「每份白名单都必须经历过一次考试交接和一次申请单交接」，
-- 所以版本号必须 >= 1。系统录入没有考试、没有申请单，版本号只能是 0，
-- 被约束直接拒掉。
--
-- 改成 >= 0：0 表示「没有走过对应交接」，是真实语义，不是凑数。
-- 走正常流程的申请仍写真实版本号（>= 1），不受影响。
--
-- 教训：这套 schema 的约束是按「必经考核」写的，准入流程一变就要把约束一起复查，
-- 不能只改一条。第一次只放开 NOT NULL 就以为够了，白跑一轮重建。

ALTER TABLE whitelist_applications
    DROP CONSTRAINT IF EXISTS whitelist_applications_exam_handoff_version_check;

ALTER TABLE whitelist_applications
    ADD CONSTRAINT whitelist_applications_exam_handoff_version_check
        CHECK (exam_handoff_version >= 0);

ALTER TABLE whitelist_applications
    DROP CONSTRAINT IF EXISTS whitelist_applications_onboarding_handoff_version_check;

ALTER TABLE whitelist_applications
    ADD CONSTRAINT whitelist_applications_onboarding_handoff_version_check
        CHECK (onboarding_handoff_version >= 0);