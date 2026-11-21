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

import io.github.jamoamo.honeycomb.rest.auth.AuthenticatedPrincipal;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;
import io.github.jamoamo.honeycomb.rest.intercept.InterceptorChain;
import io.github.jamoamo.honeycomb.rest.request.BufferedRestRequest;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.rest.response.RestResponseBody;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes {@link Idempotent} endpoints idempotent.
 *
 * <p>
 * For a request carrying an idempotency key, the key is scoped to the authenticated caller (or to anonymous callers
 * of a {@code @PermitAll} endpoint) and to the endpoint, and the request is fingerprinted from its method, path and
 * body. Then:
 * </p>
 * <ul>
 * <li>an unused key is acquired and the endpoint invoked; a {@code 2xx} response is stored for replay, and any
 * other outcome releases the key so the request can be retried;</li>
 * <li>a key used for an identical, completed request is answered with the stored response, marked with an
 * {@value #REPLAYED_HEADER} header, without invoking the endpoint;</li>
 * <li>a key still held by an unfinished request is answered with a {@code 409};</li>
 * <li>a key used for a different request is answered with a {@code 422}.</li>
 * </ul>
 * <p>
 * A request without a key is invoked normally unless the endpoint requires one, when it is answered with a
 * {@code 400}, as is a key longer than {@value #MAX_KEY_LENGTH} characters. It runs at {@link #ORDER}, after the
 * auth interceptor, so that the caller is known.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class IdempotencyInterceptor implements EndpointInterceptor
{
   /**
    * The order at which the interceptor should run: after the auth interceptor.
    */
   public static final int ORDER = 2000;

   /**
    * The header marking a replayed response.
    */
   public static final String REPLAYED_HEADER = "Idempotent-Replayed";

   /**
    * The longest accepted idempotency key.
    */
   public static final int MAX_KEY_LENGTH = 255;

   private static final TypeReference<ApiResponse<JsonNode>> STORED_BODY = new TypeReference<>()
   {
   };
   private static final String BAD_REQUEST = "Bad Request";
   private static final int STATUS_BAD_REQUEST = 400;
   private static final int STATUS_CONFLICT = 409;
   private static final int STATUS_UNPROCESSABLE = 422;
   private static final int STATUS_CLASS = 100;
   private static final int SUCCESS_CLASS = 2;
   private static final byte SEPARATOR = 0;

   private final IdempotencyKeyResolver keyResolver;
   private final IdempotencyStore store;
   private final ObjectMapper objectMapper;
   private final Map<Method, Duration> ttls = new ConcurrentHashMap<>();

   /**
    * Constructor resolving keys from the {@value HeaderIdempotencyKeyResolver#DEFAULT_HEADER} header.
    *
    * @param store        the store recording keys and responses
    * @param objectMapper the object mapper used to serialize stored response bodies
    */
   public IdempotencyInterceptor(final IdempotencyStore store, final ObjectMapper objectMapper)
   {
      this(new HeaderIdempotencyKeyResolver(), store, objectMapper);
   }

   /**
    * Constructor.
    *
    * @param keyResolver  resolves the idempotency key from the request
    * @param store        the store recording keys and responses
    * @param objectMapper the object mapper used to serialize stored response bodies
    */
   public IdempotencyInterceptor(
      final IdempotencyKeyResolver keyResolver,
      final IdempotencyStore store,
      final ObjectMapper objectMapper)
   {
      this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver cannot be null");
      this.store = Objects.requireNonNull(store, "store cannot be null");
      this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
   }

   @Override
   public RestResponse intercept(final EndpointInvocation invocation, final InterceptorChain chain)
   {
      Idempotent idempotent = invocation.method().getAnnotation(Idempotent.class);
      if (idempotent == null)
      {
         return chain.proceed(invocation);
      }
      return keyResolver.resolve(invocation.request())
         .map(key -> handle(invocation, chain, idempotent, key))
         .orElseGet(() -> idempotent.required()
            ? problem(invocation, STATUS_BAD_REQUEST, BAD_REQUEST,
               "This endpoint requires an idempotency key.", "IDEMPOTENCY_KEY_REQUIRED")
            : chain.proceed(invocation));
   }

   private RestResponse handle(
      final EndpointInvocation invocation,
      final InterceptorChain chain,
      final Idempotent idempotent,
      final String key)
   {
      if (key.length() > MAX_KEY_LENGTH)
      {
         return problem(invocation, STATUS_BAD_REQUEST, BAD_REQUEST,
            "The idempotency key is longer than " + MAX_KEY_LENGTH + " characters.", "IDEMPOTENCY_KEY_INVALID");
      }
      EndpointInvocation buffered = buffer(invocation);
      String scoped = scope(buffered, key);
      return switch (store.tryAcquire(scoped, fingerprint(buffered), ttl(idempotent, invocation.method())))
      {
         case AcquireResult.Acquired acquired -> execute(scoped, buffered, chain);
         case AcquireResult.Completed completed -> replay(completed.response());
         case AcquireResult.InProgress inProgress -> problem(invocation, STATUS_CONFLICT, "Conflict",
            "A request with this idempotency key is still in progress.", "IDEMPOTENCY_REQUEST_IN_PROGRESS");
         case AcquireResult.Mismatch mismatch -> problem(invocation, STATUS_UNPROCESSABLE, "Unprocessable Content",
            "The idempotency key was already used for a different request.", "IDEMPOTENCY_KEY_REUSED");
      };
   }

   private RestResponse execute(final String scoped, final EndpointInvocation invocation, final InterceptorChain chain)
   {
      boolean completed = false;
      try
      {
         RestResponse response = chain.proceed(invocation);
         Optional<StoredResponse> stored = toStored(response);
         if (stored.isPresent())
         {
            store.complete(scoped, stored.get());
            completed = true;
         }
         return response;
      }
      finally
      {
         if (!completed)
         {
            store.release(scoped);
         }
      }
   }

   private Optional<StoredResponse> toStored(final RestResponse response)
   {
      Optional<RestResponseBody> body = response.body();
      if (response.status() / STATUS_CLASS != SUCCESS_CLASS || body.filter(ProblemDetail.class::isInstance).isPresent())
      {
         return Optional.empty();
      }
      byte[] bytes = body.map(objectMapper::writeValueAsBytes).orElseGet(() -> new byte[0]);
      return Optional.of(new StoredResponse(response.status(), response.headers(), bytes));
   }

   private RestResponse replay(final StoredResponse stored)
   {
      byte[] bytes = stored.body();
      Optional<RestResponseBody> body = bytes.length == 0
         ? Optional.empty()
         : Optional.of(objectMapper.readValue(bytes, STORED_BODY));
      return new RestResponse(stored.status(), stored.headers(), body).withHeader(REPLAYED_HEADER, "true");
   }

   private static EndpointInvocation buffer(final EndpointInvocation invocation)
   {
      try
      {
         return new EndpointInvocation(
            BufferedRestRequest.of(invocation.request()), invocation.match(), invocation.attributes());
      }
      catch (final IOException ex)
      {
         throw new UncheckedIOException("Unable to read the request body", ex);
      }
   }

   private static String scope(final EndpointInvocation invocation, final String key)
   {
      String caller = invocation.attributes().get(AuthenticatedPrincipal.ATTRIBUTE)
         .map(principal -> "principal:" + principal.subject())
         .orElse("anonymous");
      return segment(caller) + segment(invocation.method().toString()) + key;
   }

   private static String segment(final String value)
   {
      return value.length() + ":" + value + "|";
   }

   private static String fingerprint(final EndpointInvocation invocation)
   {
      BufferedRestRequest request = (BufferedRestRequest) invocation.request();
      try
      {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         digest.update(request.method().getBytes(StandardCharsets.UTF_8));
         digest.update(SEPARATOR);
         digest.update(request.path().getBytes(StandardCharsets.UTF_8));
         digest.update(SEPARATOR);
         digest.update(request.bodyBytes());
         return HexFormat.of().formatHex(digest.digest());
      }
      catch (final NoSuchAlgorithmException ex)
      {
         throw new IllegalStateException("SHA-256 is not available", ex);
      }
   }

   private Duration ttl(final Idempotent idempotent, final Method method)
   {
      return ttls.computeIfAbsent(method, m -> parseTtl(idempotent.ttl(), m));
   }

   private static Duration parseTtl(final String text, final Method method)
   {
      Duration ttl = null;
      try
      {
         ttl = Duration.parse(text);
      }
      catch (final DateTimeParseException ex)
      {
         // Reported below.
      }
      if (ttl == null || ttl.isNegative() || ttl.isZero())
      {
         throw new IllegalArgumentException("Invalid @Idempotent ttl \"" + text + "\" on " + method
            + "; expected a positive ISO-8601 duration such as PT24H");
      }
      return ttl;
   }

   private static RestResponse problem(
      final EndpointInvocation invocation,
      final int status,
      final String title,
      final String detail,
      final String code)
   {
      return RestResponse.problem(ProblemDetail.forStatus(status, title, detail, invocation.request().path(), code));
   }
}
