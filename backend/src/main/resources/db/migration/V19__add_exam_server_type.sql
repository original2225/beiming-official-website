-- 补 server_type 列：题/模板的服型分卷此前只存在 Java 对象里，表里没有这一列，
-- 于是重启回读时服型全丢（生电卷变通用卷）。B 方案要求「生电/整合包考核内容不一致」，
-- 服型必须跟着题库一起落库，否则重启即失效。
--
-- 允许 NULL：NULL 表示两服共用（成员公约类本就该两服都考），
-- 与 Java 侧 `serverType == null` 的语义一致，也保证历史数据不被硬改。
ALTER TABLE exam_questions ADD COLUMN IF NOT EXISTS server_type text;
ALTER TABLE exam_paper_templates ADD COLUMN IF NOT EXISTS server_type text;

-- 按服型挑卷/筛题的查询会带上这一列，给个索引避免全表扫
CREATE INDEX IF NOT EXISTS idx_exam_questions_server_type ON exam_questions (server_type);
CREATE INDEX IF NOT EXISTS idx_exam_templates_server_type ON exam_paper_templates (server_type);

-- 白名单申请的 server_type / qq_group_nickname 同样只在 Java 对象里，表里没有。
-- 前者决定「生电强制绑账号、整合包可空」，后者是管理员核对人时的关键线索，
-- 只存内存等于每次重启都要玩家重填、审核时看不到群昵称。
ALTER TABLE whitelist_applications ADD COLUMN IF NOT EXISTS server_type text;
ALTER TABLE whitelist_applications ADD COLUMN IF NOT EXISTS qq_group_nickname text;
