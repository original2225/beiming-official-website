/**
 * 入服须知「下半区」内容 —— 由 AuthBook 控制绕下边框翻动，这里只负责内容。
 *
 * 花蝉 2026-11-28 定的交互：
 *   整张登录卡上下都能翻。上半（表单）绕上边框翻，下半（这一块）绕最下方的边框翻，
 *   中间一条虚线分隔，上下边框各有一排活页环。下方这块平时是一条「流动提示」，
 *   翻起来之后才是完整的守则 / 服规 / 入服指南。
 *
 * 内容分层口径：服务器守则 = 全体通用条约；服规 = 某个（某类）服务器专用
 * （生电服服规只原生电服、愚者服服规只原愚者服）。各服页面放各自服规与玩家指南。
 *
 * 正文来源（花蝉给的官方文档，未加条款）：
 *   docs.beiming.games 1.2 服务器守则 / 2.2 生电服服规 / docs/rules 愚者服服规。
 * 完整条文以 docUrl 为准。
 */

import { useEffect, useState } from 'react'
import { RULE_GROUPS, ROTATING_NOTES } from '../../constants/rulesContent'

interface EntryNoticeSheetProps {
  /** 下半区是否已翻开（翻起来看完整守则） */
  expanded: boolean
}

export function EntryNoticeSheet({ expanded }: EntryNoticeSheetProps) {
  // 轮播到第几条注意事项
  const [noteIndex, setNoteIndex] = useState(0)

  // 折叠状态下轮播流动提示，4 秒一条。翻开看条文时停住，别跟阅读抢注意力。
  useEffect(() => {
    if (expanded) return
    const timer = window.setInterval(() => {
      setNoteIndex((i) => (i + 1) % ROTATING_NOTES.length)
    }, 4000)
    return () => window.clearInterval(timer)
  }, [expanded])

  const note = ROTATING_NOTES[noteIndex]

  return (
    <div className="bm-notice-inner">
      {/* 折叠时：一条流动提示；翻开后也留在顶上当引子 */}
      <div className="bm-note-ticker" key={noteIndex}>
        <span className="bm-note-tag">{note.tag}</span>
        <span className="bm-note-text">{note.text}</span>
      </div>

      {expanded && (
        <>
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
        </>
      )}
    </div>
  )
}
