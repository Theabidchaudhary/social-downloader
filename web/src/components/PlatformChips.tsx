import type { Platform } from '../lib/api';
import { PLATFORM_META } from '../lib/platform';

const ORDER: Platform[] = ['youtube', 'instagram', 'tiktok', 'twitter', 'facebook'];

export function PlatformChips({ active }: { active: Platform | null }) {
  return (
    <div className="platforms" id="platforms" role="list" aria-label="Supported platforms">
      {ORDER.map((id) => {
        const meta = PLATFORM_META[id];
        const isActive = active === id;
        return (
          <span key={id} role="listitem" className={`chip${isActive ? ' active' : ''}`}>
            <span className="dot" style={{ background: meta.color }} aria-hidden="true" />
            {meta.name}
            {isActive && <span aria-live="polite" style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}>{`${meta.name} link detected`}</span>}
          </span>
        );
      })}
    </div>
  );
}
