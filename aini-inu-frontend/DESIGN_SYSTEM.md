# 🐕 Aini Inu Design System v1.0

아이니이누 프로젝트의 일관된 UI/UX를 위한 디자인 가이드입니다.

---

## 🎨 Color Palette

| Name | Hex | Usage |
| --- | --- | --- |
| **Navy 900** | `#0A0A23` | Primary text, Main buttons, Sidebar background |
| **Amber 500** | `#F59E0B` | Accents, Active states, Secondary buttons, Points |
| **Indigo 600** | `#4F46E5` | Feed/Vibrant theme accents, Interactive elements |
| **Background** | `#FDFCF8` | Default page background (Creamy paper texture) |
| **Success** | `#10B981` | Certified badges, Safe indicators |
| **Error** | `#EF4444` | Emergency (SOS) services, Danger actions |

---

## 📐 Layout & Spacing

- **Radius (Roundness):**
  - Small: `12px` (Small buttons, Badges)
  - Medium: `24px` (Inputs, Small cards)
  - Large: `32px` / `48px` (Main cards, Sections)
  - Full: `9999px` (Profile avatars, Pill buttons)
- **Container:** Max-width `7xl` (1280px) for desktop content.
- **Scroll:** Custom `no-scrollbar` utility for a clean, app-like feel.

---

## ✒️ Typography

- **Serif (Playfair Display or Similar):** Used for large headlines (`h1`, `h2`) to convey warmth and tradition. Often paired with `italic`.
- **Sans-serif (Inter/Geist):** Used for all functional text, body copy, and labels.
  - **Weight:** Heavy usage of `Black (900)` and `Bold (700)` for a strong, confident brand voice.
  - **Tracking:** `tracking-tighter` for headlines, `tracking-widest` for labels.

---

## 🧱 Key Components

### 1. Atoms
- **Button:** Pill-shaped, high-contrast hover states.
- **Card:** Large border-radius, subtle borders, deep shadows on interaction.
- **Badge:** Minimalist status indicators with light backgrounds.

### 2. Domain Components
- **MannerScoreGauge:** A progress bar based on a 1-10 scale with color-coded grades (BEST, GOOD, NORMAL, BAD).
- **DogAvatar:** Overlaid avatars (Dog image + Owner inset) represent the pet-first philosophy.
- **Radar:** Pulse animations and glowing markers for real-time navigation.

---

## ✨ Design Philosophy

1. **Pet-First (강아지 중심):** UI에서 반려견의 사진과 이름이 항상 주인의 정보보다 강조됩니다.
2. **Warmth & Trust (따뜻함과 신뢰):** 부드러운 배경색과 세리프 서체를 통해 신뢰감을 형성합니다.
3. **App-like Web (앱 같은 웹):** Next.js의 고성능을 활용하여 부드러운 화면 전환과 마이크로 인터랙션을 제공합니다.
