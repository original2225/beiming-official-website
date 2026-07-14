import { Link } from 'react-router-dom'
import { ThumbsUp, MessageSquare } from 'lucide-react'
import { Avatar } from './Avatar'
import { TimeDisplay } from '../data-display/TimeDisplay'
import type { PostSummary } from '../../types/domain'

interface PostCardProps {
  post: PostSummary
  compact?: boolean
}

export function PostCard({ post, compact }: PostCardProps) {
  return (
    <Link
      to={`/community/posts/${post.postId}`}
      className="group flex items-start gap-3 p-3 bg-surface-elevated border border-rim hover:border-indigo rim-light transition-all"
    >
      <Avatar name={post.authorName} size="md" />
      <div className="flex-1 min-w-0">
        <h3 className="text-sm text-text-primary group-hover:text-indigo transition-colors truncate">
          {post.title}
        </h3>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1 text-xs text-text-muted">
          <span className="text-text-secondary">{post.authorName}</span>
          <TimeDisplay iso={post.createdAt} />
          {!compact && (
            <>
              <span className="flex items-center gap-1">
                <ThumbsUp size={12} />
                {post.likes}
              </span>
              <span className="flex items-center gap-1">
                <MessageSquare size={12} />
                {post.comments}
              </span>
            </>
          )}
        </div>
      </div>
      {compact && (
        <div className="hidden sm:flex flex-col items-end gap-1 text-xs text-text-muted">
          <span className="flex items-center gap-1">
            <ThumbsUp size={12} />
            {post.likes}
          </span>
          <span className="flex items-center gap-1">
            <MessageSquare size={12} />
            {post.comments}
          </span>
        </div>
      )}
    </Link>
  )
}
