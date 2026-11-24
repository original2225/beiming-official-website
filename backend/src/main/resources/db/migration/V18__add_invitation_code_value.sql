-- 邀请码校验按明文查 invitationIdByCode，库里只存 code_hash 会让重启后所有邀请码失效（43101 invitation not found）。
-- code_value 与 auth_sessions.token_value 同一取舍：随机码本身不可猜，存明文不降低该表被拖库后的风险级别。
-- 若要收紧，可改为加密存储，用环境变量密钥解密。
ALTER TABLE auth_invitations ADD COLUMN IF NOT EXISTS code_value TEXT;

CREATE INDEX IF NOT EXISTS idx_auth_invitations_code_value ON auth_invitations(code_value);