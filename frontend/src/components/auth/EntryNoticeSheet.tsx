/**
 * 登录卡下方的「提示条」—— 一条一条随机播放的入服提示。
 *
 * 花蝉 2026-11-28 定稿：这块不再展开、不再挂入服须知条文，
 * 只做随机轮播；而且它和上方的主活页（登录/注册表单）是【分离的两块纸】，
 * 不是连在一起的一整张。
 *
 * 条文正文仍以官方文档为准（docUrl 见 constants/rulesContent.ts）。
 */

import { useEffect, useState } from 'react'
import { ROTATING_NOTES } from '../../constants/rulesContent'

/** 每条停留时长（ms） */
const NOTE_INTERVAL = 4200

export function EntryNoticeSheet() {
  // 起始条随机，避免每次打开都从第一条开始
  const [noteIndex, setNoteIndex] = useState(() => Math.floor(Math.random() * ROTATING_NOTES.length))

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNoteIndex((i) => {
        if (ROTATING_NOTES.length <= 1) return i
        // 随机且不重复当前这条
        let next = i
        while (next === i) next = Math.floor(Math.random() * ROTATING_NOTES.length)
        return next
      })
    }, NOTE_INTERVAL)
    return () => window.clearInterval(timer)
  }, [])

  const note = ROTATING_NOTES[noteIndex]
  if (!note) return null

  return (
    <div className="bm-note-inner" role="status" aria-live="polite">
      {/* key 用序号让每次换条都重新播淡入 */}
      <div className="bm-note-ticker" key={noteIndex}>
        <span className="bm-note-tag">{note.tag}</span>
        <span className="bm-note-text">{note.text}</span>
      </div>
    </div>
  )
}
