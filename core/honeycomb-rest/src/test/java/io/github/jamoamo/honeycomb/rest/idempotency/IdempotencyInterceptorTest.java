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

import io.github.jamoamo.honeycomb.rest.DefaultFailureProblemMapper;
import io.github.jamoamo.honeycomb.rest.EndpointResult;
import io.github.jamoamo.honeycomb.rest.FakeRestRequest;
import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.auth.AuthInterceptor;
import io.github.jamoamo.honeycomb.rest.auth.AuthenticatedPrincipal;
import io.github.jamoamo.honeycomb.rest.auth.BearerTokenExtractor;
import io.github.jamoamo.honeycomb.rest.auth.PermitAll;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link IdempotencyInterceptor}, run through the {@link RestControllerAdapterMethodInvoker} behind an
 * {@link AuthInterceptor}.
 *
 * @author James Amoore
 */
@DisplayName("IdempotencyInterceptor")
public class IdempotencyInterceptorTest
{
   private static final String KEY = HeaderIdempotencyKeyResolver.DEFAULT_HEADER;
   private static final ObjectMapper MAPPER = JsonMapper.builder().build();

   private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
   private final Adapter adapter = new Adapter();
   private final List<EndpointInterceptor> extra = new ArrayList<>();
   private final RestControllerAdapterMethodInvoker invoker = new RestControllerAdapterMethodInvoker(
      MAPPER, new DefaultFailureProblemMapper(), List.of(
         new AuthInterceptor(token -> AuthenticatedPrincipal.of(token)),
         new IdempotencyInterceptor(store, MAPPER),
         (invocation, chain) -> extra.isEmpty()
            ? chain.proceed(invocation)
            : extra.getFirst().intercept(invocation, chain)));

   public record Order(String item)
   {
   }

   public final class Adapter
   {
      private final AtomicInteger calls = new AtomicInteger();
      private final AtomicReference<Runnable> during = new AtomicReference<>(() -> { });

      @Idempotent
      public EndpointResult<Order> place(final Order order)
      {
         calls.incrementAndGet();
         during.get().run();
         return EndpointResult.created(order, URI.create("/orders/" + calls.get()));
      }

      @Idempotent(required = true)
      public EndpointResult<Order> required(final Order order)
      {
         calls.incrementAndGet();
         return EndpointResult.ok(order);
      }

      @Idempotent
      @PermitAll
      public EndpointResult<Void> open()
      {
         calls.incrementAndGet();
         return EndpointResult.noContent();
      }

      @Idempotent
      public EndpointResult<Order> rejected()
      {
         calls.incrementAndGet();
         return EndpointResult.problem(ProblemDetail.badRequest("no", "/x", "NO"));
      }

      @Idempotent
      public String failing()
      {
         calls.incrementAndGet();
         throw new IllegalStateException("boom");
      }

      @Idempotent
      public RestResponse oddSuccess()
      {
         calls.incrementAndGet();
         return new RestResponse(200, Map.of(), Optional.of(ProblemDetail.badRequest("odd", "/x", "ODD")));
      }

      @Idempotent(ttl = "forever")
      public String badTtl()
      {
         return "x";
      }

      @Idempotent(ttl = "PT0S")
      public String zeroTtl()
      {
         return "x";
      }

      public String plain()
      {
         calls.incrementAndGet();
         return "plain";
      }
   }

   @Test
   @DisplayName("replays the stored response for a repeated key without invoking the endpoint")
   public void testReplay()
   {
      RestResponse first = invoke("place", "james", "k1", "{\"item\":\"tea\"}");
      RestResponse second = invoke("place", "james", "k1", "{\"item\":\"tea\"}");

      assertThat(first.status()).isEqualTo(201);
      assertThat(first.headers()).doesNotContainKey(IdempotencyInterceptor.REPLAYED_HEADER);
      assertThat(second.status()).isEqualTo(201);
      assertThat(second.headers()).containsEntry(IdempotencyInterceptor.REPLAYED_HEADER, List.of("true"));
      assertThat(second.headers().get("Location")).isEqualTo(first.headers().get("Location"));
      assertThat(MAPPER.writeValueAsString(second.body().orElseThrow()))
         .isEqualTo(MAPPER.writeValueAsString(first.body().orElseThrow()));
      assertThat(adapter.calls).hasValue(1);
   }

   @Test
   @DisplayName("answers a key reused for a different request with 422")
   public void testMismatch()
   {
      invoke("place", "james", "k1", "{\"item\":\"tea\"}");

      RestResponse response = invoke("place", "james", "k1", "{\"item\":\"coffee\"}");

      assertThat(response.status()).isEqualTo(422);
      assertThat(adapter.calls).hasValue(1);
   }

   @Test
   @DisplayName("scopes keys to the caller")
   public void testScopedPerPrincipal()
   {
      invoke("place", "james", "k1", "{\"item\":\"tea\"}");
      RestResponse other = invoke("place", "alex", "k1", "{\"item\":\"tea\"}");

      assertThat(other.headers()).doesNotContainKey(IdempotencyInterceptor.REPLAYED_HEADER);
      assertThat(adapter.calls).hasValue(2);
   }

   @Test
   @DisplayName("answers a concurrent request with the same key with 409")
   public void testInProgress()
   {
      AtomicReference<RestResponse> concurrent = new AtomicReference<>();
      adapter.during.set(() -> {
         adapter.during.set(() -> { });
         concurrent.set(invoke("place", "james", "k1", "{\"item\":\"tea\"}"));
      });

      invoke("place", "james", "k1", "{\"item\":\"tea\"}");

      assertThat(concurrent.get().status()).isEqualTo(409);
   }

   @Test
   @DisplayName("releases the key after a failure or non-2xx response so the request can be retried")
   public void testReleaseOnFailure()
   {
      assertThat(invoke("failing", "james", "k1", "").status()).isEqualTo(500);
      assertThat(invoke("failing", "james", "k1", "").status()).isEqualTo(500);
      assertThat(invoke("rejected", "james", "k2", "").status()).isEqualTo(400);
      assertThat(invoke("rejected", "james", "k2", "").status()).isEqualTo(400);
      assertThat(invoke("oddSuccess", "james", "k3", "").status()).isEqualTo(200);
      assertThat(invoke("oddSuccess", "james", "k3", "").headers())
         .doesNotContainKey(IdempotencyInterceptor.REPLAYED_HEADER);

      assertThat(adapter.calls).hasValue(6);
      assertThat(store.size()).isZero();
   }

   @Test
   @DisplayName("releases the key when a later interceptor throws")
   public void testReleaseOnException()
   {
      extra.add((invocation, chain) -> {
         throw new IllegalStateException("later");
      });
      assertThat(invoke("place", "james", "k1", "{\"item\":\"tea\"}").status()).isEqualTo(500);

      extra.clear();
      assertThat(invoke("place", "james", "k1", "{\"item\":\"tea\"}").status()).isEqualTo(201);
   }

   @Test
   @DisplayName("replays bodiless responses and scopes public endpoints to anonymous callers")
   public void testNoContentAnonymous()
   {
      RestResponse first = invoke("open", null, "k1", "");
      RestResponse second = invoke("open", "james", "k1", "");

      assertThat(first.status()).isEqualTo(204);
      assertThat(second.status()).isEqualTo(204);
      assertThat(second.body()).isEmpty();
      assertThat(second.headers()).containsKey(IdempotencyInterceptor.REPLAYED_HEADER);
      assertThat(adapter.calls).hasValue(1);
   }

   @Test
   @DisplayName("invokes normally without a key unless the endpoint requires one")
   public void testMissingKey()
   {
      invoke("place", "james", null, "{\"item\":\"tea\"}");
      invoke("place", "james", null, "{\"item\":\"tea\"}");
      assertThat(adapter.calls).hasValue(2);

      RestResponse required = invoke("required", "james", null, "{\"item\":\"tea\"}");
      assertThat(required.status()).isEqualTo(400);
      assertThat(((ProblemDetail) required.body().orElseThrow()).code()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
      assertThat(invoke("required", "james", "k", "{\"item\":\"tea\"}").status()).isEqualTo(200);
   }

   @Test
   @DisplayName("rejects overlong keys")
   public void testLongKey()
   {
      char[] key = new char[IdempotencyInterceptor.MAX_KEY_LENGTH + 1];
      Arrays.fill(key, 'k');

      assertThat(invoke("place", "james", new String(key), "{\"item\":\"tea\"}").status()).isEqualTo(400);
      assertThat(adapter.calls).hasValue(0);
   }

   @Test
   @DisplayName("ignores endpoints that are not @Idempotent")
   public void testNotIdempotent()
   {
      invoke("plain", "james", "k1", "");
      invoke("plain", "james", "k1", "");

      assertThat(adapter.calls).hasValue(2);
      assertThat(store.size()).isZero();
   }

   @Test
   @DisplayName("answers an invalid ttl with a 500")
   public void testInvalidTtl()
   {
      assertThat(invoke("badTtl", "james", "k1", "").status()).isEqualTo(500);
      assertThat(invoke("zeroTtl", "james", "k1", "").status()).isEqualTo(500);
   }

   @Test
   @DisplayName("answers an unreadable body with a 500")
   public void testUnreadableBody()
   {
      RestRequest throwing =
         new DelegatingRequest(new FakeRestRequest("POST", "/x", Map.of(), headers("james", "k1"), ""));

      assertThat(invoker.invokeRestControllerAdapterMethod(match("place"), throwing).status()).isEqualTo(500);
   }

   @Test
   @DisplayName("rejects null collaborators")
   public void testNulls()
   {
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new IdempotencyInterceptor(null, MAPPER));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new IdempotencyInterceptor(store, null));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new IdempotencyInterceptor(null, store, MAPPER));
   }

   private RestResponse invoke(final String method, final String user, final String key, final String body)
   {
      return invoker.invokeRestControllerAdapterMethod(
         match(method), new FakeRestRequest("POST", "/" + method, Map.of(), headers(user, key), body));
   }

   private static Map<String, List<String>> headers(final String user, final String key)
   {
      Map<String, List<String>> headers = new HashMap<>();
      if (user != null)
      {
         headers.put(BearerTokenExtractor.AUTHORIZATION, List.of("Bearer " + user));
      }
      if (key != null)
      {
         headers.put(KEY, List.of(key));
      }
      return headers;
   }

   private RouteMatch match(final String name)
   {
      return new RouteMatch(new HandlerMethod(adapter, Arrays.stream(Adapter.class.getMethods())
         .filter(m -> m.getName().equals(name)).findFirst().orElseThrow()), Map.of());
   }

   private static final class DelegatingRequest implements RestRequest
   {
      private final RestRequest delegate;

      DelegatingRequest(final RestRequest delegate)
      {
         this.delegate = delegate;
      }

      @Override
      public String method()
      {
         return delegate.method();
      }

      @Override
      public String path()
      {
         return delegate.path();
      }

      @Override
      public List<String> queryParameterValues(final String name)
      {
         return delegate.queryParameterValues(name);
      }

      @Override
      public List<String> headerValues(final String name)
      {
         return delegate.headerValues(name);
      }

      @Override
      public InputStream body() throws IOException
      {
         throw new IOException("unreadable");
      }
   }
}
