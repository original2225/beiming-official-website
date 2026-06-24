/**
 * 帖子详情页 — 内容、评论区、点赞、收藏
 */

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { useAuth } from '../../hooks/useAuth'
import { getPost, getComments, createComment, likePost, unlikePost, favoritePost, unfavoritePost } from '../../api/modules/community'
import { ROUTES } from '../../constants/routes'

export function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>()
  const { isAuthenticated } = useAuth()
  const { data: post, loading, run } = useRequest<any>()
  const { data: comments, run: runComments } = useRequest<any[]>()
  const [comment, setComment] = useState('')
  const [liked, setLiked] = useState(false)
  const [favorited, setFavorited] = useState(false)

  useEffect(() => { if (postId) { run(() => getPost(postId)); runComments(() => getComments(postId, {}).then((d: any) => d?.items ?? [])) } }, [run, runComments, postId])

  const handleLike = async () => {
    try { if (liked) { await unlikePost(postId!) } else { await likePost(postId!) }; setLiked(!liked) } catch { /* ignore */ }
  }

  const handleFavorite = async () => {
    try { if (favorited) { await unfavoritePost(postId!) } else { await favoritePost(postId!) }; setFavorited(!favorited) } catch { /* ignore */ }
  }

  const handleComment = async () => {
    if (!comment.trim()) return
    try { await createComment(postId!, { body: comment }); setComment(''); runComments(() => getComments(postId!, {}).then((d: any) => d?.items ?? [])) } catch { /* ignore */ }
  }

  return (
    <PageLayout variant="public">
      <Link to={ROUTES.BOARDS} className="text-xs text-mc-gold hover:text-mc-grass mb-4 inline-block">← 返回社区</Link>
      {loading && <LoadingState />}
      {!loading && !post && <ErrorState message="帖子不存在" />}
      {post && (
        <article>
          <h1 className="font-minecraft text-2xl text-mc-grass mb-2">{post.title}</h1>
          <div className="flex items-center gap-4 text-xs text-text-muted mb-4">
            <span>{post.authorName}</span>
            <span><TimeDisplay iso={post.createdAt} /></span>
          </div>
          <div className="text-text-secondary text-sm leading-relaxed mb-4 panel-mc p-4">{post.body ?? post.content ?? '暂无内容'}</div>

          {isAuthenticated && (
            <div className="flex gap-3 mb-6">
              <button onClick={handleLike} className={`btn-mc text-xs px-3 py-1 ${liked ? 'bg-mc-gold' : ''}`}>👍 {liked ? '已赞' : '点赞'}</button>
              <button onClick={handleFavorite} className={`btn-mc text-xs px-3 py-1 ${favorited ? 'bg-mc-gold' : ''}`}>⭐ {favorited ? '已收藏' : '收藏'}</button>
            </div>
          )}

          <h2 className="font-minecraft text-lg text-mc-gold mb-3">评论</h2>
          {isAuthenticated && (
            <div className="flex gap-2 mb-4">
              <input type="text" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="写评论..." className="bg-surface border border-mc-stone text-text-primary px-3 py-1 text-sm flex-1 outline-none focus:border-mc-grass" />
              <button onClick={handleComment} className="btn-mc text-xs">发送</button>
            </div>
          )}
          {!comments?.length && <EmptyState text="暂无评论" />}
          {comments?.map((c: any) => (
            <div key={c.commentId} className="panel-mc p-3 mb-1">
              <div className="flex items-center gap-2 text-xs text-text-muted mb-1"><span>{c.authorName}</span><TimeDisplay iso={c.createdAt} /></div>
              <p className="text-sm text-text-secondary">{c.body}</p>
            </div>
          ))}
        </article>
      )}
    </PageLayout>
  )
}
