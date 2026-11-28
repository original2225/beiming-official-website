/**
 * 入服须知书页 —— 登录 / 注册两张卡的下沿共用。
 *
 * 花蝉 2026-11-28 定的：
 *   · 点卡片下沿那一条，下半截像便利贴 / 书页一样翻开，露出「入服须知」；
 *     上半（表单）保持不动。登录卡和注册卡都要有。
 *   · 内容分层口径：服务器守则 = 所有服务器玩家都要遵守的通用条约；
 *     服规 = 某一个 / 某一类服务器专用的规定（生电服服规只管原版生存服，
 *     愚者服服规只管愚者服）。不同服务器页面放各自的服规与玩家指南，
 *     总页面（登录 / 注册这里）放「服务器守则 + 入服指南」。
 *
 * 正文来源（花蝉 2026-11-28 给的官方文档，不自行加条款）：
 *   · docs.beiming.games/1-基本信息/1.2-服务器守则.html
 *   · docs.beiming.games/2-原版群组服/2.2-生电服服规.html
 *   · docs/rules/愚者服服规.txt（愚者服原文）
 * 完整条文以官方文档为准，故每组都带 docUrl，底部可直接跳过去。
 */

import { useEffect, useState } from 'react'
import { RULE_GROUPS, ROTATING_NOTES } from '../../constants/rulesContent'

export function EntryNoticeSheet() {
  // 书页是否翻开
  const [open, setOpen] = useState(false)
  // 轮播到第几条注意事项
  const [noteIndex, setNoteIndex] = useState(0)

  // 翻开时每 4 秒换一条注意事项；合上就停，别在后台空转。
  useEffect(() => {
    if (!open) return
    const timer = window.setInterval(() => {
      setNoteIndex((i) => (i + 1) % ROTATING_NOTES.length)
    }, 4000)
    return () => window.clearInterval(timer)
  }, [open])

  const note = ROTATING_NOTES[noteIndex]

  return (
    <div className={`bm-notice ${open ? 'is-open' : ''}`}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="bm-notice-tab"
        aria-expanded={open}
        aria-controls="bm-notice-body"
      >
        <span className="bm-notice-tab-left">
          <span className="bm-notice-tab-title">入服须知</span>
          <span className="bm-notice-tab-sub">服务器守则 · 各服服规 · 入服指南</span>
        </span>
        {/* 合上时箭头朝下（还有内容可展开），翻开后翻上来（可收起） */}
        <span className={`bm-notice-chevron ${open ? 'open' : ''}`} aria-hidden="true">⌄</span>
      </button>

      <div id="bm-notice-body" className="bm-notice-body" role="region" aria-label="入服须知">
        <div className="bm-notice-clip">
          <div className="bm-notice-inner">
            <div className="bm-note-ticker" key={noteIndex}>
              <span className="bm-note-tag">{note.tag}</span>
              <span className="bm-note-text">{note.text}</span>
            </div>

            <p className="bm-notice-scope">
              <strong>服务器守则</strong>是所有服务器玩家都要遵守的通用条约；<strong>服规</strong>是某个（某类）服务器专用的——
              生电服服规只管原版生电服、愚者服服规只管愚者服。各服务器页面里有对应的服规和玩家指南。
            </p>

            {RULE_GROUPS.map((group) => (
              <div key={group.key} className="bm-rule-group">
                <div className="bm-rule-head">
                  <span className={`bm-rule-scope scope-${group.key}`}>{group.scope}</span>
                  <span className="bm-rule-title">{group.title}</span>
                </div>
                <ul className="bm-rule-list">
                  {group.items.map((item, i) => (
                    <li key={i}>{item}</li>
                  ))}
                </ul>
                <a
                  className="bm-rule-doc"
                  href={group.docUrl}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  查看完整条文 →
                </a>
              </div>
            ))}

            <p className="bm-notice-foot">完整条文以官方文档为准。</p>
          </div>
        </div>
      </div>
    </div>
  )
}