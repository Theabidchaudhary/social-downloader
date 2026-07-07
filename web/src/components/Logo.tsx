export function Logo({ size = 32 }: { size?: number }) {
  return (
    <svg
      className="brand-mark"
      width={size}
      height={size}
      viewBox="0 0 512 512"
      role="img"
      aria-label="Siphon logo"
    >
      <defs>
        <linearGradient id="lg-tile" x1="0" y1="0" x2="512" y2="512" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#16121F" />
          <stop offset="1" stopColor="#0A0812" />
        </linearGradient>
        <linearGradient id="lg-glyph" x1="140" y1="96" x2="380" y2="420" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#9D8CFF" />
          <stop offset="0.55" stopColor="#7C6BFF" />
          <stop offset="1" stopColor="#46C8FF" />
        </linearGradient>
      </defs>
      <rect width="512" height="512" rx="116" fill="url(#lg-tile)" />
      <rect x="2" y="2" width="508" height="508" rx="114" stroke="#FFFFFF" strokeOpacity="0.08" strokeWidth="4" fill="none" />
      <path
        d="M338 132 C 338 112 322 96 302 96 L 220 96 C 172 96 134 134 134 182 C 134 230 172 268 220 268 L 256 268 L 256 330"
        stroke="url(#lg-glyph)"
        strokeWidth="44"
        strokeLinecap="round"
        fill="none"
      />
      <path
        d="M180 314 L 256 396 L 332 314"
        stroke="url(#lg-glyph)"
        strokeWidth="44"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
      <path d="M148 420 L 364 420" stroke="#F2F2F8" strokeOpacity="0.9" strokeWidth="34" strokeLinecap="round" />
    </svg>
  );
}
