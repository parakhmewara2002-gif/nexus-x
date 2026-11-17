---
name: "iOS 26 Light Liquid Glass"
description: "Authentic iOS 26 Light Liquid Glass visual language for NEXUS X featuring milky-white translucent glass, realistic refraction over vibrant wallpapers, soft borders, and floating pill navigation."
---

## Overview

**NEXUS X Light Liquid Glass** introduces an authentic, next-generation iOS 26 visual language. Replacing dark graphite surfaces completely, this interface celebrates light diffusion, optical clarity, milky-white translucent glass surfaces, and soft specular reflections floating over luminous, colorful ambient wallpaper gradients (sky blue, subtle lavender, and warm iridescent tints).

Every surface breathes with physical optical refraction: floating milky glass panels with realistic multi-tier backdrop blur (`backdrop-filter: blur(28px)`), delicate translucent white borders (`rgba(255, 255, 255, 0.7)`), soft light diffusion, and a signature floating horizontal pill navigation bar where white and frosted icons sit alongside an electric sky blue active indicator.

```
Design Pillars:
1. Authentic Light Liquid Glass (Milky-white translucency, realistic multi-tier blur, visible background diffusion)
2. iOS 26 Floating Pill Navigation (Translucent glass dock, 5 tabs: Home, Files, Device, Tools, Settings, with glowing bright-blue active pill highlight)
3. Sophisticated Color Atmosphere (Luminous sky blue, light cyan, soft lavender, and silver-white accents)
4. Desktop-Grade Functional Density (Glanceable file explorer, ADB tools, device health, storage analytics)
```

## Colors

The palette embraces a bright, radiant ground with translucent frosted glass planes. High-contrast typography in deep slate (`#0F172A`) ensures crystal-clear legibility over milky-white glass, while vibrant sky blue (`#0284C7` / `#0EA5E9`) and radiant lavender (`#8B5CF6`) bring active elements to life.

- **Primary:** Radiant Sky Blue (`#0284C7`) — active navigation pill, primary action triggers, and active toggles.
- **Glass Ground:** Luminous ambient background with subtle pastel gradient meshes visible through translucent glass.
- **Glass Panels:** Milky-white translucent glass (`rgba(255, 255, 255, 0.65)` to `rgba(255, 255, 255, 0.82)`) with 28px backdrop blur and micro-etched white specular rim highlights (`rgba(255, 255, 255, 0.9)`).

```yaml
colors:
  accent: "#0EA5E9"
  accent-glow: "rgba(14, 165, 233, 0.35)"
  lavender: "#8B5CF6"
  cyan: "#06B6D4"
  success: "#10B981"
  warning: "#F59E0B"
  danger: "#EF4444"
  light:
    primary: "#0284C7"
    on-primary: "#131313"
    secondary: "#7C3AED"
    on-secondary: "#FFFFFF"
    surface: "#F0F4F9"
    on-surface: "#0F172A"
    surface-container-low: "#FFFFFF"
    surface-container: "rgba(255, 255, 255, 0.68)"
    surface-container-high: "rgba(255, 255, 255, 0.88)"
    surface-glass: "rgba(255, 255, 255, 0.65)"
    surface-glass-border: "rgba(255, 255, 255, 0.75)"
    outline: "#64748B"
    outline-variant: "rgba(203, 213, 225, 0.6)"
  dark:
    primary: "#38BDF8"
    on-primary: "#082F49"
    secondary: "#C084FC"
    on-secondary: "#3B0764"
    surface: "#0F172A"
    on-surface: "#F8FAFC"
    surface-container-low: "#1E293B"
    surface-container: "#334155"
    surface-container-high: "#475569"
    surface-glass: "rgba(30, 41, 59, 0.7)"
    surface-glass-border: "rgba(255, 255, 255, 0.15)"
    outline: "#94A3B8"
    outline-variant: "#334155"
```

## Typography

Typographic hierarchy uses **Plus Jakarta Sans** for headlines and metrics, **Instrument Sans** for neutral content, and **Source Code Pro** for exact byte measurements, file hashes, ADB commands, and device specs.

```yaml
typography:
  display-lg:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 40px
    fontWeight: 700
    lineHeight: 48px
    letterSpacing: -0.8px
  display-lg-mobile:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 38px
    letterSpacing: -0.6px
  headline-xl:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 34px
    fontWeight: 700
    lineHeight: 42px
    letterSpacing: -0.5px
  headline-xl-mobile:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 28px
    fontWeight: 700
    lineHeight: 34px
    letterSpacing: -0.4px
  headline-lg:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 26px
    fontWeight: 600
    lineHeight: 32px
    letterSpacing: -0.3px
  headline-lg-mobile:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 22px
    fontWeight: 600
    lineHeight: 28px
    letterSpacing: -0.2px
  headline-md:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: -0.1px
  headline-sm:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 22px
    letterSpacing: 0px
  body-lg:
    fontFamily: "Instrument Sans"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
    letterSpacing: 0px
  body-md:
    fontFamily: "Instrument Sans"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
    letterSpacing: 0px
  body-sm:
    fontFamily: "Instrument Sans"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 16px
    letterSpacing: 0.1px
  label-lg:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 16px
    letterSpacing: 0.2px
  label-sm:
    fontFamily: "Plus Jakarta Sans"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 14px
    letterSpacing: 0.3px
  code-md:
    fontFamily: "Source Code Pro"
    fontSize: 13px
    fontWeight: 500
    lineHeight: 18px
    letterSpacing: 0px
  code-sm:
    fontFamily: "Source Code Pro"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 14px
    letterSpacing: 0px
```

## Layout & Spacing

An 8-point spatial system defines outer margins, floating sheets, grid cards, and navigation heights.

```yaml
spacing:
  gutter-xs: 4px
  gutter-sm: 8px
  gutter-md: 12px
  gutter-lg: 16px
  gutter-xl: 24px
  section: 32px
  page-margin: 20px
  nav-dock-height: 72px
```

## Elevation & Depth

- **Ambient Background:** Luminous, airy gradient ground featuring subtle blue, cyan, and lavender radial light pools.
- **Milky Liquid Glass Cards:** `background: rgba(255, 255, 255, 0.68)`, `backdrop-filter: blur(28px)`, `border: 1px solid rgba(255, 255, 255, 0.8)`, and `box-shadow: 0 12px 32px -8px rgba(15, 23, 42, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.95)`.
- **Floating Navigation Bar:** Ethereal pill dock floating above the bottom viewport, transparent milky-glass shell with bright sky-blue active indicator.

## Shapes

Shapes feature Apple-standard continuous superellipses with pill geometry for navigation and badges.

```yaml
rounded:
  sm: 8px
  DEFAULT: 14px
  md: 18px
  lg: 24px
  xl: 32px
  full: 9999px
```

## Motion & Micro-interaction

- **Fluid Spring Switch:** Pill tabs slide with `cubic-bezier(0.16, 1, 0.3, 1)`.
- **Card Touchdown:** Press scales down smoothly to `0.98`.

```yaml
motion:
  state-change:
    duration: 140ms
    delay: 0ms
    timingFunction: cubic-bezier(0.16, 1, 0.3, 1)
  card-spring:
    duration: 320ms
    delay: 0ms
    timingFunction: cubic-bezier(0.34, 1.56, 0.64, 1)
  sheet-slide:
    duration: 380ms
    delay: 0ms
    timingFunction: cubic-bezier(0.16, 1, 0.3, 1)
```

## Voice & Content Strategy

Clear, polished, and technical. All screen titles use Title Case, with exact byte statistics and verified ADB/OS connection statuses.

## Components

Component implementations live in `components/`:
- **Button** → `@components/button.html`: `button-primary`, `button-icon`, `button-with-icon`
- **Card** → `@components/card.html`: `card-elevated`, `card-action`, `card-interactive`, `card-interactive-2`
- **List** → `@components/list.html`: `list-single`, `list-two-line`, `list-leading-icon`, `list-trailing-action`, `list-interactive`, `list-interactive-2`
- **Searchbar** → `@components/searchbar.html`: `searchbar-standard`, `searchbar-with-suggestions`
- **Tabs** → `@components/tabs.html`: `tabs-horizontal`, `tabs-with-icon`
