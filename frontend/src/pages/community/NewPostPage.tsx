/**
 * 发帖页 — 创建新帖子
 */

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Send } from 'lucide-react'
import { PageLayout } from '../../components/layout/PageLayout'
import { FormField } from '../../components/forms/FormField'
import { TagChip } from '../../components/community/TagChip'
import { MarkdownPreview } from '../../components/community/MarkdownPreview'
import { InkBanner } from '../../components/community/InkBanner'
import { useRequest } from '../../hooks/useRequest'
import { createPost, getBoards } from '../../api/modules/community'
import { ROUTES } from '../../constants/routes'
import type { BoardView } from '../../types/view-models'

const EXAMPLE_TAGS = ['攻略', '建筑', '生存', '红石', '交易', '活动']

export function NewPostPage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [boardId, setBoardId] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const { data: boards, loading, run } = useRequest<BoardView[]>()

  useEffect(() => {
    run(() => getBoards())
  }, [run])

  const toggleTag = (tag: string) => {
    setTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !body.trim() || !boardId) return
    setSubmitting(true)
    try {
      await createPost({ title, body, boardId, tags })
      navigate(ROUTES.BOARDS)
    } catch {
      setError('发帖失败')
    }
    setSubmitting(false)
  }

  return (
    <PageLayout variant="account">
      <InkBanner
        title="发布新帖"
        seal="帖"
        subtitle="分享想法、提问或展示你的作品。"
      />

      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="panel-glass p-4 flex flex-col gap-4">
          {error && (
            <div className="bg-cinnabar/20 border border-cinnabar p-2 text-sm text-cinnabar text-center">
              {error}
            </div>
          )}

          <FormField label="标题">
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="给你的帖子起个标题"
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-2 text-sm outline-none focus:border-indigo w-full"
              required
            />
          </FormField>

          <FormField label="板块">
            <select
              value={boardId}
              onChange={(e) => setBoardId(e.target.value)}
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-2 text-sm outline-none focus:border-indigo w-full"
              disabled={loading || !boards?.length}
              required
            >
              <option value="" disabled>
                选择板块
              </option>
              {boards?.map((b: BoardView) => (
                <option key={b.boardId} value={b.boardId}>
                  {b.name}
                </option>
              ))}
              {!boards?.length && <option value="">暂无板块</option>}
            </select>
          </FormField>

          <div>
            <label className="text-xs text-text-muted font-display uppercase tracking-wider">
              标签
            </label>
            <div className="flex flex-wrap gap-2 mt-1">
              {EXAMPLE_TAGS.map((tag) => (
                <TagChip
                  key={tag}
                  label={tag}
                  active={tags.includes(tag)}
                  onClick={() => toggleTag(tag)}
                />
              ))}
            </div>
          </div>

          <FormField label="内容">
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="支持 Markdown 格式..."
              rows={12}
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-2 text-sm outline-none focus:border-indigo w-full resize-y"
              required
            />
          </FormField>

          <button
            type="submit"
            disabled={submitting || !boardId}
            className="btn-ink text-sm inline-flex items-center gap-2 self-start"
          >
            <Send size={14} />
            {submitting ? '发布中...' : '发布'}
          </button>
        </div>

        <MarkdownPreview title={title} body={body} />
      </form>
    </PageLayout>
  )
}
