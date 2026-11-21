/*
 * The MIT License
 *
 * Copyright 2026 James Amoore.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.jamoamo.honeycomb.rest.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link IdempotencyStore} held in memory, suited to tests and single-instance applications.
 *
 * <p>
 * Expired keys are treated as unused when next acquired, and swept from memory at most once a minute while the store
 * is in use, so no background thread is needed.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class InMemoryIdempotencyStore implements IdempotencyStore
{
   private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

   private final Map<String, Entry> entries = new ConcurrentHashMap<>();
   private final Clock clock;
   private final AtomicReference<Instant> nextSweep;

   /**
    * Constructor using the system clock.
    */
   public InMemoryIdempotencyStore()
   {
      this(Clock.systemUTC());
   }

   /**
    * Constructor.
    *
    * @param clock the clock used to expire keys
    */
   public InMemoryIdempotencyStore(final Clock clock)
   {
      this.clock = Objects.requireNonNull(clock, "clock cannot be null");
      this.nextSweep = new AtomicReference<>(clock.instant().plus(SWEEP_INTERVAL));
   }

   @Override
   public AcquireResult tryAcquire(final String key, final String fingerprint, final Duration ttl)
   {
      Instant now = clock.instant();
      sweepIfDue(now);
      AtomicReference<AcquireResult> result = new AtomicReference<>();
      entries.compute(key, (k, existing) -> acquire(existing, fingerprint, now.plus(ttl), now, result));
      return result.get();
   }

   private static Entry acquire(
      final Entry existing,
      final String fingerprint,
      final Instant expiresAt,
      final Instant now,
      final AtomicReference<AcquireResult> result)
   {
      if (existing == null || !existing.expiresAt().isAfter(now))
      {
         result.set(new AcquireResult.Acquired());
         return new Entry(fingerprint, expiresAt, null);
      }
      result.set(outcome(existing, fingerprint));
      return existing;
   }

   @Override
   public void complete(final String key, final StoredResponse response)
   {
      Objects.requireNonNull(response, "response cannot be null");
      entries.computeIfPresent(key, (k, entry) -> new Entry(entry.fingerprint(), entry.expiresAt(), response));
   }

   @Override
   public void release(final String key)
   {
      entries.computeIfPresent(key, (k, entry) -> entry.response() == null ? null : entry);
   }

   /**
    * The number of keys currently held, including expired keys not yet swept.
    *
    * @return the key count
    */
   int size()
   {
      return entries.size();
   }

   private static AcquireResult outcome(final Entry existing, final String fingerprint)
   {
      if (!existing.fingerprint().equals(fingerprint))
      {
         return new AcquireResult.Mismatch();
      }
      return existing.response() == null
         ? new AcquireResult.InProgress()
         : new AcquireResult.Completed(existing.response());
   }

   private void sweepIfDue(final Instant now)
   {
      Instant due = nextSweep.get();
      if (!now.isBefore(due) && nextSweep.compareAndSet(due, now.plus(SWEEP_INTERVAL)))
      {
         entries.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
      }
   }

   /**
    * A held key; {@code response} is {@code null} while the request is in progress.
    */
   private static final class Entry
   {
      private final String fingerprint;
      private final Instant expiresAt;
      private final StoredResponse response;

      Entry(final String fingerprint, final Instant expiresAt, final StoredResponse response)
      {
         this.fingerprint = fingerprint;
         this.expiresAt = expiresAt;
         this.response = response;
      }

      String fingerprint()
      {
         return fingerprint;
      }

      Instant expiresAt()
      {
         return expiresAt;
      }

      StoredResponse response()
      {
         return response;
      }
   }
}
