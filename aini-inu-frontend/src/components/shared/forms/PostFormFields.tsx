'use client';

import React from 'react';
import { Camera, MapPin, Sparkles } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { UserType, PostFormData } from '@/types';

interface PostFormFieldsProps {
  postForm: PostFormData;
  setPostForm: (form: PostFormData) => void;
  userProfile: UserType;
  onImageUpload: (base64: string) => void;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
}

export const PostFormFields: React.FC<PostFormFieldsProps> = ({
  postForm, setPostForm, userProfile, onImageUpload, fileInputRef
}) => {
  const image = postForm.images[0];

  return (
    <div className="w-full flex flex-col md:flex-row h-full">
      {/* Left: Image Upload Area */}
      <div 
        className="md:w-1/2 bg-zinc-50 relative flex flex-col items-center justify-center border-r border-zinc-100 cursor-pointer group min-h-[300px]"
        onClick={() => fileInputRef.current?.click()}
      >
        {image ? (
          <img src={image} className="w-full h-full object-cover" alt="Upload preview" />
        ) : (
          <div className="flex flex-col items-center gap-4 text-zinc-300 group-hover:text-amber-500 transition-colors">
            <div className="w-20 h-20 rounded-[32px] bg-white shadow-sm flex items-center justify-center mb-2">
              <Camera size={40} />
            </div>
            <Typography variant="body" className="font-black text-sm uppercase tracking-widest text-center">Click to upload photo</Typography>
          </div>
        )}
        <input 
          type="file" ref={fileInputRef} className="hidden" accept="image/*" 
          onChange={(e) => {
            const file = e.target.files?.[0]; if (!file) return;
            const reader = new FileReader();
            reader.onloadend = () => onImageUpload(reader.result as string);
            reader.readAsDataURL(file);
          }} 
        />
        {image && (
          <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
            <Typography variant="label" className="font-black">사진 변경하기</Typography>
          </div>
        )}
      </div>

      {/* Right: Content Area */}
      <div className="md:w-1/2 flex flex-col h-full overflow-hidden bg-white">
        <div className="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
          {/* User Info */}
          <div className="flex items-center gap-4">
            <img src={userProfile?.avatar} className="w-12 h-12 rounded-full border border-zinc-100 shadow-sm" alt="Me" />
            <div>
              <Typography variant="body" className="font-black text-navy-900">{userProfile?.nickname}</Typography>
              <div className="flex items-center gap-1.5 text-zinc-400 text-xs font-bold">
                <MapPin size={12} className="text-amber-500" />
                <input 
                  type="text" value={postForm.location} 
                  onChange={(e) => setPostForm({...postForm, location: e.target.value})}
                  className="bg-transparent border-none focus:outline-none focus:text-navy-900 transition-colors w-full"
                />
              </div>
            </div>
          </div>

          {/* Caption Area */}
          <div className="space-y-3">
            <Typography variant="label" className="text-zinc-400 font-black uppercase text-[10px] tracking-widest ml-1">Caption</Typography>
            <textarea 
              className="w-full bg-zinc-50 border border-zinc-100 rounded-[32px] p-8 text-base font-medium focus:outline-none focus:ring-4 ring-amber-500/5 min-h-[200px] resize-none no-scrollbar"
              placeholder="이야기를 들려주세요..."
              value={postForm.caption}
              onChange={(e) => setPostForm({...postForm, caption: e.target.value})}
            />
          </div>

          <div className="flex items-center gap-3 p-4 bg-amber-50/50 rounded-[24px] border border-amber-100/50">
            <Sparkles className="text-amber-500" size={20} />
            <Typography variant="body" className="text-xs text-amber-700 font-bold">
              생생한 사진과 정성 어린 글은 이웃들의 많은 관심을 받아요!
            </Typography>
          </div>
        </div>
      </div>
    </div>
  );
};
