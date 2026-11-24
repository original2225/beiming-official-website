-- 会话重启后恢复用：明文 token 需要落库，仅存 token_hash 无法在进程重启后还原 token。
--
-- 安全取舍：token 是随机 UUID（ses_ + UUIDv4），不可猜测；库里已存密码哈希，
-- 存 token 明文不降低该表被拖库后的实际风险级别。
-- 生产环境若要进一步收紧，可改为存 AES 加密后的 token_value，读取时用环境变量里的密钥解密。
ALTER TABLE auth_sessions ADD COLUMN IF NOT EXISTS token_value TEXT;

CREATE INDEX IF NOT EXISTS idx_auth_sessions_token_value ON auth_sessions(token_value);