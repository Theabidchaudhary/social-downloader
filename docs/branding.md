# Siphon — Branding & design system

## Name

**Siphon** — one word, a verb and an object at once: a tube that pulls
something down from a higher place. Lowercase wordmark (`siphon`) for the
product surface; capitalized in prose.

Tagline: **“Save any video. In seconds.”**

## Logo & app icon

The mark is an S-curved tube that resolves into a downward arrow above a
catch tray — "content flows down and lands with you". Sources of truth:

- `assets/brand/icon.svg` — 512 px app icon (rounded 116 px tile)
- `assets/brand/logo.svg` — horizontal lockup (mark + wordmark)
- Android adaptive icon: `android/app/src/main/res/drawable/ic_launcher_foreground.xml`
  over a near-black `#0A0812` background, with a monochrome variant for
  themed icons.

The glyph gradient runs iris → cyan (`#9D8CFF → #7C6BFF → #46C8FF`), the tray
is off-white.

## Color

Layered near-black surfaces, one saturated accent, generous glows. Defined in
`web/src/styles/tokens.css` and mirrored in `android/…/ui/theme/Theme.kt`.

| Token | Value | Use |
| --- | --- | --- |
| `ink-0` | `#08080C` | page/window background |
| `ink-1` | `#0E0E15` | raised sections |
| `ink-2` | `#14141D` | cards |
| `ink-3` | `#1B1B27` | nested cards, inputs |
| `ink-4` | `#232333` | hover/pressed |
| `iris-500` | `#7C6BFF` | primary actions, focus |
| `iris-300/400` | `#A99CFF` / `#948BFF` | text on dark, gradients |
| `cyan-400` | `#46C8FF` | gradient terminal, secondary accent |
| text | `#F2F2F8` / `#A9A9BC` / `#6D6D82` | primary / secondary / tertiary |
| status | `#3ADFA5` / `#FFC553` / `#FF6373` | success / warning / danger |

Platform dot colors (chips): YouTube `#FF5252`, Instagram `#E15FED`, TikTok
`#4CD9E8`, X `#E7E9EA`, Facebook `#5B8DEF`.

## Typography

**Inter** (variable) everywhere on the web; Android uses the system sans with
matched weights/tracking. Display headings are 750–800 weight with tight
(−3…−4 %) letter-spacing; micro-labels are 11–12 px, uppercase, +6 %
tracking. Numbers in progress/size contexts use tabular figures.

## Design language

Inspired by Linear, Raycast, Vercel, Stripe — not by downloader sites.

- **Layered, not bordered**: hierarchy comes from surface steps (`ink-0…4`)
  plus 1 px `rgba(255,255,255,.07)` hairlines.
- **Radii**: 8 / 12 / 16 / 24 px; pills for chips.
- **Glow, sparingly**: the iris glow (`rgba(124,107,255,.22–.35)`) marks the
  single most important element per view (URL bar focus, primary CTA).
- **Motion**: 140–240 ms, `cubic-bezier(.16,1,.3,1)`, opacity+translate only.
  Every animation signals state (result appearing, button press); nothing
  decorative. Reduced-motion preference collapses durations to zero.
- **Spacing**: strict 4 px grid.
- **Tone of voice**: short, direct, no exclamation marks. Errors say what
  happened and what to do next.
