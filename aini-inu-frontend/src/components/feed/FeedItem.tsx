'use client';

import React, { useState } from 'react';
import { Heart, MessageCircle, Share2, ChevronDown, ChevronUp, MoreHorizontal, Send, Trash2, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import {
  type PostResponse,
  type CommentResponse,
  likePost,
  getPost,
  createComment,
  deleteComment,
  deletePost,
} from '@/api/community';
import { useUserStore } from '@/store/useUserStore';
import { toast } from 'sonner';
import Link from 'next/link';

/** Compute Korean relative time string from ISO date */
function formatRelativeTime(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return '방금 전';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}

interface FeedItemProps {
  post: PostResponse;
  currentUserId?: number;
  onDelete?: (postId: number) => void;
  onLikeUpdate?: (postId: number, liked: boolean, likeCount: number) => void;
}

export const FeedItem: React.FC<FeedItemProps> = React.memo(({ post: initialPost, currentUserId, onDelete, onLikeUpdate }) => {
  const [post, setPost] = useState<PostResponse>(initialPost);
  const [isExpanded, setIsExpanded] = useState(false);
  const [isLiked, setIsLiked] = useState(initialPost.liked);
  const [commentText, setCommentText] = useState('');
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [isLoadingComments, setIsLoadingComments] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const resolvedUserId = currentUserId ?? Number(useUserStore.getState().profile?.id);
  const isMyPost = !!(resolvedUserId && post.author?.id === resolvedUserId);

  if (!post) return null;

  const image = Array.isArray(post.imageUrls) && post.imageUrls.length > 0 ? post.imageUrls[0] : '/AINIINU_ROGO_B.png';
  const authorName = post.author?.nickname || '익명';
  const authorAvatar = post.author?.profileImageUrl || '/AINIINU_ROGO_B.png';
  const timeLabel = formatRelativeTime(post.createdAt);

  const profileLink = resolvedUserId && post.author?.id === resolvedUserId ? '/profile' : `/profile/${post.author?.id}`;

  const handleDelete = async () => {
    if (isDeleting) return;
    setIsDeleting(true);
    try {
      await deletePost(post.id);
      toast.success('게시글이 삭제되었습니다.');
      onDelete?.(post.id);
    } catch {
      toast.error('삭제 중 오류가 발생했습니다.');
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const handleShare = (e: React.MouseEvent) => {
    e.stopPropagation();
    const url = typeof window !== 'undefined' ? `${window.location.origin}/feed/${post.id}` : '';
    navigator.clipboard.writeText(url);
    toast.success('게시글 링크가 복사되었습니다.');
  };

  const handleLike = async (e: React.MouseEvent) => {
    e.stopPropagation();
    const prevLiked = isLiked;
    const prevCount = post.likeCount;

    // Optimistic update
    setIsLiked(!isLiked);
    setPost(prev => ({
      ...prev,
      likeCount: isLiked ? prev.likeCount - 1 : prev.likeCount + 1,
      liked: !isLiked,
    }));

    try {
      const res = await likePost(post.id);
      // Sync with server truth
      setIsLiked(res.liked);
      setPost(prev => ({ ...prev, likeCount: res.likeCount, liked: res.liked }));
      onLikeUpdate?.(post.id, res.liked, res.likeCount);
    } catch {
      // Rollback
      setIsLiked(prevLiked);
      setPost(prev => ({ ...prev, likeCount: prevCount, liked: prevLiked }));
      toast.error('좋아요 처리에 실패했습니다.');
    }
  };

  const handleExpand = async () => {
    if (!isExpanded && comments.length === 0 && (post.commentCount || 0) > 0) {
      setIsLoadingComments(true);
      try {
        const detail = await getPost(post.id);
        setComments(detail.comments);
        setPost(prev => ({ ...prev, likeCount: detail.likeCount, commentCount: detail.commentCount, liked: detail.liked }));
        setIsLiked(detail.liked);
      } catch (e) {
        console.error(e);
      } finally {
        setIsLoadingComments(false);
      }
    }
    setIsExpanded(!isExpanded);
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim() || !post?.id) return;

    try {
      const newComment = await createComment(post.id, { content: commentText });
      setComments(prev => [...prev, newComment]);
      setPost(prev => ({ ...prev, commentCount: prev.commentCount + 1 }));
      setCommentText('');
    } catch {
      toast.error('댓글 작성에 실패했습니다.');
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    try {
      await deleteComment(post.id, commentId);
      setComments(prev => prev.filter(c => c.id !== commentId));
      setPost(prev => ({ ...prev, commentCount: prev.commentCount - 1 }));
    } catch {
      toast.error('댓글 삭제에 실패했습니다.');
    }
  };

  const canDeleteComment = (comment: CommentResponse) =>
    !!(resolvedUserId && (comment.author.id === resolvedUserId || post.author.id === resolvedUserId));

  const renderComment = (cmt: CommentResponse) => (
    <div key={cmt.id} className="flex gap-3 text-sm group">
      <img src={cmt.author?.profileImageUrl || '/AINIINU_ROGO_B.png'} className="w-8 h-8 rounded-full object-cover shrink-0" alt="User" />
      <div className="flex-1 space-y-1">
        <div className="flex items-baseline gap-2">
          <span className="font-bold text-navy-900 text-xs">{cmt.author?.nickname || '익명'}</span>
          <span className="text-[10px] text-zinc-300">{cmt.createdAt ? new Date(cmt.createdAt).toLocaleDateString() : ''}</span>
        </div>
        <Typography variant="body" className="text-zinc-600 text-xs leading-relaxed">{cmt.content}</Typography>
      </div>
      {canDeleteComment(cmt) && (
        <button
          onClick={(e) => { e.stopPropagation(); handleCommentDelete(cmt.id); }}
          className="opacity-0 group-hover:opacity-100 text-zinc-300 hover:text-red-400 transition-all shrink-0 self-center"
        >
          <Trash2 size={12} />
        </button>
      )}
    </div>
  );

  return (
    <Card
      onClick={handleExpand}
      interactive
      className={cn(
        "relative transition-all duration-700 ease-[cubic-bezier(0.23,1,0.32,1)] overflow-hidden",
        isExpanded
          ? 'w-full h-[600px] shadow-2xl cursor-default'
          : 'w-full h-[360px] cursor-pointer'
      )}
    >
      {/* 1. BACKGROUND IMAGE */}
      <div className={cn(
        "absolute inset-0 transition-all duration-700 ease-[cubic-bezier(0.23,1,0.32,1)]",
        isExpanded ? 'w-full lg:w-1/2' : 'w-full'
      )}>
        <img src={image} className="w-full h-full object-cover" alt="Post content" />
      </div>

      {/* 2. COLLAPSED BAR (Mobile & Initial View) */}
      <div className={cn(
        "absolute bottom-0 left-0 right-0 px-6 backdrop-blur-3xl transition-all duration-500 ease-[cubic-bezier(0.23,1,0.32,1)] shadow-[0_-10px_40px_rgba(0,0,0,0.03)] z-20 flex flex-col justify-start pt-6 bg-white/90",
        isExpanded ? 'lg:translate-y-full lg:opacity-0 h-[60%]' : 'h-[100px] translate-y-0 opacity-100'
      )}>
        <div className="flex items-center gap-4 w-full" onClick={(e) => !isExpanded && e.stopPropagation()}>
          <div className="shrink-0">
            <Link href={profileLink} className="block w-12 h-12 rounded-[16px] border-2 border-white overflow-hidden shadow-md hover:scale-105 transition-transform" onClick={(e) => e.stopPropagation()}>
              <img src={authorAvatar} className="w-full h-full object-cover" alt="Author" />
            </Link>
          </div>
          <div className="flex-1 min-w-0 space-y-0.5">
            <div className="flex items-center gap-2">
              <Link href={profileLink} className="hover:underline" onClick={(e) => e.stopPropagation()}>
                <Typography variant="body" className="font-black leading-none tracking-tight text-navy-900">{authorName}</Typography>
              </Link>
              <Typography variant="label" className="text-zinc-400 font-bold tracking-[0.2em]">{timeLabel}</Typography>
            </div>
            <Typography variant="body" className={cn(
              "font-bold italic border-l-2 pl-3 text-zinc-600 truncate",
              isExpanded ? 'whitespace-normal mb-2 max-h-20 overflow-y-auto' : 'max-w-xs'
            )} style={{ borderColor: '#F59E0B' }}>
              &quot;{post.content || '내용이 없습니다.'}&quot;
            </Typography>
          </div>

          <div className={cn("shrink-0 lg:hidden transition-transform", isExpanded ? 'rotate-180' : '')}>
             <ChevronDown size={18} className="text-zinc-400" />
          </div>
        </div>

        {/* Mobile Expanded Interaction Area */}
        {isExpanded && (
          <div className="lg:hidden mt-4 pt-4 border-t border-zinc-100 flex flex-col flex-1 overflow-hidden animate-in fade-in slide-in-from-bottom-2" onClick={e => e.stopPropagation()}>
             {/* Comments List (Mobile) */}
             <div className="flex-1 overflow-y-auto space-y-3 mb-4 pr-2">
                {comments.length > 0 ? comments.map((cmt) => (
                  <div key={cmt.id} className="flex gap-2 text-xs">
                    <span className="font-bold text-navy-900">{cmt?.author?.nickname || '익명'}</span>
                    <span className="text-zinc-600">{cmt?.content}</span>
                    {canDeleteComment(cmt) && (
                      <button onClick={() => handleCommentDelete(cmt.id)} className="text-zinc-300 hover:text-red-400 ml-auto">
                        <Trash2 size={10} />
                      </button>
                    )}
                  </div>
                )) : (
                  <div className="text-center py-4 text-zinc-400 text-xs">첫 댓글을 남겨보세요!</div>
                )}
             </div>

             {/* Action Bar */}
             <div className="flex items-center justify-between mt-auto">
                <div className="flex items-center gap-4">
                   <button onClick={handleLike} className={cn("flex items-center gap-2 transition-colors", isLiked ? "text-red-500" : "text-zinc-400 hover:text-red-500")}>
                      <Heart size={20} fill={isLiked ? "currentColor" : "none"} />
                      <Typography variant="h3" className="tracking-tighter text-navy-900">{post.likeCount}</Typography>
                   </button>
                   <div className="flex items-center gap-2 text-zinc-400">
                      <MessageCircle size={20} />
                      <Typography variant="h3" className="tracking-tighter text-navy-900">{post.commentCount}</Typography>
                   </div>
                </div>
             </div>
          </div>
        )}
      </div>

      {/* 3. DESKTOP EXPANDED VIEW LAYER */}
      <div className={cn(
        "absolute inset-0 hidden lg:flex flex-row pointer-events-none",
        isExpanded ? 'pointer-events-auto' : ''
      )}>
        <div className="w-1/2 h-full" onClick={() => setIsExpanded(false)}></div>
        <div className={cn(
          "w-1/2 h-full p-8 flex flex-col justify-between transition-all duration-700 ease-[cubic-bezier(0.23,1,0.32,1)] bg-[#F9FAFB] border-l border-zinc-100 shadow-2xl",
          isExpanded
            ? 'translate-x-0 opacity-100 delay-[100ms]'
            : 'translate-x-full opacity-0'
        )} onClick={e => e.stopPropagation()}>

          {/* Header */}
          <div className="flex items-center justify-between mb-6 shrink-0">
            <div className="flex items-center gap-4">
              <Link href={profileLink} className="block w-12 h-12 rounded-[18px] border-2 border-white overflow-hidden shadow-md hover:scale-105 transition-transform">
                <img src={authorAvatar} alt="Avatar" className="w-full h-full object-cover" />
              </Link>
              <div>
                <Link href={profileLink} className="hover:underline">
                  <Typography variant="h3" className="text-lg leading-none mb-1 text-navy-900">{authorName}</Typography>
                </Link>
                <Typography variant="label" className="text-zinc-400 text-[10px]">{timeLabel}</Typography>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {isMyPost && (
                <button
                  onClick={() => setShowDeleteConfirm(true)}
                  className="w-8 h-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-400 hover:bg-red-50 hover:text-red-500 transition-colors"
                >
                  <MoreHorizontal size={16} />
                </button>
              )}
              <button onClick={() => setIsExpanded(false)} className="w-8 h-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-400 hover:bg-zinc-200">
                <ChevronUp size={16} />
              </button>
            </div>
          </div>

          {/* Delete Confirmation */}
          {showDeleteConfirm && (
            <div className="absolute inset-0 bg-white/95 backdrop-blur-sm z-50 flex flex-col items-center justify-center gap-4 rounded-r-[40px]">
              <div className="w-14 h-14 bg-red-50 rounded-full flex items-center justify-center mb-2">
                <Trash2 size={24} className="text-red-500" />
              </div>
              <Typography variant="h3" className="text-navy-900 text-center">게시글을 삭제할까요?</Typography>
              <Typography variant="label" className="text-zinc-400 text-center text-xs">삭제된 게시글은 복구할 수 없습니다.</Typography>
              <div className="flex gap-3 mt-2">
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  className="px-6 py-2.5 rounded-full bg-zinc-100 text-zinc-600 text-sm font-bold hover:bg-zinc-200 transition-colors flex items-center gap-1.5"
                >
                  <X size={14} /> 취소
                </button>
                <button
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="px-6 py-2.5 rounded-full bg-red-500 text-white text-sm font-bold hover:bg-red-600 transition-colors disabled:opacity-60 flex items-center gap-1.5"
                >
                  <Trash2 size={14} /> {isDeleting ? '삭제 중...' : '삭제'}
                </button>
              </div>
            </div>
          )}

          {/* Content */}
          <div className="space-y-4 mb-6 shrink-0">
            <Typography variant="body" className="font-medium text-zinc-700 leading-relaxed text-sm">
              {post.content}
            </Typography>
          </div>

          {/* Comments Section (Scrollable) */}
          <div className="flex-1 overflow-y-auto border-t border-zinc-100 py-4 space-y-4 no-scrollbar min-h-0">
            {isLoadingComments ? (
              <div className="text-center py-10 text-zinc-300 text-xs">Loading comments...</div>
            ) : comments.length > 0 ? (
              comments.map(renderComment)
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-zinc-300 space-y-2 opacity-50">
                <MessageCircle size={24} />
                <span className="text-xs font-medium">아직 댓글이 없습니다.</span>
              </div>
            )}
          </div>

          {/* Interaction Footer */}
          <div className="mt-4 pt-4 border-t border-zinc-100 shrink-0 space-y-4 bg-white -mx-8 px-8 -mb-8 pb-8">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-6">
                <button onClick={handleLike} className="flex items-center gap-2 group">
                  <Heart size={20} className={cn("transition-colors", isLiked ? "text-red-500 fill-current" : "text-zinc-400 group-hover:text-red-500")} />
                  <Typography variant="h3" className="text-navy-900">{post.likeCount}</Typography>
                </button>
                <div className="flex items-center gap-2 text-zinc-400">
                  <MessageCircle size={20} />
                  <Typography variant="h3" className="tracking-tighter text-navy-900">{post.commentCount}</Typography>
                </div>
              </div>
              <button onClick={handleShare} className="text-zinc-400 hover:text-navy-900"><Share2 size={20} /></button>
            </div>

            {/* Comment Input */}
            <form onSubmit={handleCommentSubmit} className="relative">
              <input
                type="text"
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder="댓글 달기..."
                className="w-full bg-zinc-50 border-none rounded-xl py-3 pl-4 pr-12 text-sm focus:ring-2 ring-amber-500/20 transition-all"
              />
              <button
                type="submit"
                disabled={!commentText.trim()}
                className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 text-amber-500 disabled:text-zinc-300 hover:bg-amber-50 rounded-lg transition-colors"
              >
                <Send size={16} />
              </button>
            </form>
          </div>
        </div>
      </div>
    </Card>
  );
});

FeedItem.displayName = 'FeedItem';
