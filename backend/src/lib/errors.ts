/**
 * Typed application errors. Every error the API surfaces to a client carries a
 * stable machine-readable `code` so the web and Android clients can localize
 * and branch on failures without parsing prose.
 */
export type ErrorCode =
  | 'INVALID_URL'
  | 'UNSUPPORTED_PLATFORM'
  | 'EXTRACTION_FAILED'
  | 'REQUIRES_LOGIN'
  | 'MEDIA_UNAVAILABLE'
  | 'TOKEN_INVALID'
  | 'TOKEN_EXPIRED'
  | 'DOWNLOAD_TOO_LARGE'
  | 'RATE_LIMITED'
  | 'INTERNAL';

export class ApiError extends Error {
  constructor(
    public readonly code: ErrorCode,
    message: string,
    public readonly statusCode: number,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  toBody() {
    return {
      error: {
        code: this.code,
        message: this.message,
        ...(this.details ? { details: this.details } : {}),
      },
    };
  }

  static invalidUrl(message = 'The provided value is not a valid URL.') {
    return new ApiError('INVALID_URL', message, 400);
  }

  static unsupportedPlatform() {
    return new ApiError(
      'UNSUPPORTED_PLATFORM',
      'This link is not from a supported platform. Siphon supports YouTube, Instagram, TikTok, X and Facebook.',
      422,
    );
  }

  static extractionFailed(details?: Record<string, unknown>) {
    return new ApiError(
      'EXTRACTION_FAILED',
      'We could not read this link. It may be private, region-locked or removed.',
      502,
      details,
    );
  }

  static requiresLogin() {
    return new ApiError(
      'REQUIRES_LOGIN',
      'This video is private, age-restricted, or the platform is asking the server to sign in. It may work later, or the server operator can add login cookies to fix this permanently.',
      422,
    );
  }

  static mediaUnavailable() {
    return new ApiError(
      'MEDIA_UNAVAILABLE',
      'This media is no longer available at the source.',
      410,
    );
  }

  static tokenInvalid() {
    return new ApiError('TOKEN_INVALID', 'The download link is invalid.', 403);
  }

  static tokenExpired() {
    return new ApiError(
      'TOKEN_EXPIRED',
      'This download link has expired. Resolve the video again to get a fresh one.',
      410,
    );
  }

  static downloadTooLarge(limitMb: number) {
    return new ApiError(
      'DOWNLOAD_TOO_LARGE',
      `This file exceeds the ${limitMb} MB per-download limit.`,
      413,
    );
  }
}
