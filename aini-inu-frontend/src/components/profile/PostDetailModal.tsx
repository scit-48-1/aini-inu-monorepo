'use client';

import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, Heart, MessageCircle, MoreHorizontal, Pencil, Trash2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { useUserStore } from '@/store/useUserStore';
import { updatePost, deletePost } from '@/api/community';
import type { PostResponse } from '@/api/community';
import { toast } from 'sonner';

interface PostDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  post: PostResponse | null;
  onUpdated?: (post: PostResponse) => void;
  onDeleted?: (postId: number) => void;
}

export const PostDetailModal: React.FC<PostDetailModalProps> = ({
  isOpen,
  onClose,
  post,
  onUpdated,
  onDeleted,
}) => {
  const profile = useUserStore((s) => s.profile);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [showMenu, setShowMenu] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  if (!isOpen || !post) return null;

  const isOwner = post.author.id === Number(profile?.id);

  const handleEdit = () => {
    setEditContent(post.content);
    setIsEditing(true);
  };

  const handleSave = async () => {
    if (!editContent.trim()) {
      toast.error('내용을 입력해주세요.');
      return;
    }
    try {
      const updatedPost = await updatePost(post.id, { content: editContent });
      toast.success('포스트가 수정되었습니다.');
      onUpdated?.(updatedPost);
      setIsEditing(false);
    } catch {
      toast.error('포스트 수정에 실패했습니다.');
    }
  };

  const handleDelete = async () => {
    try {
      await deletePost(post.id);
      toast.success('포스트가 삭제되었습니다.');
      onDeleted?.(post.id);
      onClose();
    } catch {
      toast.error('포스트 삭제에 실패했습니다.');
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-[3000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 animate-in fade-in duration-300" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <Card className="w-full max-w-4xl bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none rounded-[48px] overflow-hidden flex flex-col md:flex-row max-h-[90vh]">
        <div className="md:w-1/2 bg-zinc-100 relative shrink-0 min-h-[300px] md:min-h-full">
          <img src={post.imageUrls?.[0]} className="w-full h-full object-cover" alt="Post content" />
          <button onClick={onClose} className="absolute top-6 left-6 md:hidden w-10 h-10 bg-black/20 backdrop-blur-md text-white rounded-full flex items-center justify-center"><X size={20} /></button>
        </div>
        <div className="md:w-1/2 flex flex-col h-full overflow-hidden bg-white relative">
          <div className="p-6 border-b border-zinc-50 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-3">
              <img src={post.author.profileImageUrl || '/AINIINU_ROGO_B.png'} className="w-10 h-10 rounded-full border border-zinc-100" alt="Author" />
              <div>
                <Typography variant="body" className="font-black text-navy-900 leading-none">{post.author.nickname}</Typography>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {isOwner && !isEditing && (
                <div className="relative">
                  <button
                    onClick={() => setShowMenu(!showMenu)}
                    className="p-2 text-zinc-400 hover:text-navy-900 transition-colors"
                  >
                    <MoreHorizontal size={20} />
                  </button>
                  {showMenu && (
                    <>
                      <div className="fixed inset-0 z-40" onClick={() => setShowMenu(false)} />
                      <div className="absolute right-0 top-full mt-1 bg-white rounded-xl shadow-lg border border-zinc-100 py-1 z-50 min-w-[120px]">
                        <button
                          onClick={() => { setShowMenu(false); handleEdit(); }}
                          className="w-full px-4 py-2.5 text-left text-sm font-medium text-zinc-700 hover:bg-zinc-50 flex items-center gap-2"
                        >
                          <Pencil size={14} /> 수정
                        </button>
                        <button
                          onClick={() => { setShowMenu(false); setShowDeleteConfirm(true); }}
                          className="w-full px-4 py-2.5 text-left text-sm font-medium text-red-500 hover:bg-red-50 flex items-center gap-2"
                        >
                          <Trash2 size={14} /> 삭제
                        </button>
                      </div>
                    </>
                  )}
                </div>
              )}
              <button onClick={onClose} className="hidden md:flex p-2 text-zinc-300 hover:text-navy-900 transition-colors"><X size={24} /></button>
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
            <div className="space-y-4">
              {isEditing ? (
                <div className="space-y-4">
                  <textarea className="w-full bg-zinc-50 border border-zinc-100 rounded-2xl p-4 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/10 min-h-[120px] resize-none" value={editContent} onChange={(e) => setEditContent(e.target.value)} placeholder="글 내용을 입력해주세요..." />
                  <div className="flex gap-2">
                    <Button variant="primary" size="sm" fullWidth onClick={handleSave}>저장하기</Button>
                    <Button variant="outline" size="sm" onClick={() => setIsEditing(false)}>취소</Button>
                  </div>
                </div>
              ) : (
                <>
                  <Typography variant="body" className="text-zinc-700 leading-relaxed text-sm whitespace-pre-line">{post.content}</Typography>
                  <Typography variant="label" className="text-zinc-300 text-[10px] font-bold">{post.createdAt}</Typography>
                </>
              )}
            </div>
            {!isEditing && (
              <div className="pt-6 border-t border-zinc-50 space-y-4">
                <div className="flex items-center gap-4">
                  <button className="text-navy-900 hover:text-red-500 transition-colors"><Heart size={24} /></button>
                  <button className="text-navy-900 hover:text-amber-500 transition-colors"><MessageCircle size={24} /></button>
                </div>
                <Typography variant="body" className="text-sm font-black text-navy-900">좋아요 {post.likeCount}개</Typography>
              </div>
            )}
          </div>
          {showDeleteConfirm && (
            <div className="absolute inset-0 bg-white/95 backdrop-blur-sm z-50 flex flex-col items-center justify-center gap-4 rounded-3xl">
              <div className="w-14 h-14 bg-red-50 rounded-full flex items-center justify-center">
                <Trash2 size={24} className="text-red-500" />
              </div>
              <Typography variant="h3" className="text-navy-900">게시글을 삭제할까요?</Typography>
              <Typography variant="label" className="text-zinc-400 text-xs">삭제된 게시글은 복구할 수 없습니다.</Typography>
              <div className="flex gap-3 mt-2">
                <button onClick={() => setShowDeleteConfirm(false)} className="px-6 py-2.5 rounded-full bg-zinc-100 text-zinc-600 text-sm font-bold hover:bg-zinc-200">취소</button>
                <button onClick={handleDelete} className="px-6 py-2.5 rounded-full bg-red-500 text-white text-sm font-bold hover:bg-red-600">삭제</button>
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>,
    document.body
  );
};
