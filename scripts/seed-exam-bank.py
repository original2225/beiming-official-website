#!/usr/bin/env python3
"""
把 docs/rules/exam-question-bank.seed.json 灌进北冥后端题库。

为什么用 HTTP 接口而不是直接写库：
  题目/模板的读写路径都有 audit、幂等、evidence 记录，直插表会绕过这些，
  后端也认不到（内存索引 + 持久化两条线）。走接口才是唯一没坑的灌入方式。

用法：
  python scripts/seed-exam-bank.py --token <ADMIN_TOKEN> [--base http://127.0.0.1:8135] [--dry-run]

要点（踩过的坑）：
  - 题目创建只建 DRAFT，必须再 PATCH .../publish 才进试卷池；模板同理。
  - 每个写请求都要 idempotencyKey，重复跑不会灌两遍（按 key 幂等）。
  - 模板的 questionRules 用 tags 限定题目；tags 里没题会报 43915「题库不足」。
"""
import argparse
import json
import sys
import urllib.error
import urllib.request

API_PREFIX = "/api/v1/exam"


def call(base, path, token, body=None, method="POST"):
    url = base.rstrip("/") + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "Bearer " + token)
    if data:
        req.add_header("Content-Type", "application/json; charset=utf-8")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def field(resp, name):
    """从接口响应里取 data.<name>。

    resp 的类型在静态检查里会被推断成多种形状（list/dict 都可能），
    这里显式收口：不是 dict 就当缺失处理，别让上游崩溃掩盖真正的问题。
    """
    data = resp.get("data")
    if not isinstance(data, dict):
        return None
    return data.get(name)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--token", required=True, help="ADMIN 或 OWNER 的访问令牌")
    ap.add_argument("--base", default="http://127.0.0.1:8135")
    ap.add_argument("--seed", default="docs/rules/exam-question-bank.seed.json")
    ap.add_argument("--dry-run", action="store_true", help="只打印将要发的请求，不真发")
    args = ap.parse_args()

    with open(args.seed, encoding="utf-8") as f:
        seed = json.load(f)

    created_questions = []
    failures = 0

    for i, q in enumerate(seed["questions"]):
        payload = dict(q)
        payload["reason"] = "初始化准入考核题库（《生电服服规》/《愚者服服规》取证）"
        payload["idempotencyKey"] = f"seed-q-{i:03d}"
        if args.dry_run:
            print(f"[dry] POST {API_PREFIX}/admin/question-bank/questions  {q['stem'][:40]}")
            continue
        status, resp = call(args.base, API_PREFIX + "/admin/question-bank/questions", args.token, payload)
        if status not in (200, 201) or resp.get("code") != 0:
            print(f"✗ 题目 {i} 创建失败 {status} {resp.get('code')} {resp.get('message')}")
            failures += 1
            continue
        qid = field(resp, "questionId")
        created_questions.append(qid)

        # 题目建出来是 DRAFT，必须发布才进池
        status2, resp2 = call(args.base, f"{API_PREFIX}/admin/question-bank/questions/{qid}",
                              args.token,
                              {"status": "ACTIVE", "reason": "初始化题库发布", "idempotencyKey": f"seed-q-pub-{i:03d}"},
                              method="PATCH")
        if status2 not in (200, 201) or resp2.get("code") != 0:
            print(f"✗ 题目 {i} 发布失败 {status2} {resp2.get('code')} {resp2.get('message')}")
            failures += 1
        else:
            print(f"✓ 题目 {i} -> {qid}")

    for i, t in enumerate(seed["templates"]):
        payload = dict(t)
        payload.pop("_待办", None)
        payload["reason"] = "初始化准入考核试卷模板"
        payload["idempotencyKey"] = f"seed-t-{i:03d}"
        if args.dry_run:
            print(f"[dry] POST {API_PREFIX}/admin/paper-templates  {t['name']}")
            continue
        status, resp = call(args.base, API_PREFIX + "/admin/paper-templates", args.token, payload)
        if status not in (200, 201) or resp.get("code") != 0:
            print(f"✗ 模板 {i} 创建失败 {status} {resp.get('code')} {resp.get('message')}")
            failures += 1
            continue
        tid = field(resp, "templateId")
        status2, resp2 = call(args.base, f"{API_PREFIX}/admin/paper-templates/{tid}/publish",
                              args.token,
                              {"reason": "初始化模板发布", "idempotencyKey": f"seed-t-pub-{i:03d}"},
                              method="PATCH")
        if status2 not in (200, 201) or resp2.get("code") != 0:
            print(f"✗ 模板 {i} 发布失败 {status2} {resp2.get('code')} {resp2.get('message')}")
            failures += 1
        else:
            print(f"✓ 模板 {i} -> {tid}")

    print(f"\n完成：题目 {len(created_questions)} 道，失败 {failures} 处")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())