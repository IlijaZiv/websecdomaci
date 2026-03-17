package com.example.websecurity.service;

import com.example.websecurity.exception.WebSecTooManyAttemptsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force / dictionary attack protection.
 *
 * Strategy: escalating lockout times after consecutive failed login attempts.
 *   1-2 failures  -> no lockout
 *   3rd failure   -> locked for  5 seconds
 *   4th failure   -> locked for 15 seconds
 *   5th failure   -> locked for 30 seconds
 *   6th+ failure  -> locked for 60 seconds
 *
 * The lockout counter resets to zero after a successful login.
 */
@Service
@Slf4j
public class LoginAttemptService {

    // Lockout durations (seconds) indexed by failure count (1-based).
    // Index 0 and 1 -> no lockout, index 2+ -> escalating.
    private static final long[] LOCKOUT_SECONDS = {0, 0, 5, 15, 30, 60};

    private record AttemptInfo(int failureCount, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    /**
     * Check whether the given email is currently locked out.
     * Throws {@link WebSecTooManyAttemptsException} if it is.
     */
    public void checkNotLocked(String email) {
        AttemptInfo info = attempts.get(email);
        if (info == null) return;

        if (info.lockedUntil() != null && Instant.now().isBefore(info.lockedUntil())) {
            long secondsLeft = info.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
            log.warn("Login attempt blocked for {}: {} seconds remaining", email, secondsLeft);
            throw new WebSecTooManyAttemptsException(
                    "Too many failed login attempts. Try again in " + secondsLeft + " second(s).",
                    secondsLeft
            );
        }
    }

    /**
     * Called after a failed login attempt. Increments the failure counter
     * and sets the lockout window accordingly.
     */
    public void registerFailure(String email) {
        AttemptInfo current = attempts.getOrDefault(email, new AttemptInfo(0, null));

        // If a previous lockout has already expired, count from where we left off
        int newCount = current.failureCount() + 1;
        long lockoutSeconds = LOCKOUT_SECONDS[Math.min(newCount, LOCKOUT_SECONDS.length - 1)];

        Instant lockedUntil = lockoutSeconds > 0
                ? Instant.now().plusSeconds(lockoutSeconds)
                : null;

        attempts.put(email, new AttemptInfo(newCount, lockedUntil));
        log.warn("Failed login for {} (attempt #{}). Locked for {} seconds.", email, newCount, lockoutSeconds);
    }

    /**
     * Called after a successful login. Clears the failure record.
     */
    public void registerSuccess(String email) {
        attempts.remove(email);
        log.info("Successful login for {} - attempt counter reset.", email);
    }
}
