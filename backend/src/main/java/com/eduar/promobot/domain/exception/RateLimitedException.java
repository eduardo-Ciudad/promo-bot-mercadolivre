package com.eduar.promobot.domain.exception;

public class RateLimitedException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super("Envio limitado por rate limit do provider, retry apos %d segundos".formatted(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
