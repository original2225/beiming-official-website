/**
 * 发帖页 — 创建新帖子
 */

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { FormField } from '../../components/forms/FormField'
import { createPost } from '../../api/modules/community'
import { ROUTES } from '../../constants/routes'

export function NewPostPage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      await createPost({ title, body })
      navigate(ROUTES.BOARDS)
    } catch {
      setError('发帖失败')
    }
    setSubmitting(false)
  }

  return (
    <PageLayout variant="account">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">发帖</h1>
      <form onSubmit={handleSubmit} className="panel-mc p-4 max-w-2xl flex flex-col gap-4">
        {error && <div className="bg-mc-redstone/20 border border-mc-redstone p-2 text-sm text-mc-redstone text-center">{error}</div>}
        <FormField label="标题">
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} className="bg-surface-dark border border-mc-stone text-text-primary px-3 py-2 text-sm outline-none focus:border-mc-grass w-full" required />
        </FormField>
        <FormField label="内容">
          <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={8} className="bg-surface-dark border border-mc-stone text-text-primary px-3 py-2 text-sm outline-none focus:border-mc-grass w-full resize-y" required />
        </FormField>
        <button type="submit" disabled={submitting} className="btn-mc text-sm self-start">{submitting ? '发布中...' : '发布'}</button>
      </form>
    </PageLayout>
  )
}
