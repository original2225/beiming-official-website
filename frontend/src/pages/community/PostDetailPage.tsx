/**
 * 帖子详情页 — 内容、评论区、点赞、收藏
 */

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, MessageSquare, Send, Star, ThumbsUp } from 'lucide-react'
import type { ComponentType } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Avatar } from '../../components/community/Avatar'
import { InkBanner } from '../../components/community/InkBanner'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { useAuth } from '../../hooks/useAuth'
import { getPost, getComments, createComment, likePost, unlikePost, favoritePost, unfavoritePost } from '../../api/modules/community'
import { ROUTES } from '../../constants/routes'
import type { CommentView, PostDetailView } from '../../types/view-models'
import type { PageResult } from '../../types/api'

interface ActionButtonProps {
  active: boolean
  onClick: () => void
  icon: ComponentType<{ size?: number; className?: string }>
  label: string
  activeLabel: string
}

function ActionButton({ active, onClick, icon: Icon, label, activeLabel }: ActionButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`btn-ink-ghost inline-flex items-center gap-2 text-xs ${active ? 'btn-ink-ghost-active' : ''}`}
    >
      <Icon size={16} className={active ? 'fill-current' : ''} />
      {active ? activeLabel : label}
    </button>
  )
}

function CommentItem({ comment }: { comment: CommentView }) {
  return (
    <div className="flex gap-3">
      <div className="flex flex-col items-center gap-1">
        <Avatar name={comment.authorName} size="sm" />
        <div className="w-0.5 flex-1 bg-ink-600/30 min-h-[1rem]" />
      </div>
      <div className="flex-1 pb-4 border-b border-rim last:border-b-0">
        <div className="flex items-center gap-3 text-xs text-text-muted mb-1">
          <span className="text-text-secondary">{comment.authorName}</span>
          <TimeDisplay iso={comment.createdAt} />
        </div>
        <p className="text-sm text-text-secondary leading-relaxed whitespace-pre-wrap">{comment.body}</p>
      </div>
    </div>
  )
}

export function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>()
  const { isAuthenticated } = useAuth()
  const { data: post, loading, run } = useRequest<PostDetailView>()
  const { data: comments, run: runComments } = useRequest<CommentView[]>()
  const [comment, setComment] = useState('')
  const [liked, setLiked] = useState(false)
  const [favorited, setFavorited] = useState(false)

  useEffect(() => {
    if (postId) {
      run(() => getPost(postId))
      runComments(() =>
        getComments(postId, {}).then(
          (d) => (d as PageResult<CommentView> | null)?.items ?? []
        )
      )
    }
  }, [run, runComments, postId])

  const handleLike = async () => {
    try {
      if (liked) {
        await unlikePost(postId!)
      } else {
        await likePost(postId!)
      }
      setLiked(!liked)
    } catch {
      /* ignore */
    }
  }

  const handleFavorite = async () => {
    try {
      if (favorited) {
        await unfavoritePost(postId!)
      } else {
        await favoritePost(postId!)
      }
      setFavorited(!favorited)
    } catch {
      /* ignore */
    }
  }

  const handleComment = async () => {
    if (!comment.trim()) return
    try {
      await createComment(postId!, { body: comment })
      setComment('')
      runComments(() =>
        getComments(postId!, {}).then(
          (d) => (d as PageResult<CommentView> | null)?.items ?? []
        )
      )
    } catch {
      /* ignore */
    }
  }

  return (
    <PageLayout variant="public">
      <Link
        to={ROUTES.BOARDS}
        className="inline-flex items-center gap-1 text-xs text-ochre hover:text-indigo mb-4"
      >
        <ChevronLeft size={14} />
        返回社区
      </Link>

      {loading && <LoadingState />}
      {!loading && !post && <ErrorState message="帖子不存在" />}

      {post && (
        <article>
          <InkBanner
            title={post.title}
            seal="文"
            subtitle={
              <span className="flex items-center gap-2 text-sm text-text-secondary">
                <Avatar name={post.authorName} size="sm" />
                <span>{post.authorName}</span>
                <TimeDisplay iso={post.createdAt} />
              </span>
            }
          />

          <div className="panel-glass p-5 border-l-4 border-l-indigo mb-6">
            <div className="text-base text-text-secondary leading-[1.7] whitespace-pre-wrap">
              {post.body ?? post.content ?? '暂无内容'}
            </div>
          </div>

          {isAuthenticated && (
            <div className="flex flex-wrap items-center gap-3 mb-6">
              <ActionButton
                active={liked}
                onClick={handleLike}
                icon={ThumbsUp}
                label="点赞"
                activeLabel="已赞"
              />
              <ActionButton
                active={favorited}
                onClick={handleFavorite}
                icon={Star}
                label="收藏"
                activeLabel="已收藏"
              />
            </div>
          )}

          <div className="flex items-center gap-3 mb-2">
            <MessageSquare size={18} className="text-ochre" />
            <h2 className="font-display text-lg text-ochre tracking-wider">评论</h2>
            <Seal text="评" />
          </div>
          <InkStroke className="mb-6" />

          {isAuthenticated && (
            <div className="flex gap-2 mb-6">
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="写下你的评论..."
                rows={2}
                className="bg-surface border border-ink-600 text-text-primary px-3 py-2 text-sm flex-1 outline-none focus:border-indigo resize-none"
              />
              <button
                onClick={handleComment}
                className="btn-ink text-xs inline-flex items-center gap-2 self-stretch"
              >
                <Send size={14} />
                发送
              </button>
            </div>
          )}

          {!comments?.length && (
            <EmptyState
              icon={<MessageSquare size={32} />}
              title="暂无评论"
              text="还没有人发言，快来抢沙发吧！"
            />
          )}

          <div className="flex flex-col">
            {comments?.map((c: CommentView) => (
              <CommentItem key={c.commentId} comment={c} />
            ))}
          </div>
        </article>
      )}
    </PageLayout>
  )
}
