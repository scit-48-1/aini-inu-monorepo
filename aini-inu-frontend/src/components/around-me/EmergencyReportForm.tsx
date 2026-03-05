'use client';

import React, { useState } from 'react';
import { Camera, X, Loader2, Sparkles, Megaphone, PlusCircle, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { analyzeDogImage } from '@/services/geminiService';
import { DOG_BREEDS, DogBreed } from '@/constants/dogBreeds';

type EmergencyMode = 'LOST' | 'FOUND';

interface EmergencyReportFormProps {
  isSubmitting: boolean;
  isSuccess: boolean;
  onSubmit: (image: string, result: any, memo: string, mode: EmergencyMode) => void;
  optimizeImage: (base64: string) => Promise<string>;
}

export const EmergencyReportForm: React.FC<EmergencyReportFormProps> = ({
  isSubmitting,
  isSuccess,
  onSubmit,
  optimizeImage
}) => {
  const [mode, setMode] = useState<EmergencyMode>('LOST');
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [analysisResult, setAnalysisResult] = useState<any>(null);
  const [isAnalyzing, setAnalyzing] = useState(false);
  const [memo, setMemo] = useState('');
  const [breedSuggestions, setBreedSuggestions] = useState<DogBreed[]>([]);
  const [showBreedSuggestions, setShowBreedSuggestions] = useState(false);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]; if (!file) return;
    const reader = new FileReader();
    reader.onloadend = async () => {
      const optimized = await optimizeImage(reader.result as string);
      setImagePreview(optimized);
    };
    reader.readAsDataURL(file);
  };

  const startAIAnalysis = async () => {
    if (!imagePreview) return;
    setAnalyzing(true);
    try {
      const result = await analyzeDogImage(imagePreview);
      if (result) setAnalysisResult(result);
    } catch (e) { console.error(e); } finally { setAnalyzing(false); }
  };

  const handleBreedChange = (val: string) => {
    if (!analysisResult) return;
    const currentBreed = analysisResult.breed && typeof analysisResult.breed === 'object' ? analysisResult.breed : { en: val, ko: val, jp: val };
    setAnalysisResult({ ...analysisResult, breed: { ...currentBreed, ko: val } });
    if (val.trim().length > 0) {
      const filtered = DOG_BREEDS.filter(b => (b.ko && b.ko.includes(val)) || (b.en && b.en.toLowerCase().includes(val.toLowerCase()))).slice(0, 5);
      setBreedSuggestions(filtered); 
      setShowBreedSuggestions(true);
    } else { 
      setBreedSuggestions([]); 
      setShowBreedSuggestions(false); 
    }
  };

  const handleSubmit = () => {
    if (!imagePreview || !analysisResult) return;
    onSubmit(imagePreview, analysisResult, memo, mode);
  };

  return (
    <div className="space-y-6 pb-20 animate-in slide-in-from-right-10 duration-700">
      <Card className="p-10 bg-white shadow-2xl rounded-[48px] space-y-10 relative overflow-hidden min-h-[500px] justify-center flex flex-col items-center text-center">
        <div className="flex bg-white p-2 rounded-3xl border border-card-border shadow-sm w-full">
          <button onClick={() => { setMode('LOST'); setImagePreview(null); setAnalysisResult(null); }} className={cn("flex-1 py-4 rounded-2xl text-sm font-black transition-all", mode === 'LOST' ? 'bg-amber-500 text-black shadow-md' : 'text-black')}>내 아이 실종</button>
          <button onClick={() => { setMode('FOUND'); setImagePreview(null); setAnalysisResult(null); }} className={cn("flex-1 py-4 rounded-2xl text-sm font-black transition-all", mode === 'FOUND' ? 'bg-amber-500 text-black shadow-md' : 'text-black')}>유기견 제보</button>
        </div>

        {!imagePreview ? (
          <div className="py-10 flex flex-col items-center space-y-8">
            <label className="w-32 h-32 rounded-[40px] flex items-center justify-center shadow-xl bg-white border border-card-border hover:bg-zinc-50 transition-all relative group cursor-pointer active:scale-95">
              <div className="absolute inset-0 rounded-[40px] animate-ping opacity-10 bg-amber-500"></div>
              <Camera size={40} className="text-black relative group-hover:scale-110 transition-transform" />
              <input type="file" className="hidden" accept="image/*" onChange={handleFileUpload} />
            </label>
            <div className="space-y-3">
              <Typography variant="h2" className="text-2xl text-black">{mode === 'LOST' ? '실종 강아지 등록' : '유기견 제보'}</Typography>
              <Typography variant="body" className="text-zinc-400 text-base leading-relaxed">
                {mode === 'LOST' ? '아이의 특징이 잘 드러난 사진을 올려주시면 즉시 대조를 시작합니다.' : '아이를 마주친 장소와 특징을 제보해 주시면 즉시 알림이 전파됩니다.'}
              </Typography>
            </div>
          </div>
        ) : (
          <div className="space-y-8 animate-in fade-in duration-500 w-full text-left">
            <div className="relative mx-auto max-w-[280px]">
              <img src={imagePreview} className="w-full aspect-square object-cover rounded-[40px] shadow-xl border-4 border-white" alt="Emergency" />
              {isAnalyzing && (
                <div className="absolute inset-0 bg-black/40 backdrop-blur-sm rounded-[40px] flex flex-col items-center justify-center text-white space-y-3">
                  <Loader2 className="animate-spin" size={48} />
                  <Typography variant="label" className="text-white font-black text-sm">AI 분석 중...</Typography>
                </div>
              )}
              {!isSubmitting && <button onClick={() => setImagePreview(null)} className="absolute -top-4 -right-4 w-10 h-10 bg-black text-white rounded-full flex items-center justify-center shadow-lg"><X size={20} /></button>}
            </div>

            {!analysisResult && !isAnalyzing && (
              <Button variant="primary" fullWidth size="lg" className="shadow-lg gap-3 py-5 bg-navy-900 text-white border-none" onClick={startAIAnalysis}>
                <Sparkles size={20} className="text-amber-500" />
                <Typography variant="body" className="font-black text-white">AI 정밀 분석 시작</Typography>
              </Button>
            )}

            {analysisResult && (
              <div className="space-y-8 relative">
                <div className={cn("p-6 rounded-[32px] space-y-6 border-2 border-amber-500/20 bg-white relative")}>
                  <div className="flex flex-col border-b border-card-border pb-4">
                    <Typography variant="label" className="text-zinc-400 text-[10px]">품종 (직접 수정 가능)</Typography>
                    <input type="text" value={analysisResult.breed?.ko || ''} onChange={(e) => handleBreedChange(e.target.value)} className="bg-transparent font-black text-2xl text-black focus:outline-none focus:text-amber-600 transition-colors w-full" />
                    {showBreedSuggestions && breedSuggestions.length > 0 && (
                      <div className="absolute top-24 left-6 right-6 bg-white rounded-2xl border border-card-border shadow-2xl z-[2000] overflow-hidden">
                        {breedSuggestions.map((b, i) => (
                          <button key={i} onClick={() => { setAnalysisResult({ ...analysisResult, breed: b }); setShowBreedSuggestions(false); }} className="w-full px-5 py-3 text-left hover:bg-amber-50 border-b border-zinc-50 last:border-none transition-colors">
                            <Typography variant="body" className="font-bold text-navy-900">{b.ko} ({b.en})</Typography>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2.5">
                    {(analysisResult.features || []).map((f: any, i: number) => (
                      <Badge key={i} variant="default" className="bg-white text-black border-card-border text-sm px-4 py-1.5">
                        #{typeof f === 'string' ? f : f.ko || f.en}
                      </Badge>
                    ))}
                  </div>
                  <textarea value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="아이를 마주친 장소와 특징을 제보해 주세요." className="w-full h-28 bg-background border border-card-border rounded-2xl p-4 text-base font-medium focus:outline-none focus:ring-2 ring-amber-500/10 resize-none no-scrollbar" />
                </div>
                <Button variant="secondary" fullWidth size="lg" className="shadow-lg gap-3 text-base py-5" onClick={handleSubmit} disabled={isSubmitting}>
                  {isSubmitting ? <Loader2 className="animate-spin" size={20} /> : (mode === 'FOUND' ? <Megaphone size={20} /> : <PlusCircle size={20} />)}
                  <Typography variant="body" className="font-black">{isSubmitting ? '접수 중...' : (mode === 'FOUND' ? '주변 전파' : '실종 등록')}</Typography>
                </Button>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};
