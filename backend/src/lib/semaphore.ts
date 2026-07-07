/**
 * Minimal FIFO async semaphore used to cap concurrent yt-dlp processes and
 * concurrent proxied streams. Keeps the box healthy under bursty load without
 * dragging in a queue dependency for what is fundamentally back-pressure.
 */
export class Semaphore {
  private available: number;
  private readonly waiters: Array<() => void> = [];

  constructor(private readonly capacity: number) {
    if (capacity < 1) throw new Error('Semaphore capacity must be >= 1');
    this.available = capacity;
  }

  get inUse(): number {
    return this.capacity - this.available;
  }

  get queued(): number {
    return this.waiters.length;
  }

  async acquire(): Promise<() => void> {
    if (this.available > 0) {
      this.available -= 1;
      return this.releaseOnce();
    }
    await new Promise<void>((resolve) => this.waiters.push(resolve));
    this.available -= 1;
    return this.releaseOnce();
  }

  async run<T>(fn: () => Promise<T>): Promise<T> {
    const release = await this.acquire();
    try {
      return await fn();
    } finally {
      release();
    }
  }

  private releaseOnce(): () => void {
    let released = false;
    return () => {
      if (released) return;
      released = true;
      this.available += 1;
      const next = this.waiters.shift();
      if (next) next();
    };
  }
}
