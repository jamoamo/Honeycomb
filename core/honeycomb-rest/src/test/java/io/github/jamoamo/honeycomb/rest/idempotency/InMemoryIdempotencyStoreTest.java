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

import io.github.jamoamo.honeycomb.rest.FakeRestRequest;
import io.github.jamoamo.honeycomb.rest.request.BufferedRestRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InMemoryIdempotencyStore}, {@link StoredResponse}, {@link HeaderIdempotencyKeyResolver} and
 * {@link BufferedRestRequest}.
 *
 * @author James Amoore
 */
@DisplayName("InMemoryIdempotencyStore")
public class InMemoryIdempotencyStoreTest
{
   private static final Duration TTL = Duration.ofMinutes(10);
   private static final StoredResponse RESPONSE =
      new StoredResponse(201, Map.of("Location", List.of("/x")), "{}".getBytes(StandardCharsets.UTF_8));

   private final MutableClock clock = new MutableClock();
   private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock);

   @Test
   @DisplayName("acquires, reports in-progress, mismatch and completed keys")
   public void testLifecycle()
   {
      assertThat(store.tryAcquire("k", "f", TTL)).isInstanceOf(AcquireResult.Acquired.class);
      assertThat(store.tryAcquire("k", "f", TTL)).isInstanceOf(AcquireResult.InProgress.class);
      assertThat(store.tryAcquire("k", "other", TTL)).isInstanceOf(AcquireResult.Mismatch.class);

      store.complete("k", RESPONSE);

      assertThat(store.tryAcquire("k", "f", TTL)).isEqualTo(new AcquireResult.Completed(RESPONSE));
      assertThat(store.tryAcquire("k", "other", TTL)).isInstanceOf(AcquireResult.Mismatch.class);
   }

   @Test
   @DisplayName("releases in-progress keys only")
   public void testRelease()
   {
      store.tryAcquire("a", "f", TTL);
      store.release("a");
      assertThat(store.tryAcquire("a", "f", TTL)).isInstanceOf(AcquireResult.Acquired.class);

      store.complete("a", RESPONSE);
      store.release("a");
      assertThat(store.tryAcquire("a", "f", TTL)).isInstanceOf(AcquireResult.Completed.class);

      store.release("missing");
      store.complete("missing", RESPONSE);
      assertThat(store.size()).isEqualTo(1);
   }

   @Test
   @DisplayName("treats expired keys as unused and sweeps them")
   public void testExpiry()
   {
      store.tryAcquire("a", "f", TTL);
      store.complete("a", RESPONSE);
      store.tryAcquire("b", "f", Duration.ofHours(2));

      clock.advance(TTL);
      assertThat(store.tryAcquire("a", "other", TTL)).isInstanceOf(AcquireResult.Acquired.class);

      clock.advance(Duration.ofMinutes(15));
      store.tryAcquire("c", "f", TTL);
      assertThat(store.size()).isEqualTo(2);
   }

   @Test
   @DisplayName("rejects a null clock or response")
   public void testNulls()
   {
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new InMemoryIdempotencyStore(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> store.complete("k", null));
      assertThat(new InMemoryIdempotencyStore().size()).isZero();
   }

   @Test
   @DisplayName("copies stored response bodies and compares by content")
   public void testStoredResponse()
   {
      byte[] body = {1, 2};
      StoredResponse stored = new StoredResponse(200, Map.of(), body);
      body[0] = 9;
      stored.body()[1] = 9;

      assertThat(stored.body()).containsExactly(1, 2);
      assertThat(stored).isEqualTo(new StoredResponse(200, Map.of(), new byte[] {1, 2}))
         .hasSameHashCodeAs(new StoredResponse(200, Map.of(), new byte[] {1, 2}))
         .isNotEqualTo(new StoredResponse(201, Map.of(), new byte[] {1, 2}))
         .isNotEqualTo(new StoredResponse(200, Map.of("a", List.of("b")), new byte[] {1, 2}))
         .isNotEqualTo(new StoredResponse(200, Map.of(), new byte[] {1}))
         .isNotEqualTo("other");
      assertThat(stored.toString()).contains("2 bytes");
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new AcquireResult.Completed(null));
   }

   @Test
   @DisplayName("resolves keys from a header, ignoring blank values")
   public void testHeaderResolver()
   {
      HeaderIdempotencyKeyResolver resolver = new HeaderIdempotencyKeyResolver("Request-Id");

      assertThat(resolver.resolve(request(Map.of("request-id", List.of(" abc "))))).contains("abc");
      assertThat(resolver.resolve(request(Map.of("Request-Id", List.of("  "))))).isEmpty();
      assertThat(resolver.resolve(request(Map.of()))).isEmpty();
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new HeaderIdempotencyKeyResolver(null));
   }

   @Test
   @DisplayName("buffers a request body for repeated reads")
   public void testBufferedRequest() throws IOException
   {
      FakeRestRequest original = new FakeRestRequest(
         "POST", "/x", Map.of("q", List.of("1")), Map.of("h", List.of("v")), "body");
      BufferedRestRequest buffered = BufferedRestRequest.of(original);

      assertThat(buffered.body().readAllBytes()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
      assertThat(buffered.body().readAllBytes()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
      assertThat(buffered.bodyBytes()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
      assertThat(buffered.method()).isEqualTo("POST");
      assertThat(buffered.path()).isEqualTo("/x");
      assertThat(buffered.queryParameterValues("q")).containsExactly("1");
      assertThat(buffered.headerValues("h")).containsExactly("v");
      assertThat(BufferedRestRequest.of(buffered)).isSameAs(buffered);
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> BufferedRestRequest.of(null));
   }

   private static FakeRestRequest request(final Map<String, List<String>> headers)
   {
      return new FakeRestRequest("POST", "/x", Map.of(), headers, "");
   }

   private static final class MutableClock extends Clock
   {
      private Instant now = Instant.parse("2026-01-01T00:00:00Z");

      void advance(final Duration duration)
      {
         now = now.plus(duration);
      }

      @Override
      public ZoneId getZone()
      {
         return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(final ZoneId zone)
      {
         return this;
      }

      @Override
      public Instant instant()
      {
         return now;
      }
   }
}
