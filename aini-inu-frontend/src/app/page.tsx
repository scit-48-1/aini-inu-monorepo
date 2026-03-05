'use client';

import React, { useEffect, useState, useRef } from 'react';
import { 
  ArrowRight, MapPin, Heart, ChevronDown, Sparkles, 
  CheckCircle2, TrendingUp, Shield, MessageSquare, Play, Star, 
  Fingerprint, ShieldAlert, X, Map as MapIcon, Loader2, ShieldCheck,
  Target, Users, UserPlus
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { useTheme } from 'next-themes';

type FlowStep = 'LANDING' | 'ADDRESS' | 'SCANNING' | 'RESULTS';

export default function LandingPage() {
  const router = useRouter();
  const [scrollY, setScrollY] = useState(0);
  const [flowStep, setFlowStep] = useState<FlowStep>('LANDING');
  const [address, setAddress] = useState({ city: '', district: '', detail: '' });
  const discoveryRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const { setTheme } = useTheme();

  useEffect(() => {
    setTheme('light');
    const container = containerRef.current;
    if (!container) return;

    const handleScroll = () => {
      setScrollY(container.scrollTop);
    };

    container.addEventListener('scroll', handleScroll, { passive: true });
    return () => container.removeEventListener('scroll', handleScroll);
  }, [setTheme]);

  const startDiscovery = () => {
    setFlowStep('ADDRESS');
  };

  const handleSearch = () => {
    if (!address.city || !address.district) return;
    setFlowStep('SCANNING');
    setTimeout(() => {
      setFlowStep('RESULTS');
    }, 2500);
  };

  const MOCK_NEIGHBORS = [
    { name: "보리", breed: "토이푸들", img: "https://picsum.photos/id/1025/200/200", temp: 38.2 },
    { name: "초코", breed: "시바견", img: "https://picsum.photos/id/65/200/200", temp: 37.5 },
    { name: "뭉치", breed: "비숑 프리제", img: "https://picsum.photos/id/1012/200/200", temp: 39.1 },
    { name: "루이", breed: "골든리트리버", img: "https://picsum.photos/id/1062/200/200", temp: 36.8 },
  ];

  const scrollToSection = (id: string) => {
    const section = document.getElementById(id);
    if (section) {
      section.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="bg-[#FDFCF8] text-navy-900 font-sans selection:bg-amber-100 relative h-screen overflow-hidden" data-theme="light">
      
      {/* Navigation (Floating - OUTSIDE of snap container) */}
      <nav className={cn(
        "fixed top-0 left-0 right-0 h-20 flex items-center justify-between px-6 md:px-10 lg:px-20 z-[100] transition-all duration-500",
        scrollY > 50 ? 'bg-white/90 backdrop-blur-md shadow-sm h-16' : 'bg-transparent'
      )}>
        <div className="text-xl md:text-2xl font-black tracking-tighter flex items-center gap-2 shrink-0 cursor-pointer" onClick={() => containerRef.current?.scrollTo({top: 0, behavior: 'smooth'})}>
          <div className="w-10 h-10 flex items-center justify-center">
            <img src="/AINIINU_ROGO_B.png" alt="Logo" className="w-full h-full object-contain" />
          </div>
          <Typography variant="body" className="text-xl md:text-2xl font-black tracking-tighter lowercase leading-none">
            aini<span className="text-amber-500">inu</span>
          </Typography>
        </div>
        <div className="hidden lg:flex items-center gap-10">
          <button onClick={() => scrollToSection('mate')}><Typography variant="label" className="text-zinc-500 hover:text-amber-500 transition-colors">Walk Mate</Typography></button>
          <button onClick={() => scrollToSection('radar')}><Typography variant="label" className="text-zinc-500 hover:text-amber-500 transition-colors">Radar</Typography></button>
          <button onClick={() => scrollToSection('community')}><Typography variant="label" className="text-zinc-500 hover:text-amber-500 transition-colors">Social</Typography></button>
          <button onClick={() => scrollToSection('emergency')}><Typography variant="label" className="text-zinc-500 hover:text-amber-500 transition-colors">SOS</Typography></button>
        </div>
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="lg" onClick={() => router.push('/login')}>Login</Button>
          <Button variant="primary" size="lg" onClick={() => router.push('/signup')}>Sign Up</Button>
        </div>
      </nav>

      {/* Pure Snap Container (ONLY snap-start direct children) */}
      <div ref={containerRef} className="h-full overflow-y-auto snap-y snap-mandatory no-scrollbar relative">
        
        {/* 1. Hero Section */}
        <section id="hero" className="relative h-screen flex flex-col items-center justify-center overflow-hidden snap-start shrink-0">
          <div 
            className="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&q=80&w=2000')] bg-cover bg-center"
            style={{ 
              backgroundPosition: 'center 30%',
              transform: `translateY(${scrollY * 0.4}px)` 
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-b from-white/30 via-white/60 to-[#FDFCF8]"></div>
          
          <div className="relative z-10 text-center px-6 max-w-6xl mx-auto space-y-10 pt-32 animate-in fade-in slide-in-from-bottom-10 duration-1000">
            <Typography variant="h1" className="text-navy-900">
              산책의 <br /> <span className="italic text-amber-500">진심</span>을 <br /> 잇다.
            </Typography>
            <Typography variant="body" className="text-xl md:text-2xl text-zinc-500 max-w-2xl mx-auto">
              반려견을 위한 가장 똑똑한 산책 메이트 레이더, <br />
              아이니이누가 당신의 동네를 더 즐겁고 안전하게 바꿉니다.
            </Typography>
            <div className="pt-6 flex justify-center">
              <Button variant="primary" size="xl" onClick={startDiscovery} className="group gap-3">
                내 주변 친구 탐색하기 <ArrowRight className="group-hover:translate-x-2 transition-transform" />
              </Button>
            </div>
          </div>

          <div className="absolute bottom-10 left-0 right-0 flex justify-center pointer-events-none">
            <div className="flex flex-col items-center gap-2 animate-bounce opacity-40 text-navy-900">
               <Typography variant="label">Scroll Down</Typography>
               <ChevronDown size={20} />
            </div>
          </div>
        </section>

        {/* 2. Feature: AI Walk Mate */}
        <section id="mate" className="h-screen py-20 px-6 bg-white overflow-hidden snap-start flex items-center shrink-0">
          <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-24 items-center">
             <div className="space-y-12">
                <div className="space-y-4">
                  <Typography variant="label" className="text-amber-500">AI-Powered Matching</Typography>
                  <Typography variant="serif" className="text-5xl md:text-7xl text-navy-900 not-italic">
                    맞지 않는 친구와 <br /> 긴장되는 산책은 <br /> <span className="text-zinc-200 italic">이제 그만.</span>
                  </Typography>
                </div>
                <Typography variant="body" className="text-lg text-zinc-400">
                  강아지마다 성향은 모두 다릅니다. 에너지가 넘치는 대형견인지, 낯을 가리는 소형견인지 AI가 분석합니다. 
                  나이, 견종, 사회성 지수(SQ)를 종합해 우리 아이와 가장 잘 맞는 '베프' 후보를 1:1로 매칭해드립니다.
                </Typography>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-left">
                   <Card className="p-8 bg-zinc-50 border-zinc-100 hover:shadow-xl">
                      <Heart size={28} className="text-amber-500 mb-4" />
                      <Typography variant="h3" className="text-lg mb-2">성향 정밀 매칭</Typography>
                      <Typography variant="body" className="text-xs text-zinc-400 font-bold">에너지 레벨과 사회성을 분석해 공격성 없는 안전한 만남을 주선합니다.</Typography>
                   </Card>
                   <Card className="p-8 bg-zinc-50 border-zinc-100 hover:shadow-xl">
                      <Target size={28} className="text-amber-500 mb-4" />
                      <Typography variant="h3" className="text-lg mb-2">실시간 메이트 호출</Typography>
                      <Typography variant="body" className="text-xs text-zinc-400 font-bold">지금 당장 산책하고 싶을 때, 근처 이웃에게 번개 산책을 제안해보세요.</Typography>
                   </Card>
                </div>
             </div>
             <div className="relative">
                <div className="absolute inset-0 bg-amber-500/10 rounded-[80px] blur-3xl"></div>
                <img src="https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&q=80&w=1000" className="rounded-[80px] shadow-2xl relative z-10" alt="Walk mate" />
                <Card className="absolute top-10 -right-10 p-8 shadow-2xl z-20 animate-in slide-in-from-right-10 border-zinc-100 text-left">
                   <div className="flex items-center gap-4 mb-4">
                      <div className="w-12 h-12 bg-amber-500 rounded-full flex items-center justify-center text-navy-900">
                         <Sparkles size={24} />
                      </div>
                      <div>
                         <Typography variant="label" className="text-amber-500">Match Success</Typography>
                         <Typography variant="h3" className="text-xl">98.4% 일치</Typography>
                      </div>
                   </div>
                   <Typography variant="label" className="text-zinc-400 tracking-widest leading-relaxed normal-case">"보리와 뭉치는 에너지가 비슷해요!<br/>오늘 서울숲 벚꽃길에서 만나는 건 어때요?"</Typography>
                </Card>
             </div>
          </div>
        </section>

                {/* 3. Feature: Neighborhood Radar */}
                <section id="radar" className="h-screen py-20 px-6 bg-[#FDFCF8] overflow-hidden snap-start flex items-center shrink-0">
                  <Card className="max-w-7xl mx-auto bg-navy-900 p-12 md:p-24 text-white relative shadow-2xl border-none rounded-[80px]">
                     <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-amber-500/20 rounded-full -mr-96 -mt-96 blur-[120px] animate-pulse"></div>
                     <div className="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-20 items-center">
                        <div className="lg:col-span-5 space-y-12 text-left">
                           <Badge variant="amber" className="bg-white/10 border-transparent py-2 px-5">
                              <TrendingUp size={16} className="inline mr-2" /> Real-time Radar
                           </Badge>
                           <Typography variant="serif" className="text-5xl md:text-7xl not-italic">내 주변에 <br /> 누가 걷고 있을까?</Typography>
                           <Typography variant="body" className="text-zinc-400 text-lg">
                             앱을 켜기만 하면 지도 위에서 지금 산책 중인 이웃 강아지들을 실시간으로 확인할 수 있습니다. 
                             사람이 너무 붐비는 곳은 피하고, 친구가 모여있는 핫스팟은 빠르게 찾아가세요.
                           </Typography>
                           <div className="space-y-6">
                              <div className="flex items-center gap-6 p-6 bg-white/5 rounded-3xl border border-white/10">
                                 <div className="w-10 h-10 bg-amber-500 rounded-xl flex items-center justify-center text-navy-900 shrink-0"><MapPin size={20} /></div>
                                 <Typography variant="body" className="text-sm font-black">성수동 서울숲 광장: 현재 12마리 산책 중</Typography>
                              </div>
                              <div className="flex items-center gap-6 p-6 bg-white/5 rounded-3xl border border-white/10">
                                 <div className="w-10 h-10 bg-blue-500 rounded-xl flex items-center justify-center text-white shrink-0"><Play size={20} /></div>
                                 <Typography variant="body" className="text-sm font-black">근처 '보리'가 산책 약속을 시작했어요!</Typography>
                              </div>
                           </div>
                        </div>
                        <div className="lg:col-span-7 relative">
                           <div className="aspect-[4/3] bg-white/5 rounded-[60px] border border-white/10 overflow-hidden relative shadow-inner">
                              <img src="https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&q=80&w=1000" className="w-full h-full object-cover opacity-40 mix-blend-overlay" alt="Radar map" />
                              <div className="absolute inset-0 flex items-center justify-center">
                                 <div className="w-48 h-48 border-2 border-amber-500/30 rounded-full animate-ping"></div>
                                 <div className="w-96 h-96 border border-amber-500/10 rounded-full animate-ping delay-700 absolute"></div>
                                 <div className="bg-amber-500 p-4 rounded-3xl shadow-2xl shadow-amber-500/50 relative z-10">
                                    <Users size={40} className="text-navy-900" />
                                 </div>
                              </div>
                           </div>
                        </div>
                     </div>
                  </Card>
                </section>
        
                {/* 4. Feature: Community & Social */}
                <section id="community" className="h-screen py-20 px-6 bg-white snap-start flex items-center shrink-0">
                  <div className="max-w-6xl mx-auto space-y-24">
                     <div className="text-center space-y-6 max-w-3xl mx-auto">
                        <Typography variant="label" className="text-amber-500">Neighborhood Hub</Typography>
                        <Typography variant="serif" className="text-5xl md:text-7xl text-navy-900 not-italic">
                          단순한 정보 공유를 넘어 <br /> <span className="italic text-amber-500">따뜻한 이웃</span>이 됩니다.
                        </Typography>
                        <Typography variant="body" className="text-lg text-zinc-400">우리 동네 강아지들의 소소한 일상 피드부터, 검증된 병원 정보, 사료 나눔까지. 반려인들만의 투명하고 따뜻한 커뮤니티.</Typography>
                     </div>
                     
                     <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                        {[
                          { title: "실시간 동네 피드", icon: MessageSquare, desc: "방금 서울숲에 핀 꽃, 지금 산책하기 좋은 날씨 등 동네 소식을 실시간으로 확인하세요." },
                          { title: "단골 친구 맺기", icon: UserPlus, desc: "마음이 잘 맞는 산책 파트너와 친구를 맺고 단골 메이트가 되어보세요." },
                          { title: "검증된 이웃 정보", icon: CheckCircle2, desc: "인증된 견주들만 활동하는 안전한 공간에서 신뢰할 수 있는 정보를 나누세요." }
                        ].map((item, i) => (
                          <Card key={i} className="p-12 bg-zinc-50 border-zinc-100 hover:bg-white hover:shadow-2xl group rounded-[56px] text-left">
                             <div className="w-16 h-16 bg-navy-900 text-amber-500 rounded-2xl flex items-center justify-center mb-8 group-hover:scale-110 transition-transform">
                                <MessageSquare size={28} />
                             </div>
                             <Typography variant="h3" className="text-2xl text-navy-900 mb-4">{item.title}</Typography>
                             <Typography variant="body" className="text-sm text-zinc-400 font-medium">{item.desc}</Typography>
                          </Card>
                        ))}
                     </div>
                  </div>
                </section>
        
                {/* 5. Feature: SOS Emergency */}
                <section id="emergency" className="h-screen py-20 px-6 bg-red-600 relative overflow-hidden text-white snap-start flex items-center shrink-0">
                  <div className="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1541364983171-a8ba01e95cfc?auto=format&fit=crop&q=80&w=2000')] bg-cover bg-center opacity-10"></div>
                  <div className="max-w-6xl mx-auto relative z-10 flex flex-col lg:flex-row items-center gap-24">
                     <div className="flex-1 space-y-12 text-left">
                        <Badge variant="red" className="bg-white/20 border-transparent text-white py-2 px-5">
                           <ShieldAlert size={16} className="inline mr-2" /> Critical Golden Time
                        </Badge>
                        <Typography variant="serif" className="text-5xl md:text-7xl leading-tight not-italic">
                          골든타임을 지키는 <br /> <span className="italic text-amber-300 underline decoration-amber-300 underline-offset-8">AI 긴급 구조.</span>
                        </Typography>
                        <Typography variant="body" className="text-white/80 text-xl">
                          실종 사고 발생 시 즉각 주변 모든 사용자에게 알림을 전송합니다. 
                          누군가 유기견 사진을 제보하면, AI가 실종 신고 데이터와 실시간 대조하여 0.1초 만에 일치 여부를 판별합니다.
                        </Typography>
                        <div className="space-y-4 pt-6">
                           <div className="flex items-center gap-4 text-sm font-black"><CheckCircle2 className="text-amber-300" size={20} /> 실시간 주변 사용자 푸시 알림</div>
                           <div className="flex items-center gap-4 text-sm font-black"><CheckCircle2 className="text-amber-300" size={20} /> Gemini AI 이미지 매칭 시스템</div>
                           <div className="flex items-center gap-4 text-sm font-black"><CheckCircle2 className="text-amber-300" size={20} /> 112/유관기관 실시간 연동 지원</div>
                        </div>
                     </div>
                     <div className="flex-1 w-full max-w-lg">
                        <Card className="p-10 shadow-2xl space-y-8 animate-pulse border-none rounded-[64px]">
                           <div className="flex items-center justify-between border-b border-zinc-100 pb-6">
                              <Typography variant="h3" className="text-2xl text-red-600 uppercase">Emergency SOS</Typography>
                              <Typography variant="label" className="text-zinc-400">성수동 인근</Typography>
                           </div>
                           <div className="flex gap-6">
                              <div className="relative">
                                 <img src="https://picsum.photos/id/1025/200/200" className="w-32 h-32 rounded-[32px] object-cover grayscale" alt="Missing dog" />
                                 <div className="absolute top-0 left-0 w-full h-1 bg-red-500 animate-scan z-10" />
                              </div>
                              <div className="space-y-2 text-left">
                                 <Typography variant="body" className="text-lg font-black text-navy-900">리틀 루이 (골든리트리버)</Typography>
                                 <Typography variant="body" className="text-xs text-zinc-400 font-bold">하늘색 하네스 착용. 오늘 오후 3시경 서울숲 마지막 목격. 발견 시 즉시 제보 바랍니다.</Typography>
                              </div>
                           </div>
                           <Button variant="danger" size="lg" fullWidth>
                              제보 사진 업로드하기
                           </Button>
                        </Card>
                     </div>
                  </div>
                </section>
        
                {/* 6. Trust & Safety Section */}
                <section id="safety" className="h-screen py-20 px-6 bg-[#FDFCF8] snap-start flex items-center shrink-0">
                   <div className="max-w-6xl mx-auto flex flex-col lg:flex-row items-center gap-24">
                      <div className="flex-1 space-y-12 text-left">
                         <Badge variant="emerald" className="py-2 px-4">
                            <ShieldCheck size={14} className="inline mr-2" /> Trust Certified
                         </Badge>
                         <Typography variant="serif" className="text-5xl md:text-6xl text-navy-900 not-italic">
                           매너가 만드는 <br /> <span className="text-amber-500 italic">행복한</span> 산책 문화.
                         </Typography>
                         <Typography variant="body" className="text-lg text-zinc-400">아이니이누는 100% 실명 인증과 반려견 등록번호 확인을 거친 이웃들만 활동할 수 있습니다. 상호 평가를 통한 '매너 온도'로 검증된 메이트만 만나보세요.</Typography>
                         <div className="space-y-6">
                            {[
                              { icon: Fingerprint, title: "100% 견주 인증", desc: "동물등록번호 조회를 통해 실제 견주임을 철저히 확인합니다." },
                              { icon: Star, title: "실시간 매너 온도", desc: "산책 후 서로의 매너를 평가하여 건전한 문화를 만듭니다." },
                              { icon: Shield, title: "데이터 암호화", desc: "사용자의 위치와 정보는 엄격하게 보호됩니다." }
                            ].map((s, i) => (
                              <Card key={i} className="flex gap-6 p-8 hover:shadow-xl rounded-[40px] text-left">
                                 <div className="w-12 h-12 bg-zinc-50 rounded-2xl flex items-center justify-center text-navy-900 shrink-0">
                                    <s.icon size={24} />
                                 </div>
                                 <div>
                                    <Typography variant="h3" className="text-lg text-navy-900 mb-1">{s.title}</Typography>
                                    <Typography variant="body" className="text-sm text-zinc-400 font-medium">{s.desc}</Typography>
                                 </div>
                              </Card>
                            ))}
                         </div>
                      </div>
                      <div className="flex-1 relative">
                         <div className="w-full aspect-square bg-zinc-200 rounded-[80px] overflow-hidden shadow-2xl relative">
                            <img src="https://images.unsplash.com/photo-1544568100-847a948585b9?auto=format&fit=crop&q=80&w=1000" className="w-full h-full object-cover" alt="Security" />
                            <div className="absolute inset-0 bg-navy-900/10"></div>
                            <Card variant="glass" className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 p-10 min-w-[320px] rounded-[56px] text-center space-y-6">
                               <img src="https://picsum.photos/id/64/100/100" className="w-24 h-24 rounded-[32px] border-4 border-[#FDFCF8] shadow-xl mx-auto" alt="Profile" />
                               <div>
                                  <Typography variant="label" className="text-emerald-500 mb-1">Certified Manager</Typography>
                                  <Typography variant="h3" className="text-3xl text-navy-900">몽이아빠</Typography>
                                  <Typography variant="body" className="text-sm font-bold text-zinc-400 mt-2">매너온도 37.5°C • 골드 멤버</Typography>
                               </div>
                            </Card>
                         </div>
                      </div>
                   </div>
                </section>
        
                {/* 7. Final CTA Section */}
                <section id="cta" className="h-screen py-20 px-6 relative bg-navy-900 overflow-hidden text-center text-white snap-start flex items-center shrink-0">
        
         <div className="absolute inset-0 bg-gradient-to-br from-amber-500/20 to-transparent"></div>
         <div className="relative z-10 max-w-5xl mx-auto space-y-16">
            <Typography variant="serif" className="text-6xl sm:text-8xl md:text-9xl leading-[0.9] not-italic">
               함께 걷는 <br /> <span className="italic text-amber-500">기쁨</span>을 <br /> 경험하세요.
            </Typography>
            <div className="flex flex-col items-center gap-10">
               <Button 
                onClick={() => router.push('/signup')}
                variant="secondary"
                size="xl"
                className="bg-white text-navy-900 hover:bg-amber-500 hover:text-navy-900 shadow-2xl scale-105"
               >
                 아이니이누 지금 가입하기 <ArrowRight className="inline ml-4 group-hover:translate-x-3 transition-transform" size={40} />
               </Button>
               <div className="flex items-center gap-10 text-white/30">
                  <Typography variant="label">Seoul Forest Based</Typography>
                  <div className="w-1 h-1 bg-white/20 rounded-full"></div>
                  <Typography variant="label">Nationwide Service</Typography>
               </div>
            </div>
         </div>
      </section>

      {/* Footer */}
      <footer className="py-20 bg-white border-t border-zinc-100 px-6 md:px-20 flex flex-col md:flex-row justify-between items-center gap-10 snap-start shrink-0">
         <div className="space-y-4 text-center md:text-left">
            <Typography variant="body" className="text-2xl font-black tracking-tighter lowercase leading-none">
              aini<span className="text-amber-500">inu</span>
            </Typography>
            <Typography variant="label" className="text-zinc-400 leading-relaxed normal-case">Designing the future of human and pet connection.<br/>The most trusted neighborhood radar.</Typography>
         </div>
         <div className="flex gap-10">
            <Typography variant="label" className="text-zinc-400 hover:text-amber-500 transition-colors cursor-pointer">Contact</Typography>
            <Typography variant="label" className="text-zinc-400 hover:text-amber-500 transition-colors cursor-pointer">Terms of Service</Typography>
            <Typography variant="label" className="text-zinc-400 hover:text-amber-500 transition-colors cursor-pointer">Privacy Policy</Typography>
         </div>
         <Typography variant="label" className="text-zinc-300">© 2026 AINI INU. ALL RIGHTS RESERVED.</Typography>
      </footer>
    </div>

    {/* Discovery Popup Section (Completely OUTSIDE the snap container) */}
    {flowStep !== 'LANDING' && (
      <section className="fixed inset-0 z-[150] bg-white/95 backdrop-blur-2xl animate-in fade-in duration-500 flex items-center justify-center p-6 text-center">
        <button 
          onClick={() => setFlowStep('LANDING')}
          className="absolute top-10 right-10 p-4 hover:rotate-90 transition-transform text-zinc-300 hover:text-navy-900"
        >
          <X size={32} />
        </button>

        <div className="max-w-4xl w-full">
          {flowStep === 'ADDRESS' && (
            <div className="space-y-12 animate-in slide-in-from-bottom-10 duration-700">
              <div className="space-y-4">
                <div className="w-20 h-20 bg-amber-500 rounded-3xl flex items-center justify-center text-navy-900 mx-auto shadow-2xl shadow-amber-500/20 mb-6">
                  <MapIcon size={36} />
                </div>
                <Typography variant="serif" className="text-5xl md:text-7xl">어디에 살고 계신가요?</Typography>
                <Typography variant="body" className="text-xl text-zinc-400">상세 주소를 입력하시면 주변에 있는 이웃들을 찾아드릴게요.</Typography>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2 text-left">
                  <Typography variant="label" className="px-4">City / Province</Typography>
                  <input 
                    type="text" 
                    placeholder="예: 서울특별시" 
                    className="w-full bg-zinc-50 border border-zinc-100 rounded-[32px] px-8 py-6 text-xl font-bold focus:outline-none focus:ring-4 ring-amber-500/10 transition-all text-navy-900"
                    value={address.city}
                    onChange={(e) => setAddress({...address, city: e.target.value})}
                  />
                </div>
                <div className="space-y-2 text-left">
                  <Typography variant="label" className="px-4">District / Neighborhood</Typography>
                  <input 
                    type="text" 
                    placeholder="예: 성동구 성수동" 
                    className="w-full bg-zinc-50 border border-zinc-100 rounded-[32px] px-8 py-6 text-xl font-bold focus:outline-none focus:ring-4 ring-amber-500/10 transition-all text-navy-900"
                    value={address.district}
                    onChange={(e) => setAddress({...address, district: e.target.value})}
                  />
                </div>
              </div>

              <Button 
                onClick={handleSearch}
                disabled={!address.city || !address.district}
                variant="primary"
                size="xl"
                fullWidth
                className="gap-4 group"
              >
                주변 이웃 검색하기 <ArrowRight className="group-hover:translate-x-2 transition-transform" />
              </Button>
            </div>
          )}

          {flowStep === 'SCANNING' && (
            <div className="space-y-8 animate-in zoom-in-95 duration-1000">
              <div className="relative w-48 h-48 mx-auto">
                <div className="absolute inset-0 border-4 border-amber-500/20 rounded-full"></div>
                <div className="absolute inset-0 border-4 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
                <div className="absolute inset-0 flex items-center justify-center text-amber-500">
                  <Loader2 size={64} className="animate-pulse" />
                </div>
              </div>
              <div className="space-y-2">
                <Typography variant="serif" className="text-4xl">Neighborhood Scanning...</Typography>
                <Typography variant="label" className="text-zinc-400">성수동 1가 인근 데이터를 분석 중입니다</Typography>
              </div>
            </div>
          )}

          {flowStep === 'RESULTS' && (
            <div className="space-y-12 animate-in slide-in-from-bottom-10 duration-700">
              <div className="space-y-4">
                <Typography variant="serif" className="text-5xl md:text-7xl leading-tight">
                  와우! <span className="text-amber-500">24명</span>의 <br /> 새로운 친구를 찾았어요.
                </Typography>
                <Typography variant="body" className="text-xl text-zinc-400">{address.city} {address.district} 인근에서 활동 중인 메이트들입니다.</Typography>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                {MOCK_NEIGHBORS.map((neighbor, i) => (
                  <Card key={i} interactive className="p-6 text-center space-y-4">
                    <div className="relative">
                      <img src={neighbor.img} alt={neighbor.name} className="w-full aspect-square rounded-[32px] object-cover group-hover:scale-105 transition-transform" />
                      <div className="absolute -bottom-2 -right-2 w-10 h-10 bg-white rounded-full flex items-center justify-center text-red-500 shadow-lg">
                        <Heart size={18} fill="currentColor" />
                      </div>
                    </div>
                    <div>
                      <Typography variant="body" className="font-black text-navy-900 text-lg">{neighbor.name}</Typography>
                      <Typography variant="label" className="text-zinc-400">{neighbor.breed}</Typography>
                    </div>
                    <Badge variant="amber">매너온도 {neighbor.temp}°</Badge>
                  </Card>
                ))}
              </div>

              <div className="pt-10 flex flex-col items-center gap-8">
                <Card className="p-8 bg-zinc-50 border-zinc-100 max-w-xl">
                  <Typography variant="body" className="text-lg text-navy-900 font-bold italic">
                    "지금 가입하면 이 이웃들과 바로 채팅하고 <br /> 오늘 저녁 산책을 약속할 수 있어요!"
                  </Typography>
                </Card>
                <Button 
                  onClick={() => router.push('/signup')}
                  variant="primary"
                  size="xl"
                  className="scale-105 group gap-6"
                >
                  이들과 산책 시작하기 <ArrowRight size={40} className="group-hover:translate-x-3 transition-transform" />
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setFlowStep('ADDRESS')}>다시 검색하기</Button>
              </div>
            </div>
          )}
        </div>
      </section>
    )}

    <style jsx global>{`
      @keyframes scan {
        from { top: 0; }
        to { top: 100%; }
      }
      .animate-scan {
        animation: scan 2s linear infinite;
      }
      .no-scrollbar::-webkit-scrollbar {
        display: none;
      }
      .no-scrollbar {
        -ms-overflow-style: none;
        scrollbar-width: none;
      }
    `}</style>
  </div>
);
}
