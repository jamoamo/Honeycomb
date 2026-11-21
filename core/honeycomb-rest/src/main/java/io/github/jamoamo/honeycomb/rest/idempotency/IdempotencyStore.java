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

import java.time.Duration;

/**
 * Records idempotency keys and the responses they produced; the port behind {@link IdempotencyInterceptor}.
 *
 * <p>
 * Implementations must make {@link #tryAcquire} atomic, so that of two concurrent requests with the same key exactly
 * one acquires it. Shared stores (a database, Redis) let idempotency hold across application instances; the default
 * {@link InMemoryIdempotencyStore} holds only within one.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public interface IdempotencyStore
{
   /**
    * Atomically acquires a key unless it is already in use.
    *
    * @param key         the scoped key, already combining the caller, endpoint and client key
    * @param fingerprint a digest of the request, used to detect a key reused for a different request
    * @param ttl         how long the key is remembered, including while the request is in progress
    * @return the outcome
    */
   AcquireResult tryAcquire(String key, String fingerprint, Duration ttl);

   /**
    * Marks an acquired key completed with the response to replay for later requests.
    *
    * @param key      the scoped key
    * @param response the response
    */
   void complete(String key, StoredResponse response);

   /**
    * Releases an acquired key that was not completed, so that the request can be retried with it.
    *
    * @param key the scoped key
    */
   void release(String key);
}
