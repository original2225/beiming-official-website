-- V20：放行「管理员后台系统录入」的白名单申请
--
-- 背景（花蝉 2026-11-27 定）：白名单不走邀请码，也不走考核，
-- 由管理员在官网后台直接把玩家录进白名单。
--
-- onboarding_application_id 原本 NOT NULL，意思是「每份白名单申请都必须
-- 挂在一份 onboarding 申请单下」。系统录入没有这道前置流程，也没有对应的
-- 申请单可挂 —— 编一个假单号就是伪造数据。
--
-- 因此这里放开非空约束：NULL 表示「这份白名单不是从 onboarding 流程来的」。
-- 走正常流程的申请仍会写入真实单号，不受影响。

ALTER TABLE whitelist_applications
    ALTER COLUMN onboarding_application_id DROP NOT NULL;